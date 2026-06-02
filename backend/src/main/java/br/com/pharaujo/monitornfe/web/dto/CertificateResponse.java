package br.com.pharaujo.monitornfe.web.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CertificateResponse(
    Long id,
    String originalFilename,
    Long fileSizeBytes,
    LocalDate validFrom,
    LocalDate validTo,
    OffsetDateTime uploadedAt
) {
}
