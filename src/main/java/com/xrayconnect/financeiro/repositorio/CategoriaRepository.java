package com.xrayconnect.financeiro.repositorio;

import com.xrayconnect.financeiro.entidade.Categoria;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNomeAndTipo(String nome, TipoLancamento tipo);
}
