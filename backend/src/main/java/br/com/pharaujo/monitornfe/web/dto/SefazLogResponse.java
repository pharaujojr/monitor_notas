package br.com.pharaujo.monitornfe.web.dto;

import java.time.OffsetDateTime;

public record SefazLogResponse(
    Long id,
    String cnpj,
    String ambiente,
    String nsuInicial,
    String nsuFinal,
    String cstat,
    String motivo,
    OffsetDateTime occurredAt,
    String errorMessage
) {
}
