package br.com.pharaujo.monitornfe.web.dto;

public record SyncResultResponse(
    String cstat,
    String motivo,
    String ultNsu,
    String maxNsu,
    int documentosProcessados,
    int consultasRealizadas
) {
}
