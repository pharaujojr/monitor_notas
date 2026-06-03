package br.com.pharaujo.monitornfe.repository;

import br.com.pharaujo.monitornfe.domain.ExternalXmlImportRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalXmlImportRecordRepository extends JpaRepository<ExternalXmlImportRecord, Long> {
    Optional<ExternalXmlImportRecord> findBySha256AndStatus(String sha256, String status);
}
