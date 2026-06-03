create table external_xml_import_records (
    id bigserial primary key,
    source_path varchar(1000) not null,
    sha256 varchar(64) not null,
    status varchar(20) not null,
    chave_acesso varchar(44),
    message varchar(1000),
    imported_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_external_xml_import_records_sha_status
    on external_xml_import_records(sha256, status);

create index idx_external_xml_import_records_imported_at
    on external_xml_import_records(imported_at desc);
