package com.xrayconnect.financeiro.repositorio;

import com.xrayconnect.financeiro.entidade.LancamentoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiro, Long> {

    boolean existsByCategoria_Id(Long categoriaId);
}
