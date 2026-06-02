package br.com.pharaujo.monitornfe.repository;

import br.com.pharaujo.monitornfe.domain.NfeNote;
import br.com.pharaujo.monitornfe.domain.NfeStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NfeNoteRepository extends JpaRepository<NfeNote, Long> {

    Optional<NfeNote> findByChaveAcesso(String chaveAcesso);

    @Query("""
        select n from NfeNote n
        where (:status is null or n.status = :status)
          and (:emitenteNome is null or lower(n.emitenteNome) like lower(concat('%', :emitenteNome, '%')))
          and (:chave is null or n.chaveAcesso = :chave)
          and (:dataInicial is null or n.dataEmissao >= :dataInicial)
          and (:dataFinal is null or n.dataEmissao < :dataFinal)
        order by n.dataEmissao desc
        """)
    Page<NfeNote> search(
        @Param("status") NfeStatus status,
        @Param("emitenteNome") String emitenteNome,
        @Param("chave") String chave,
        @Param("dataInicial") LocalDateTime dataInicial,
        @Param("dataFinal") LocalDateTime dataFinal,
        Pageable pageable
    );

    long countByStatus(NfeStatus status);

    List<NfeNote> findTop10ByOrderByDataEmissaoDesc();
}
