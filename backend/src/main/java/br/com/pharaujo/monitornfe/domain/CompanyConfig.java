package br.com.pharaujo.monitornfe.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "company_configs")
public class CompanyConfig extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cnpj", nullable = false, length = 14, unique = true)
    private String cnpj;

    @Column(name = "razao_social", nullable = false, length = 255)
    private String razaoSocial;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Enumerated(EnumType.STRING)
    @Column(name = "ambiente", nullable = false, length = 20)
    private EnvironmentType ambiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CompanyStatus status;

    @Column(name = "ult_nsu", nullable = false, length = 20)
    private String ultNsu = "0";

    @Column(name = "max_nsu", nullable = false, length = 20)
    private String maxNsu = "0";

    /** Momento a partir do qual uma nova consulta à SEFAZ é permitida (intervalo mínimo). */
    @Column(name = "proxima_consulta_permitida")
    private java.time.Instant proximaConsultaPermitida;

    /** Próxima sincronização agendada (cadência de 6h). */
    @Column(name = "proxima_sincronizacao")
    private java.time.Instant proximaSincronizacao;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "certificate_id")
    private CertificateRecord certificate;
}
