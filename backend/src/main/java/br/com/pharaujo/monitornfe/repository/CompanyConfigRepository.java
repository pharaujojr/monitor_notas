package br.com.pharaujo.monitornfe.repository;

import br.com.pharaujo.monitornfe.domain.CompanyConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyConfigRepository extends JpaRepository<CompanyConfig, Long> {
    Optional<CompanyConfig> findTopByOrderByIdAsc();
}
