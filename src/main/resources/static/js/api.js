/**
 * X-Ray Connect — cliente HTTP da API REST.
 * O Spring Boot serve o front-end e a API na mesma origem (localhost:8080).
 * Nenhuma requisição acontece ao importar este módulo.
 */

/** Erro padronizado: mantém o código HTTP e os erros de campo do ProblemDetail. */
export class ErroApi extends Error {
  constructor(mensagem, { status = 0, erros = {}, detalhes = null } = {}) {
    super(mensagem);
    this.name = 'ErroApi';
    this.status = status;
    this.erros = erros;
    this.detalhes = detalhes;
  }
}

/**
 * Executa uma requisição à nossa API, envia JSON e interpreta ProblemDetail.
 * Retorna null quando a resposta não possui corpo (ex.: DELETE -> 204).
 */
export async function apiFetch(caminho, { method = 'GET', body, headers, ...opcoes } = {}) {
  const cabecalhos = new Headers(headers);
  cabecalhos.set('Accept', 'application/json');

  // Não definir Content-Type em requisições sem corpo.
  if (body !== undefined) {
    cabecalhos.set('Content-Type', 'application/json');
  }

  let resposta;
  try {
    resposta = await fetch(caminho, {
      ...opcoes,
      method,
      headers: cabecalhos,
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    });
  } catch (erro) {
    // Erros de cancelamento devem continuar reconhecíveis pelo navegador.
    if (erro?.name === 'AbortError') throw erro;
    throw new ErroApi('Não foi possível conectar ao servidor. Verifique se o Spring Boot está em execução.');
  }

  // Um 204 não possui JSON para desserializar.
  if (resposta.status === 204 || resposta.status === 205) {
    return null;
  }

  const conteudo = await resposta.text();
  const tipoConteudo = resposta.headers.get('content-type') || '';
  let dados = null;

  if (conteudo && tipoConteudo.includes('json')) {
    try {
      dados = JSON.parse(conteudo);
    } catch {
      throw new ErroApi('O servidor retornou um JSON inválido.', { status: resposta.status });
    }
  } else if (conteudo) {
    dados = conteudo;
  }

  if (!resposta.ok) {
    const problema = dados && typeof dados === 'object' ? dados : {};
    const mensagem = problema.detail || problema.message ||
      (resposta.status >= 500
        ? 'Ocorreu um erro no servidor. Tente novamente.'
        : `Não foi possível concluir a operação (HTTP ${resposta.status}).`);

    throw new ErroApi(mensagem, {
      status: resposta.status,
      erros: problema.erros || {},
      detalhes: problema,
    });
  }

  return dados;
}

/** Constrói query strings com filtros opcionais (sem enviar null ou undefined). */
function comParametros(caminho, parametros = {}) {
  const busca = new URLSearchParams();
  for (const [chave, valor] of Object.entries(parametros)) {
    if (valor !== null && valor !== undefined && valor !== '') {
      busca.set(chave, String(valor));
    }
  }
  const query = busca.toString();
  return query ? `${caminho}?${query}` : caminho;
}

const api = {
  categorias: {
    listar({ pagina = 0, tamanho = 10, ordenar = 'nome,asc' } = {}) {
      return apiFetch(comParametros('/api/categorias', {
        page: pagina, size: tamanho, sort: ordenar,
      }));
    },
    buscar(id) {
      return apiFetch(`/api/categorias/${encodeURIComponent(id)}`);
    },
    cadastrar(dados) {
      return apiFetch('/api/categorias', { method: 'POST', body: dados });
    },
    atualizar(id, dados) {
      return apiFetch(`/api/categorias/${encodeURIComponent(id)}`, { method: 'PUT', body: dados });
    },
    excluir(id) {
      return apiFetch(`/api/categorias/${encodeURIComponent(id)}`, { method: 'DELETE' });
    },
  },

  lancamentos: {
    listar({ tipo = null, pagina = 0, tamanho = 10, ordenar = 'dataVencimento,asc' } = {}) {
      return apiFetch(comParametros('/api/lancamentos', {
        tipo, page: pagina, size: tamanho, sort: ordenar,
      }));
    },
    buscar(id) {
      return apiFetch(`/api/lancamentos/${encodeURIComponent(id)}`);
    },
    cadastrar(dados) {
      return apiFetch('/api/lancamentos', { method: 'POST', body: dados });
    },
    atualizar(id, dados) {
      return apiFetch(`/api/lancamentos/${encodeURIComponent(id)}`, { method: 'PUT', body: dados });
    },
    excluir(id) {
      return apiFetch(`/api/lancamentos/${encodeURIComponent(id)}`, { method: 'DELETE' });
    },
    liquidar(id, dataLiquidacao) {
      return apiFetch(`/api/lancamentos/${encodeURIComponent(id)}/liquidar`, {
        method: 'PATCH', body: { dataLiquidacao },
      });
    },
  },

  relatorios: {
    dashboard() {
      return apiFetch('/api/relatorios/dashboard');
    },
    fluxoCaixa(inicio, fim) {
      return apiFetch(comParametros('/api/relatorios/fluxo-caixa', { inicio, fim }));
    },
  },
};

export default api;
