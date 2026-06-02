package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.NfeStatus;
import java.time.LocalDate;

public record NoteFilter(
    LocalDate dataInicial,
    LocalDate dataFinal,
    NfeStatus status,
    String emitenteNome,
    String chave
) {
}
