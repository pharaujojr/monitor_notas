package br.com.pharaujo.monitornfe.web.dto;

public record AttachmentResponse(
    Long id,
    String originalFilename,
    String contentType,
    Long fileSizeBytes,
    String downloadUrl
) {
}
