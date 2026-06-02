package br.com.pharaujo.monitornfe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "nfe_events")
public class NfeEvent extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nfe_note_id", nullable = false)
    private NfeNote note;

    @Column(name = "event_code", nullable = false, length = 6)
    private String eventCode;

    @Column(name = "event_name", nullable = false, length = 120)
    private String eventName;

    @Column(name = "event_protocol", length = 40)
    private String eventProtocol;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "details", length = 4000)
    private String details;
}
