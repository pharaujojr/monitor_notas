# monitor-nfe-entrada

Projeto self-hosted para monitoramento de NF-e de entrada para um CNPJ específico. O sistema registra eventos, acompanha manifestação externa, baixa XML quando disponível e gera DANFE/PDF localmente.

## Stack

- Backend: Java 21 + Spring Boot + Gradle Wrapper
- Frontend: React + Vite
- Banco: PostgreSQL
- Infra: Docker + Docker Compose

## Estrutura

- `backend/`: API, serviços, jobs, migrations e storage logic
- `frontend/`: dashboard operacional React
- `docker-compose.yml`: sobe frontend, backend e postgres
- `.env.example`: variáveis obrigatórias
- `script.sh.example`: modelo de deploy remoto via SSH

## Endpoints principais

- `GET /api/dashboard`
- `GET /api/notes`
- `GET /api/notes/{id}`
- `GET /api/notes/{id}/xml`
- `GET /api/notes/{id}/pdf`
- `GET /api/settings/company`
- `PUT /api/settings/company`
- `POST /api/settings/company/certificate`
- `GET /api/logs`
- `POST /api/exports/xml`
- `POST /api/exports/pdf`
- `POST /api/external-xml/importar`

## Coexistência com outro ERP

Quando outro ERP usa o mesmo CNPJ/certificado, os dois sistemas dividem o limite de consumo da SEFAZ. Se o outro ERP fica em outro ambiente e não disponibiliza XML para leitura local, o modo recomendado é coexistência defensiva:

- deixar `ERP_XML_IMPORT_ENABLED=false`
- manter `SEFAZ_FALLBACK_ENABLED=true`
- usar `SEFAZ_MAX_CONSULTAS_POR_EXECUCAO=1`
- deixar o agendamento persistido controlar cooldown, backoff e jitter

O importador por pasta continua disponível para cenários em que algum ERP grave XMLs em um diretório acessível pelo TrueNAS.

## Subida local

1. Copie `.env.example` para `.env`
2. Ajuste as variáveis
3. Suba com:

```bash
docker compose up -d --build
```

Frontend: `http://localhost:3000`

Backend: `http://localhost:8080`

## Observações do MVP

- A integração com SEFAZ roda em modo defensivo por padrão
- O importador por diretório é opcional e fica desligado por padrão
- O MVP não manifesta NF-e
- O job agendado respeita `ultNSU`, cooldown, backoff exponencial no `656` e jitter para reduzir colisão com outro ERP
- O projeto sobe com dados de exemplo para facilitar validação inicial

## Próximos passos recomendados

- integrar biblioteca homologada para NFeDistribuicaoDFe
- endurecer autenticação e autorização
- trocar o gerador simplificado de PDF por uma DANFE completa
- adicionar paginação/ordenação avançadas no frontend
