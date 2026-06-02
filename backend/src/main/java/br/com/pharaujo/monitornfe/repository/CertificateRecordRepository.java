package br.com.pharaujo.monitornfe.repository;

import br.com.pharaujo.monitornfe.domain.CertificateRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRecordRepository extends JpaRepository<CertificateRecord, Long> {
}
