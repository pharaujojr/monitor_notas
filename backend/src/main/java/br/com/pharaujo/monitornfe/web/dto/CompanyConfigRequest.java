package br.com.pharaujo.monitornfe.web.dto;

import br.com.pharaujo.monitornfe.domain.CompanyStatus;
import br.com.pharaujo.monitornfe.domain.EnvironmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CompanyConfigRequest(
    @NotBlank
    @Pattern(regexp = "\\d{14}")
    String cnpj,
    @NotBlank
    String razaoSocial,
    @NotBlank
    @Pattern(regexp = "[A-Z]{2}")
    String uf,
    @NotNull
    EnvironmentType ambiente,
    @NotNull
    CompanyStatus status
) {
}
