package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.CompanyStatus;
import br.com.pharaujo.monitornfe.domain.EnvironmentType;
import br.com.pharaujo.monitornfe.web.dto.CompanyConfigRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SampleDataInitializer implements CommandLineRunner {

    private final CompanyConfigService companyConfigService;
    private final SefazDistributionService sefazDistributionService;

    @Override
    public void run(String... args) {
        if (companyConfigService.getCurrent() == null) {
            companyConfigService.save(new CompanyConfigRequest(
                "12345678000195",
                "Empresa Monitorada Exemplo LTDA",
                "MT",
                EnvironmentType.HOMOLOGACAO,
                CompanyStatus.ATIVO
            ));
        }
        sefazDistributionService.bootstrapSampleData();
    }
}
