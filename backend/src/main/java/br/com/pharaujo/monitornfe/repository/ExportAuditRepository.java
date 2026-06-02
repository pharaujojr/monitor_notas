package br.com.pharaujo.monitornfe.repository;

import br.com.pharaujo.monitornfe.domain.ExportAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportAuditRepository extends JpaRepository<ExportAudit, Long> {
}
