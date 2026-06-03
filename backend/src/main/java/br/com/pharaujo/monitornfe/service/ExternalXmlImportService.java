package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.config.AppProperties;
import br.com.pharaujo.monitornfe.domain.CompanyConfig;
import br.com.pharaujo.monitornfe.domain.ExternalXmlImportRecord;
import br.com.pharaujo.monitornfe.repository.ExternalXmlImportRecordRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalXmlImportService {

    private static final String STATUS_IMPORTED = "IMPORTED";
    private static final String STATUS_IGNORED = "IGNORED";
    private static final String STATUS_ERROR = "ERROR";
    private static final int MAX_FILES_PER_RUN = 500;

    private final AppProperties appProperties;
    private final CompanyConfigService companyConfigService;
    private final DistribuicaoProcessor distribuicaoProcessor;
    private final ExternalXmlImportRecordRepository importRecordRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${app.erp-xml-import-cron}")
    public void scheduledImport() {
        if (appProperties.isErpXmlImportEnabled()) {
            importar();
        }
    }

    @Transactional
    public int importar() {
        if (!running.compareAndSet(false, true)) {
            return 0;
        }
        try {
            CompanyConfig company = companyConfigService.getRequiredEntity();
            Path root = Path.of(appProperties.getErpXmlImportPath()).toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) {
                log.warn("Diretorio de XML do ERP nao encontrado: {}", root);
                return 0;
            }
            try (Stream<Path> files = Files.walk(root)) {
                return files
                    .filter(Files::isRegularFile)
                    .filter(this::isXml)
                    .limit(MAX_FILES_PER_RUN)
                    .mapToInt(path -> importarArquivo(company, path))
                    .sum();
            }
        } catch (Exception exception) {
            log.warn("Falha no importador de XML externo: {}", exception.getMessage());
            return 0;
        } finally {
            running.set(false);
        }
    }

    private int importarArquivo(CompanyConfig company, Path path) {
        String sha256 = "unknown";
        try {
            byte[] bytes = Files.readAllBytes(path);
            sha256 = sha256(bytes);
            if (importRecordRepository.findBySha256AndStatus(sha256, STATUS_IMPORTED).isPresent()) {
                return 0;
            }
            String xml = new String(bytes, StandardCharsets.UTF_8);
            String nsuSintetico = "ERP" + sha256.substring(0, 17);
            boolean imported = distribuicaoProcessor.processarXmlCompleto(company, nsuSintetico, xml, "ERP externo");
            registrar(path, sha256, imported ? STATUS_IMPORTED : STATUS_IGNORED, chave(xml), imported ? "XML importado" : "XML ignorado");
            return imported ? 1 : 0;
        } catch (Exception exception) {
            registrar(path, sha256, STATUS_ERROR, null, exception.getMessage());
            return 0;
        }
    }

    private boolean isXml(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".xml");
    }

    private void registrar(Path path, String sha256, String status, String chave, String message) {
        ExternalXmlImportRecord record = new ExternalXmlImportRecord();
        record.setSourcePath(path.toAbsolutePath().normalize().toString());
        record.setSha256(sha256);
        record.setStatus(status);
        record.setChaveAcesso(chave);
        record.setMessage(message);
        record.setImportedAt(Instant.now());
        importRecordRepository.save(record);
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception exception) {
            throw new BadRequestException("Nao foi possivel calcular hash do XML externo");
        }
    }

    private String chave(String xml) {
        int start = xml.indexOf("Id=\"NFe");
        int offset = 7;
        if (start < 0) {
            start = xml.indexOf("Id='NFe");
        }
        if (start < 0 || start + offset + 44 > xml.length()) {
            return null;
        }
        return xml.substring(start + offset, start + offset + 44);
    }
}
