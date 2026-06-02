package br.com.pharaujo.monitornfe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "export_audits")
public class ExportAudit extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 10)
    private ExportType exportType;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "filters_json", nullable = false, length = 4000)
    private String filtersJson;

    @Column(name = "generated_file_path", nullable = false, length = 500)
    private String generatedFilePath;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;
}
