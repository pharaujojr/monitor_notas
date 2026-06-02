package br.com.pharaujo.monitornfe.web.dto;

import br.com.pharaujo.monitornfe.domain.NfeStatus;
import java.time.LocalDate;

public record ExportRequest(
    LocalDate dataInicial,
    LocalDate dataFinal,
    NfeStatus status,
    String emitenteNome
) {
}
