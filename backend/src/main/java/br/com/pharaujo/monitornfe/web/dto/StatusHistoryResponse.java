package br.com.pharaujo.monitornfe.web.dto;

import br.com.pharaujo.monitornfe.domain.NfeStatus;
import java.time.OffsetDateTime;

public record StatusHistoryResponse(
    Long id,
    NfeStatus previousStatus,
    NfeStatus newStatus,
    OffsetDateTime changedAt,
    String reason
) {
}
