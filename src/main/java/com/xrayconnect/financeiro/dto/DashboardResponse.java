package com.xrayconnect.financeiro.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        BigDecimal totalAReceber,
        BigDecimal totalAPagar,
        BigDecimal totalRecebido,
        BigDecimal totalPago,
        BigDecimal saldoRealizado,
        long quantidadeVencidos
) {
}
