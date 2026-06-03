package br.com.pharaujo.monitornfe.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotBlank
    private String storagePath;

    @NotBlank
    private String certStoragePath;

    @NotBlank
    private String sefazAmbiente;

    @NotBlank
    private String schedulerDistributionCron;

    private boolean erpXmlImportEnabled = true;

    @NotBlank
    private String erpXmlImportPath;

    @NotBlank
    private String erpXmlImportCron;

    private boolean sefazFallbackEnabled = false;

    private int sefazMaxConsultasPorExecucao = 2;
}
