package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.NfeNote;
import br.com.pharaujo.monitornfe.domain.NfeStatus;
import br.com.pharaujo.monitornfe.repository.NfeNoteRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfRegenerationService {

    private final NfeNoteRepository nfeNoteRepository;
    private final DanfePdfService danfePdfService;
    private final StorageService storageService;

    @Transactional
    public int regenerateAll() {
        int regenerated = 0;
        for (NfeNote note : nfeNoteRepository.findByXmlStoragePathIsNotNull()) {
            try {
                byte[] pdf = danfePdfService.generate(note.getChaveAcesso(), note.getEmitenteNome(), note.getXmlStoragePath());
                note.setPdfStoragePath(storageService.storePdf(note, pdf));
                note.setPdfGeneratedAt(OffsetDateTime.now());
                if (note.getStatus() != NfeStatus.CANCELADA) {
                    note.setStatus(NfeStatus.PDF_GERADO);
                }
                nfeNoteRepository.save(note);
                regenerated++;
            } catch (Exception exception) {
                log.warn("Falha ao regenerar PDF da nota {}: {}", note.getChaveAcesso(), exception.getMessage());
            }
        }
        return regenerated;
    }
}
