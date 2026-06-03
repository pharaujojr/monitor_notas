-- Contador de rejeições 656 consecutivas para backoff exponencial.
alter table company_configs add column bloqueios_consecutivos int not null default 0;
