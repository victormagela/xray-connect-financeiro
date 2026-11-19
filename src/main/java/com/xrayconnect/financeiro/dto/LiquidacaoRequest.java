package com.xrayconnect.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record LiquidacaoRequest(
        @NotNull @PastOrPresent(message = "Data deve ser passada ou presente.") LocalDate dataLiquidacao
) {
}
