package br.com.pharaujo.monitornfe.web.dto;

import br.com.pharaujo.monitornfe.domain.NfeStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NoteSummaryResponse(
    Long id,
    String chaveAcesso,
    String emitenteNome,
    String emitenteCnpj,
    LocalDateTime dataEmissao,
    BigDecimal valorTotal,
    NfeStatus status,
    String manifestacaoStatus,
    String manifestacaoDescricao
) {
}
