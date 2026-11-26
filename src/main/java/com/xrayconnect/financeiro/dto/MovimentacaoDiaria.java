package com.xrayconnect.financeiro.dto;

import com.xrayconnect.financeiro.enumeracao.TipoLancamento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimentacaoDiaria(
        LocalDate data,
        TipoLancamento tipo,
        BigDecimal total
) {
}