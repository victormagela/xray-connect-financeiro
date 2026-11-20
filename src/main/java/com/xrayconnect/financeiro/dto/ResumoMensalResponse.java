package com.xrayconnect.financeiro.dto;

import java.math.BigDecimal;

public record ResumoMensalResponse(
        String mes,
        BigDecimal receitas,
        BigDecimal despesas,
        BigDecimal saldo
) {
}