package com.xrayconnect.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FluxoCaixaResponse(
        LocalDate inicio,
        LocalDate fim,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldoPeriodo,
        List<ResumoMensalResponse> meses
) {
}