package br.com.pharaujo.monitornfe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "nfe_notes")
public class NfeNote extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_config_id", nullable = false)
    private CompanyConfig companyConfig;

    @Column(name = "chave_acesso", nullable = false, length = 44, unique = true)
    private String chaveAcesso;

    @Column(name = "nsu", nullable = false, length = 20, unique = true)
    private String nsu;

    @Column(name = "modelo", nullable = false, length = 2)
    private String modelo;

    @Column(name = "emitente_cnpj", nullable = false, length = 14)
    private String emitenteCnpj;

    @Column(name = "emitente_nome", nullable = false, length = 255)
    private String emitenteNome;

    @Column(name = "destinatario_cnpj", nullable = false, length = 14)
    private String destinatarioCnpj;

    @Column(name = "data_emissao", nullable = false)
    private LocalDateTime dataEmissao;

    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private NfeStatus status;

    @Column(name = "xml_storage_path", length = 500)
    private String xmlStoragePath;

    @Column(name = "pdf_storage_path", length = 500)
    private String pdfStoragePath;

    @Column(name = "xml_downloaded_at")
    private OffsetDateTime xmlDownloadedAt;

    @Column(name = "pdf_generated_at")
    private OffsetDateTime pdfGeneratedAt;
}
