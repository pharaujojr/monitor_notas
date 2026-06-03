package br.com.pharaujo.monitornfe.web;

import br.com.pharaujo.monitornfe.service.SefazDistributionService;
import br.com.pharaujo.monitornfe.web.dto.SyncResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sefaz")
@RequiredArgsConstructor
public class SefazController {

    private final SefazDistributionService sefazDistributionService;

    /** Dispara a sincronização com a SEFAZ sob demanda (mesma lógica do agendador). */
    @PostMapping("/sincronizar")
    public SyncResultResponse sincronizar() {
        return sefazDistributionService.sincronizar();
    }
}
