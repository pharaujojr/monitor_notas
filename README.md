# monitor-nfe-entrada

Projeto self-hosted para monitoramento de NF-e de entrada para um CNPJ específico. O sistema prioriza XMLs exportados por um ERP externo, registra eventos, acompanha manifestação externa, baixa XML quando disponível e gera DANFE/PDF localmente.

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

Quando outro ERP usa o mesmo CNPJ/certificado, os dois sistemas dividem o limite de consumo da SEFAZ. Para evitar rejeição `656 - Consumo Indevido`, o modo recomendado é:

- deixar `ERP_XML_IMPORT_ENABLED=true`
- apontar `ERP_XML_HOST_PATH` para a pasta do TrueNAS onde o ERP grava XMLs
- manter `SEFAZ_FALLBACK_ENABLED=false`
- habilitar `SEFAZ_FALLBACK_ENABLED=true` apenas em janela controlada, com `SEFAZ_MAX_CONSULTAS_POR_EXECUCAO=1` ou `2`

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

- A integração com SEFAZ fica em modo fallback defensivo por padrão
- O caminho principal de entrada de XML é o diretório configurado em `ERP_XML_IMPORT_PATH`
- O MVP não manifesta NF-e
- O job agendado evita consultar a SEFAZ quando `SEFAZ_FALLBACK_ENABLED=false`
- O projeto sobe com dados de exemplo para facilitar validação inicial

## Próximos passos recomendados

- integrar biblioteca homologada para NFeDistribuicaoDFe
- endurecer autenticação e autorização
- trocar o gerador simplificado de PDF por uma DANFE completa
- adicionar paginação/ordenação avançadas no frontend
