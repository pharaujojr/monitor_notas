create table certificate_records (
    id bigserial primary key,
    original_filename varchar(255) not null,
    storage_path varchar(500) not null,
    file_size_bytes bigint not null,
    sha256 varchar(64) not null,
    valid_from date,
    valid_to date,
    uploaded_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table company_configs (
    id bigserial primary key,
    cnpj varchar(14) not null unique,
    razao_social varchar(255) not null,
    uf varchar(2) not null,
    ambiente varchar(20) not null,
    status varchar(20) not null,
    ult_nsu varchar(20) not null default '0',
    max_nsu varchar(20) not null default '0',
    certificate_id bigint null references certificate_records(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table nfe_notes (
    id bigserial primary key,
    company_config_id bigint not null references company_configs(id),
    chave_acesso varchar(44) not null unique,
    nsu varchar(20) not null unique,
    modelo varchar(2) not null,
    emitente_cnpj varchar(14) not null,
    emitente_nome varchar(255) not null,
    destinatario_cnpj varchar(14) not null,
    data_emissao timestamp not null,
    valor_total numeric(15,2) not null,
    status varchar(40) not null,
    xml_storage_path varchar(500),
    pdf_storage_path varchar(500),
    xml_downloaded_at timestamptz,
    pdf_generated_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_nfe_notes_status on nfe_notes(status);
create index idx_nfe_notes_data_emissao on nfe_notes(data_emissao desc);
create index idx_nfe_notes_emitente_nome on nfe_notes(lower(emitente_nome));

create table nfe_events (
    id bigserial primary key,
    nfe_note_id bigint not null references nfe_notes(id),
    event_code varchar(6) not null,
    event_name varchar(120) not null,
    event_protocol varchar(40),
    occurred_at timestamptz not null,
    details varchar(4000),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_nfe_events_note_occurred_at on nfe_events(nfe_note_id, occurred_at desc);

create table note_status_history (
    id bigserial primary key,
    nfe_note_id bigint not null references nfe_notes(id),
    previous_status varchar(40),
    new_status varchar(40) not null,
    changed_at timestamptz not null,
    reason varchar(255) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_note_status_history_note_changed_at on note_status_history(nfe_note_id, changed_at desc);

create table sefaz_query_logs (
    id bigserial primary key,
    cnpj varchar(14) not null,
    ambiente varchar(20) not null,
    nsu_inicial varchar(20) not null,
    nsu_final varchar(20) not null,
    cstat varchar(10) not null,
    motivo varchar(500) not null,
    occurred_at timestamptz not null,
    error_message varchar(2000),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_sefaz_query_logs_occurred_at on sefaz_query_logs(occurred_at desc);

create table export_audits (
    id bigserial primary key,
    export_type varchar(10) not null,
    period_start date,
    period_end date,
    filters_json varchar(4000) not null,
    generated_file_path varchar(500) not null,
    generated_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
