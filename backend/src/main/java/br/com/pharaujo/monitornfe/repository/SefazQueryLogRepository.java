package br.com.pharaujo.monitornfe.repository;

import br.com.pharaujo.monitornfe.domain.SefazQueryLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SefazQueryLogRepository extends JpaRepository<SefazQueryLog, Long> {
    List<SefazQueryLog> findTop50ByOrderByOccurredAtDesc();
}
