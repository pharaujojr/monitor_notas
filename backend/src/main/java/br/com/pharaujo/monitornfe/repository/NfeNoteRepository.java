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

    @Query(value = """
        select * from nfe_notes n
        where (:status is null or n.status = cast(:status as varchar))
          and (:emitenteNome is null or lower(n.emitente_nome) like lower(concat('%', cast(:emitenteNome as varchar), '%')))
          and (:chave is null or n.chave_acesso = cast(:chave as varchar))
          and (:dataInicial is null or n.data_emissao >= :dataInicial)
          and (:dataFinal is null or n.data_emissao < :dataFinal)
        order by n.data_emissao desc
        """,
        countQuery = """
        select count(*) from nfe_notes n
        where (:status is null or n.status = cast(:status as varchar))
          and (:emitenteNome is null or lower(n.emitente_nome) like lower(concat('%', cast(:emitenteNome as varchar), '%')))
          and (:chave is null or n.chave_acesso = cast(:chave as varchar))
          and (:dataInicial is null or n.data_emissao >= :dataInicial)
          and (:dataFinal is null or n.data_emissao < :dataFinal)
        """,
        nativeQuery = true)
    Page<NfeNote> search(
        @Param("status") String status,
        @Param("emitenteNome") String emitenteNome,
        @Param("chave") String chave,
        @Param("dataInicial") LocalDateTime dataInicial,
        @Param("dataFinal") LocalDateTime dataFinal,
        Pageable pageable
    );

    long countByStatus(NfeStatus status);

    List<NfeNote> findTop10ByOrderByDataEmissaoDesc();
}
