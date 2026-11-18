package com.xrayconnect.financeiro.repositorio;

import com.xrayconnect.financeiro.entidade.Categoria;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNomeAndTipo(String nome, TipoLancamento tipo);

    List<Categoria> id(Long id);
}
