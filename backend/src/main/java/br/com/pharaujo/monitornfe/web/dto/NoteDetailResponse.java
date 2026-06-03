package br.com.pharaujo.monitornfe.web.dto;

import br.com.pharaujo.monitornfe.domain.NfeStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record NoteDetailResponse(
    Long id,
    String chaveAcesso,
    String nsu,
    String modelo,
    String emitenteCnpj,
    String emitenteNome,
    String destinatarioCnpj,
    LocalDateTime dataEmissao,
    BigDecimal valorTotal,
    NfeStatus status,
    String manifestacaoStatus,
    String manifestacaoDescricao,
    OffsetDateTime manifestacaoEventoAt,
    String xmlStoragePath,
    String pdfStoragePath,
    OffsetDateTime xmlDownloadedAt,
    OffsetDateTime pdfGeneratedAt,
    List<NfeEventResponse> eventos,
    List<StatusHistoryResponse> historicoStatus
) {
}
