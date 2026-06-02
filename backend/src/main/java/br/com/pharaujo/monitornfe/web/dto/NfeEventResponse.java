package br.com.pharaujo.monitornfe.web.dto;

import java.time.OffsetDateTime;

public record NfeEventResponse(
    Long id,
    String eventCode,
    String eventName,
    String eventProtocol,
    OffsetDateTime occurredAt,
    String details
) {
}
