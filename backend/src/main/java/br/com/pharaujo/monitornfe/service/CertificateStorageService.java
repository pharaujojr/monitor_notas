package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.config.AppProperties;
import br.com.pharaujo.monitornfe.domain.CertificateRecord;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CertificateStorageService {

    private final AppProperties appProperties;

    @Value("${CERT_PASSWORD:}")
    private String certPassword;

    public CertificateRecord store(MultipartFile file) {
        validate(file);
        try {
            byte[] bytes = file.getBytes();
            CertificateRecord record = buildMetadata(file, bytes);
            Path directory = Path.of(appProperties.getCertStoragePath()).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String extension = extension(file.getOriginalFilename());
            Path target = directory.resolve(UUID.randomUUID() + extension);
            Files.copy(new ByteArrayInputStream(bytes), target, StandardCopyOption.REPLACE_EXISTING);
            record.setStoragePath(target.toString());
            return record;
        } catch (IOException exception) {
            throw new BadRequestException("Nao foi possivel armazenar o certificado");
        }
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Arquivo de certificado vazio");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".pfx")) {
            throw new BadRequestException("Somente arquivos .pfx sao aceitos");
        }
        if (file.getSize() > 5 * 1024 * 1024L) {
            throw new BadRequestException("O certificado excede o limite de 5MB");
        }
    }

    private CertificateRecord buildMetadata(MultipartFile file, byte[] bytes) {
        CertificateRecord record = new CertificateRecord();
        record.setOriginalFilename(file.getOriginalFilename());
        record.setFileSizeBytes(file.getSize());
        record.setUploadedAt(OffsetDateTime.now());
        record.setSha256(sha256(bytes));
        extractValidity(bytes, record);
        return record;
    }

    private void extractValidity(byte[] bytes, CertificateRecord record) {
        if (certPassword == null || certPassword.isBlank()) {
            return;
        }
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(bytes), certPassword.toCharArray());
            String alias = keyStore.aliases().nextElement();
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
            ZoneId zone = ZoneId.systemDefault();
            record.setValidFrom(certificate.getNotBefore().toInstant().atZone(zone).toLocalDate());
            record.setValidTo(certificate.getNotAfter().toInstant().atZone(zone).toLocalDate());
        } catch (Exception ignored) {
            // Mantem upload funcional mesmo sem conseguir ler a validade.
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception exception) {
            throw new BadRequestException("Nao foi possivel calcular o hash do certificado");
        }
    }

    private String extension(String name) {
        int index = name.lastIndexOf('.');
        return index >= 0 ? name.substring(index) : ".pfx";
    }
}
