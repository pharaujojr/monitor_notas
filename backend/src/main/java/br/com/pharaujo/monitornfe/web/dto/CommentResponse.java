package br.com.pharaujo.monitornfe.web.dto;

import java.time.Instant;
import java.util.List;

public record CommentResponse(
    Long id,
    String autor,
    String body,
    Instant createdAt,
    Instant updatedAt,
    boolean editado,
    List<AttachmentResponse> anexos
) {
}
