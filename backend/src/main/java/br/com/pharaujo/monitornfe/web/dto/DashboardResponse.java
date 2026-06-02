package br.com.pharaujo.monitornfe.web.dto;

import java.util.List;

public record DashboardResponse(
    long totalNotasDetectadas,
    long totalPendenteXml,
    long totalXmlBaixado,
    long totalPdfGerado,
    long totalCanceladas,
    List<NoteSummaryResponse> ultimasNotas
) {
}
