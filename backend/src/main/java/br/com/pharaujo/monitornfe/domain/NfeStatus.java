package br.com.pharaujo.monitornfe.domain;

public enum NfeStatus {
    DETECTADA_RESUMO,
    MANIFESTADA_EXTERNAMENTE,
    XML_DISPONIVEL,
    XML_BAIXADO,
    PDF_GERADO,
    CANCELADA,
    ERRO_DOWNLOAD
}
