-- Estado de sincronização persistido (sobrevive a restarts) ------------------
alter table company_configs add column proxima_consulta_permitida timestamptz;
alter table company_configs add column proxima_sincronizacao timestamptz;

-- Semente do agendamento: respeita o bloqueio de consumo indevido (656) ativo
-- desde os testes; primeira consulta logo após o cooldown, depois a cada 6h.
update company_configs
   set proxima_consulta_permitida = timestamptz '2026-06-03 13:29:00+00',
       proxima_sincronizacao      = timestamptz '2026-06-03 13:30:00+00';

-- Status de manifestação (somente leitura; a manifestação ocorre em outra aplicação)
alter table nfe_notes add column manifestacao_status varchar(40);
alter table nfe_notes add column manifestacao_descricao varchar(120);
alter table nfe_notes add column manifestacao_evento_at timestamptz;

-- Comentários (estilo rede social) ------------------------------------------
create table note_comments (
    id bigserial primary key,
    nfe_note_id bigint not null references nfe_notes(id) on delete cascade,
    autor varchar(120) not null,
    body varchar(4000) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_note_comments_note_created_at on note_comments(nfe_note_id, created_at desc);

create table comment_attachments (
    id bigserial primary key,
    note_comment_id bigint not null references note_comments(id) on delete cascade,
    original_filename varchar(255) not null,
    storage_path varchar(500) not null,
    content_type varchar(120),
    file_size_bytes bigint not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_comment_attachments_comment on comment_attachments(note_comment_id);
