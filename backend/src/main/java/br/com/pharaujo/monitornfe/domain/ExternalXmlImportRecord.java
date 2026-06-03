package br.com.pharaujo.monitornfe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "external_xml_import_records")
public class ExternalXmlImportRecord extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_path", nullable = false, length = 1000)
    private String sourcePath;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "chave_acesso", length = 44)
    private String chaveAcesso;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
