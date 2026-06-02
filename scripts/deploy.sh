#!/usr/bin/env bash
#
# Deploy do Monitor NF-e para o servidor TrueNAS a partir desta máquina.
#
# Pré-requisitos:
#   - Alias SSH "truenas" em ~/.ssh/config (autenticação por chave pessoal)
#   - Deploy key read-only "github-monitor-notas" configurada no truenas
#     (gere com --setup-key caso ainda não exista)
#   - .env local presente (não é commitado) — é sincronizado a cada deploy
#
# Uso:
#   ./scripts/deploy.sh                 # commit pendente + push + pull+up no servidor
#   ./scripts/deploy.sh --no-commit     # não tenta commitar; só push (se houver) + pull + up
#   ./scripts/deploy.sh --logs          # após o deploy, mostra logs do backend
#   ./scripts/deploy.sh -m "mensagem"   # mensagem custom para o commit automático
#   ./scripts/deploy.sh --setup-key     # gera deploy key e exibe instruções de configuração
#
set -euo pipefail

# -------- configuração --------
SSH_HOST="truenas"
REMOTE_DIR="/mnt/DATASERVER/MONITOR_NOTAS"
DEPLOY_KEY_PATH="${HOME}/.ssh/monitor_notas_deploy"
DEFAULT_COMMIT_MSG="deploy: $(date +%Y-%m-%d_%H:%M:%S)"
# ------------------------------

cd "$(dirname "$0")/.."

# cores
B=$'\033[1m'; G=$'\033[32m'; Y=$'\033[33m'; R=$'\033[31m'; N=$'\033[0m'
log()  { echo "${B}${G}▶${N} $*"; }
warn() { echo "${B}${Y}!${N} $*"; }
die()  { echo "${B}${R}✗${N} $*" >&2; exit 1; }

# -------- --setup-key --------
if [[ "${1:-}" == "--setup-key" ]]; then
    if [[ -f "${DEPLOY_KEY_PATH}" ]]; then
        warn "Chave já existe em ${DEPLOY_KEY_PATH}. Abortando para não sobrescrever."
        echo "  Pública: ${DEPLOY_KEY_PATH}.pub"
        exit 1
    fi

    log "Gerando chave ed25519 read-only para deploy..."
    ssh-keygen -t ed25519 -C "monitor-notas-deploy@truenas" -f "${DEPLOY_KEY_PATH}" -N ""
    chmod 600 "${DEPLOY_KEY_PATH}"

    echo ""
    echo "${B}Chave gerada:${N}"
    echo "  Privada : ${DEPLOY_KEY_PATH}"
    echo "  Pública : ${DEPLOY_KEY_PATH}.pub"
    echo ""
    echo "${B}Passos seguintes:${N}"
    echo ""
    echo "  1. Adicione a chave pública abaixo ao GitHub como Deploy Key (somente leitura)"
    echo "     Repositório → Settings → Deploy keys → Add deploy key"
    echo "     Allow write access: NÃO marcar"
    echo ""
    cat "${DEPLOY_KEY_PATH}.pub"
    echo ""
    echo "  2. Copie a chave privada para o TrueNAS:"
    echo "     scp ${DEPLOY_KEY_PATH} truenas:~/.ssh/monitor_notas_deploy"
    echo "     ssh truenas 'chmod 600 ~/.ssh/monitor_notas_deploy'"
    echo ""
    echo "  3. Configure o alias SSH no TrueNAS (~/.ssh/config):"
    echo "     Host github-monitor-notas"
    echo "         HostName github.com"
    echo "         User git"
    echo "         IdentityFile ~/.ssh/monitor_notas_deploy"
    echo "         IdentitiesOnly yes"
    echo ""
    echo "  4. Prepare o diretório remoto (primeira vez):"
    echo "     ssh truenas 'mkdir -p ${REMOTE_DIR}'"
    echo "     ssh truenas 'git clone git@github-monitor-notas:pharaujojr/monitor_notas.git ${REMOTE_DIR}'"
    echo ""
    exit 0
fi

# -------- parse de argumentos --------
DO_COMMIT=1
SHOW_LOGS=0
COMMIT_MSG="$DEFAULT_COMMIT_MSG"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-commit) DO_COMMIT=0; shift ;;
        --logs)      SHOW_LOGS=1; shift ;;
        -m)          COMMIT_MSG="${2:?-m exige mensagem}"; shift 2 ;;
        -h|--help)
            sed -n '2,15p' "$0"
            exit 0 ;;
        *) die "Argumento desconhecido: $1" ;;
    esac
done

# -------- sanity checks --------
[[ -f .env ]] || die ".env não encontrado em $(pwd). Crie-o antes do deploy."
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "Não é um repositório git."

# lê BACKEND_PORT do .env (fallback 8080)
BACKEND_PORT=$(grep -E '^BACKEND_PORT=' .env | cut -d= -f2 | tr -d '[:space:]')
BACKEND_PORT="${BACKEND_PORT:-8080}"
ssh -o BatchMode=yes -o ConnectTimeout=5 "$SSH_HOST" true 2>/dev/null \
    || die "Não foi possível conectar ao host SSH '${SSH_HOST}'. Verifique ~/.ssh/config."

# -------- 1) commit local --------
if [[ "$DO_COMMIT" == "1" ]]; then
    if ! git diff --quiet || ! git diff --cached --quiet || \
       [[ -n "$(git ls-files --others --exclude-standard)" ]]; then
        log "Mudanças detectadas — fazendo commit: ${COMMIT_MSG}"
        git add -A
        git commit -m "$COMMIT_MSG"
    else
        log "Sem mudanças locais para commitar."
    fi
fi

# -------- 2) push --------
if [[ -n "$(git log @{u}.. 2>/dev/null || true)" ]] || ! git rev-parse @{u} >/dev/null 2>&1; then
    log "Enviando para origin..."
    git push
else
    log "origin já está em dia."
fi

LOCAL_SHA=$(git rev-parse HEAD)

# -------- 3) sincroniza .env --------
log "Sincronizando .env para ${SSH_HOST}:${REMOTE_DIR}/.env"
scp -q ./.env "${SSH_HOST}:${REMOTE_DIR}/.env"
ssh "$SSH_HOST" "chmod 600 ${REMOTE_DIR}/.env"

# -------- 4) pull + rebuild + up no servidor --------
log "Pull e rebuild no servidor (${SSH_HOST})"
ssh "$SSH_HOST" "set -e
    cd ${REMOTE_DIR}
    git fetch --quiet origin
    git reset --hard origin/main >/dev/null
    REMOTE_SHA=\$(git rev-parse HEAD)
    echo '   commit no servidor: '\$REMOTE_SHA
    sudo docker compose up -d --build --remove-orphans
"

# -------- 5) verifica SHA --------
REMOTE_SHA=$(ssh "$SSH_HOST" "cd ${REMOTE_DIR} && git rev-parse HEAD")
if [[ "$LOCAL_SHA" == "$REMOTE_SHA" ]]; then
    log "${G}OK${N} — local e servidor em ${LOCAL_SHA:0:10}"
else
    warn "SHA divergente: local=${LOCAL_SHA:0:10} servidor=${REMOTE_SHA:0:10}"
fi

# -------- 6) health check (backend actuator) --------
log "Aguardando backend subir..."
for i in {1..20}; do
    STATUS=$(ssh "$SSH_HOST" \
        "curl -fsS http://localhost:${BACKEND_PORT}/actuator/health 2>/dev/null | grep -o '\"status\":\"[^\"]*\"' || true")
    if echo "$STATUS" | grep -q '"UP"'; then
        log "${G}Backend UP${N} — /actuator/health OK"
        break
    fi
    sleep 3
    [[ $i == 20 ]] && warn "Health check não respondeu em 60s — confira os logs com --logs."
done

# -------- 7) logs opcionais --------
if [[ "$SHOW_LOGS" == "1" ]]; then
    log "Últimas linhas do log do backend:"
    ssh "$SSH_HOST" "cd ${REMOTE_DIR} && sudo docker compose logs --tail 60 backend"
fi
