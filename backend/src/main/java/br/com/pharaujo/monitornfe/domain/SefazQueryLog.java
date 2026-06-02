package br.com.pharaujo.monitornfe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sefaz_query_logs")
public class SefazQueryLog extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cnpj", nullable = false, length = 14)
    private String cnpj;

    @Column(name = "ambiente", nullable = false, length = 20)
    private String ambiente;

    @Column(name = "nsu_inicial", nullable = false, length = 20)
    private String nsuInicial;

    @Column(name = "nsu_final", nullable = false, length = 20)
    private String nsuFinal;

    @Column(name = "cstat", nullable = false, length = 10)
    private String cstat;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;
}
