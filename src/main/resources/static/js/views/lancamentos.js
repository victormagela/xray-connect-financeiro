/**
 * X-Ray Connect — uma única view reutilizada por Contas a Receber e a Pagar.
 * O tipo é fixado pela rota; filtros e paginação são feitos pelo Spring Boot.
 */
import api from '../api.js';
import {
  dataAtualISO,
  escaparHtml,
  formatarData,
  formatarMoeda,
  mensagemDeErro,
  obterErrosPorCampo,
  situacaoExibicao,
} from '../utils.js';

const TAMANHO_PAGINA = 10;
const TIPOS = {
  RECEITA: {
    titulo: 'Contas a receber',
    descricao: 'Acompanhe os valores previstos e os recebimentos já realizados.',
    acao: 'Recebido',
    singular: 'receita',
    contraparte: 'Cliente / contraparte',
  },
  DESPESA: {
    titulo: 'Contas a pagar',
    descricao: 'Controle os compromissos financeiros e os pagamentos da empresa.',
    acao: 'Pago',
    singular: 'despesa',
    contraparte: 'Fornecedor / contraparte',
  },
};

function configuracao(tipo) {
  if (!Object.hasOwn(TIPOS, tipo)) throw new Error(`Tipo de lançamento inválido: ${tipo}`);
  return TIPOS[tipo];
}

export function renderLancamentos(tipo) {
  const cfg = configuracao(tipo);
  return `
    <div class="lf-page">
      <div class="lf-toolbar">
        <div>
          <p class="lf-eyebrow">LANÇAMENTOS · ${tipo}</p>
          <p class="lf-toolbar-description">${cfg.descricao}</p>
        </div>
        <button type="button" class="button primary" data-lf-new>+ Novo lançamento</button>
      </div>

      <div class="lf-feedback" data-lf-feedback role="status" aria-live="polite" hidden></div>

      <section class="panel" aria-label="Lançamentos financeiros">
        <div class="panel-header lf-panel-header">
          <div>
            <h2 class="panel-title">Lançamentos</h2>
            <p class="panel-subtitle" data-lf-count>Carregando registros...</p>
          </div>
          <div class="lf-controls">
            <label for="lf-sort-${tipo}">Ordenar por</label>
            <select id="lf-sort-${tipo}" data-lf-sort>
              <option value="dataVencimento,asc">Vencimento (mais próximo)</option>
              <option value="dataVencimento,desc">Vencimento (mais distante)</option>
              <option value="valor,desc">Maior valor</option>
              <option value="valor,asc">Menor valor</option>
              <option value="id,desc">Mais recentes</option>
            </select>
            <button type="button" class="button lf-button-small" data-lf-refresh>Atualizar</button>
          </div>
        </div>
        <div class="table-wrap">
          <table class="data-table lf-table">
            <thead>
              <tr>
                <th scope="col">Descrição / Contraparte</th>
                <th scope="col">Categoria</th>
                <th scope="col">Valor</th>
                <th scope="col">Vencimento</th>
                <th scope="col">Situação</th>
                <th scope="col">Ações</th>
              </tr>
            </thead>
            <tbody data-lf-rows>
              <tr><td class="empty-table" colspan="6">Carregando lançamentos...</td></tr>
            </tbody>
          </table>
        </div>
        <div class="lf-pagination">
          <span class="lf-page-info" data-lf-page-info>—</span>
          <div class="lf-page-buttons">
            <button type="button" class="button lf-button-small" data-lf-prev disabled>Anterior</button>
            <button type="button" class="button lf-button-small" data-lf-next disabled>Próxima</button>
          </div>
        </div>
      </section>
      <p class="lf-footnote">Contas vencidas são lançamentos pendentes com data de vencimento anterior a hoje. Lançamentos liquidados não podem ser editados ou excluídos.</p>

      <dialog class="lf-dialog" data-lf-editor aria-labelledby="lf-editor-title-${tipo}">
        <form data-lf-form>
          <div class="lf-dialog-header">
            <div>
              <span class="lf-eyebrow">${tipo}</span>
              <h2 id="lf-editor-title-${tipo}" data-lf-editor-title>Novo lançamento</h2>
            </div>
            <button class="lf-close" type="button" data-lf-close-editor aria-label="Fechar formulário">×</button>
          </div>
          <div class="lf-dialog-body">
            <div class="lf-feedback" data-lf-form-feedback role="alert" hidden></div>
            <div class="lf-field">
              <label for="lf-categoria-${tipo}">Categoria <span aria-hidden="true">*</span></label>
              <select id="lf-categoria-${tipo}" name="categoriaId" required disabled>
                <option value="">Carregando categorias...</option>
              </select>
              <small class="lf-field-error" data-lf-field-error="categoriaId" hidden></small>
              <p class="lf-help" data-lf-category-help hidden>Cadastre primeiro uma categoria deste tipo na <a href="#categorias">tela de categorias</a>.</p>
            </div>
            <div class="lf-field">
              <label for="lf-descricao-${tipo}">Descrição <span aria-hidden="true">*</span></label>
              <input id="lf-descricao-${tipo}" name="descricao" type="text" maxlength="255" required placeholder="Ex.: Venda de equipamento" />
              <small class="lf-field-error" data-lf-field-error="descricao" hidden></small>
            </div>
            <div class="lf-field">
              <label for="lf-contraparte-${tipo}">${cfg.contraparte}</label>
              <input id="lf-contraparte-${tipo}" name="nomeContraparte" type="text" maxlength="120" placeholder="Opcional" />
              <small class="lf-field-error" data-lf-field-error="nomeContraparte" hidden></small>
            </div>
            <div class="lf-field-grid">
              <div class="lf-field">
                <label for="lf-valor-${tipo}">Valor (R$) <span aria-hidden="true">*</span></label>
                <input id="lf-valor-${tipo}" name="valor" type="number" min="0.01" step="0.01" inputmode="decimal" required placeholder="0,00" />
                <small class="lf-field-error" data-lf-field-error="valor" hidden></small>
              </div>
              <div class="lf-field">
                <label for="lf-vencimento-${tipo}">Vencimento <span aria-hidden="true">*</span></label>
                <input id="lf-vencimento-${tipo}" name="dataVencimento" type="date" required />
                <small class="lf-field-error" data-lf-field-error="dataVencimento" hidden></small>
              </div>
            </div>
            <p class="lf-help">O lançamento será criado como pendente. A liquidação é registrada separadamente.</p>
          </div>
          <div class="lf-dialog-actions">
            <button type="button" class="button" data-lf-close-editor>Cancelar</button>
            <button type="submit" class="button primary" data-lf-save disabled>Salvar lançamento</button>
          </div>
        </form>
      </dialog>

      <dialog class="lf-dialog lf-dialog-small" data-lf-settlement aria-labelledby="lf-settlement-title-${tipo}">
        <form data-lf-settlement-form>
          <div class="lf-dialog-header">
            <div>
              <span class="lf-eyebrow">REGISTRAR LIQUIDAÇÃO</span>
              <h2 id="lf-settlement-title-${tipo}">${cfg.acao === 'Recebido' ? 'Confirmar recebimento' : 'Confirmar pagamento'}</h2>
            </div>
            <button class="lf-close" type="button" data-lf-close-settlement aria-label="Fechar">×</button>
          </div>
          <div class="lf-dialog-body">
            <p class="lf-settlement-description" data-lf-settlement-description></p>
            <div class="lf-feedback" data-lf-settlement-feedback role="alert" hidden></div>
            <div class="lf-field">
              <label for="lf-data-liquidacao-${tipo}">Data de liquidação <span aria-hidden="true">*</span></label>
              <input id="lf-data-liquidacao-${tipo}" type="date" name="dataLiquidacao" required />
              <small class="lf-field-error" data-lf-field-error="dataLiquidacao" hidden></small>
            </div>
            <p class="lf-help">A data não pode ser futura. Após a liquidação, a conta não poderá ser editada ou excluída.</p>
          </div>
          <div class="lf-dialog-actions">
            <button type="button" class="button" data-lf-close-settlement>Cancelar</button>
            <button type="submit" class="button primary" data-lf-confirm-settlement>Confirmar</button>
          </div>
        </form>
      </dialog>
    </div>
  `;
}

/** Todos os textos vindos do banco são escapados antes de entrar no HTML. */
function linhaLancamento(lancamento) {
  const pendente = lancamento.situacaoLancamento === 'PENDENTE';
  const situacao = situacaoExibicao(lancamento);
  const classe = situacao === 'Vencido' ? 'vencido'
    : situacao === 'Liquidado' ? 'liquidado' : 'pendente';
  const id = Number(lancamento.id);
  const idValido = Number.isSafeInteger(id) && id > 0;
  const idAtributo = idValido ? String(id) : '';
  const acoes = pendente && idValido
    ? `<div class="lf-row-actions">
         <button type="button" class="lf-action" data-lf-action="edit" data-id="${idAtributo}" aria-label="Editar lançamento ${idAtributo}">Editar</button>
         <button type="button" class="lf-action lf-action-primary" data-lf-action="settle" data-id="${idAtributo}" aria-label="Liquidar lançamento ${idAtributo}">Liquidar</button>
         <button type="button" class="lf-action lf-action-danger" data-lf-action="delete" data-id="${idAtributo}" aria-label="Excluir lançamento ${idAtributo}">Excluir</button>
       </div>`
    : '<span class="lf-no-actions">Sem ações disponíveis</span>';

  return `<tr>
    <td><strong class="lf-description">${escaparHtml(lancamento.descricao || '—')}</strong>
      <span class="lf-secondary">${escaparHtml(lancamento.nomeContraparte || 'Sem contraparte')}</span></td>
    <td>${escaparHtml(lancamento.categoriaNome || `Categoria #${lancamento.categoriaId ?? '?'}`)}</td>
    <td class="lf-amount">${escaparHtml(formatarMoeda(lancamento.valor))}</td>
    <td>${escaparHtml(formatarData(lancamento.dataVencimento))}</td>
    <td><span class="lf-status lf-status-${classe}">${escaparHtml(situacao)}</span>
      ${!pendente && lancamento.dataLiquidacao ? `<span class="lf-secondary">${escaparHtml(formatarData(lancamento.dataLiquidacao))}</span>` : ''}</td>
    <td>${acoes}</td>
  </tr>`;
}

/**
 * Registra os eventos e carrega a primeira página. As checagens de conexão
 * evitam modificar uma view que foi desmontada após uma mudança de rota.
 */
export function inicializarLancamentos(container, tipo) {
  configuracao(tipo);
  const $ = seletor => container.querySelector(seletor);
  const elementos = {
    linhas: $('[data-lf-rows]'),
    quantidade: $('[data-lf-count]'),
    pagina: $('[data-lf-page-info]'),
    anterior: $('[data-lf-prev]'),
    proxima: $('[data-lf-next]'),
    ordenar: $('[data-lf-sort]'),
    atualizar: $('[data-lf-refresh]'),
    novo: $('[data-lf-new]'),
    feedback: $('[data-lf-feedback]'),
    editor: $('[data-lf-editor]'),
    formulario: $('[data-lf-form]'),
    salvar: $('[data-lf-save]'),
    feedbackFormulario: $('[data-lf-form-feedback]'),
    categoria: $('[name="categoriaId"]'),
    avisoCategoria: $('[data-lf-category-help]'),
    liquidacao: $('[data-lf-settlement]'),
    formularioLiquidacao: $('[data-lf-settlement-form]'),
    feedbackLiquidacao: $('[data-lf-settlement-feedback]'),
    confirmarLiquidacao: $('[data-lf-confirm-settlement]'),
  };

  let numeroPagina = 0;
  let totalPaginas = 0;
  let registros = [];
  let idEmEdicao = null;
  let idEmLiquidacao = null;
  let contadorCarregamentos = 0;
  let emProcessamento = false;

  function feedback(elemento, texto, natureza = 'erro') {
    elemento.textContent = texto;
    elemento.className = `lf-feedback ${natureza === 'sucesso' ? 'lf-feedback-success' : 'lf-feedback-error'}`;
    elemento.hidden = !texto;
  }

  function limparErrosFormulario(dialogo) {
    dialogo.querySelectorAll('[data-lf-field-error]').forEach(item => {
      item.textContent = '';
      item.hidden = true;
    });
  }

  function mostrarErrosFormulario(dialogo, erro, alvo) {
    feedback(alvo, mensagemDeErro(erro));
    const erros = obterErrosPorCampo(erro);
    for (const [nome, mensagens] of Object.entries(erros)) {
      const campo = [...dialogo.querySelectorAll('[data-lf-field-error]')]
        .find(item => item.dataset.lfFieldError === nome);
      if (campo) {
        campo.textContent = (Array.isArray(mensagens) ? mensagens : [mensagens]).join(' ');
        campo.hidden = false;
      }
    }
  }

  async function carregarPagina() {
    const execucao = ++contadorCarregamentos;
    elementos.linhas.innerHTML = '<tr><td colspan="6" class="empty-table">Carregando lançamentos...</td></tr>';
    elementos.quantidade.textContent = 'Consultando registros...';
    elementos.pagina.textContent = 'Carregando...';
    elementos.anterior.disabled = true;
    elementos.proxima.disabled = true;
    elementos.atualizar.disabled = true;

    try {
      const resposta = await api.lancamentos.listar({
        tipo,
        pagina: numeroPagina,
        tamanho: TAMANHO_PAGINA,
        ordenar: elementos.ordenar.value,
      });
      if (!container.isConnected || execucao !== contadorCarregamentos) return;

      totalPaginas = Number(resposta.totalPages) || 0;
      if (totalPaginas > 0 && numeroPagina >= totalPaginas) {
        numeroPagina = totalPaginas - 1;
        return carregarPagina();
      }

      if (elementos.feedback.classList.contains('lf-feedback-error')) feedback(elementos.feedback, '');
      registros = Array.isArray(resposta.content) ? resposta.content : [];
      const total = Number(resposta.totalElements) || 0;
      elementos.quantidade.textContent = `${total.toLocaleString('pt-BR')} lançamento(s) encontrado(s)`;
      elementos.pagina.textContent = totalPaginas
        ? `Página ${numeroPagina + 1} de ${totalPaginas}` : 'Nenhum lançamento';
      elementos.anterior.disabled = numeroPagina === 0;
      elementos.proxima.disabled = totalPaginas === 0 || numeroPagina + 1 >= totalPaginas;
      elementos.linhas.innerHTML = registros.length
        ? registros.map(linhaLancamento).join('')
        : `<tr><td colspan="6" class="empty-table">Nenhuma ${TIPOS[tipo].singular} cadastrada nesta listagem.<br>Utilize “Novo lançamento” para começar.</td></tr>`;
    } catch (erro) {
      if (!container.isConnected || execucao !== contadorCarregamentos) return;
      registros = [];
      elementos.quantidade.textContent = 'Não foi possível consultar os lançamentos';
      elementos.pagina.textContent = '—';
      elementos.linhas.innerHTML = '<tr><td colspan="6" class="empty-table">Falha ao carregar. Clique em “Atualizar” para tentar novamente.</td></tr>';
      feedback(elementos.feedback, mensagemDeErro(erro));
    } finally {
      if (container.isConnected && execucao === contadorCarregamentos) {
        elementos.atualizar.disabled = false;
      }
    }
  }

  /** Busca todas as páginas porque a categoria desejada pode não estar na primeira. */
  async function carregarCategorias() {
    const categorias = [];
    let pagina = 0;
    let paginasTotais = 1;
    do {
      const resposta = await api.categorias.listar({ pagina, tamanho: 100, ordenar: 'nome,asc' });
      categorias.push(...(Array.isArray(resposta.content) ? resposta.content : []));
      paginasTotais = Number(resposta.totalPages) || 0;
      pagina += 1;
    } while (pagina < paginasTotais);
    return categorias.filter(categoria => categoria.tipo === tipo);
  }

  async function abrirFormulario(id = null) {
    if (emProcessamento) return;
    idEmEdicao = id;
    elementos.formulario.reset();
    limparErrosFormulario(elementos.editor);
    feedback(elementos.feedbackFormulario, '');
    elementos.avisoCategoria.hidden = true;
    elementos.categoria.disabled = true;
    elementos.categoria.replaceChildren(new Option('Carregando categorias...', ''));
    elementos.salvar.disabled = true;
    $('[data-lf-editor-title]').textContent = id === null ? 'Novo lançamento' : 'Editar lançamento';
    elementos.editor.showModal();

    try {
      const [categorias, lancamento] = await Promise.all([
        carregarCategorias(),
        id === null ? Promise.resolve(null) : api.lancamentos.buscar(id),
      ]);
      if (!container.isConnected || !elementos.editor.open || idEmEdicao !== id) return;

      elementos.categoria.replaceChildren(new Option('Selecione uma categoria', ''));
      for (const categoria of categorias) {
        elementos.categoria.add(new Option(categoria.nome, String(categoria.id)));
      }
      elementos.avisoCategoria.hidden = categorias.length > 0;
      elementos.categoria.disabled = categorias.length === 0;
      elementos.salvar.disabled = categorias.length === 0;

      if (lancamento) {
        if (lancamento.situacaoLancamento !== 'PENDENTE') {
          throw new Error('Somente lançamentos pendentes podem ser editados.');
        }
        elementos.formulario.elements.categoriaId.value = String(lancamento.categoriaId);
        elementos.formulario.elements.descricao.value = lancamento.descricao ?? '';
        elementos.formulario.elements.nomeContraparte.value = lancamento.nomeContraparte ?? '';
        elementos.formulario.elements.valor.value = lancamento.valor ?? '';
        elementos.formulario.elements.dataVencimento.value = lancamento.dataVencimento ?? '';
      } else {
        elementos.formulario.elements.dataVencimento.value = dataAtualISO();
      }
    } catch (erro) {
      if (!container.isConnected || !elementos.editor.open || idEmEdicao !== id) return;
      elementos.salvar.disabled = true;
      feedback(elementos.feedbackFormulario, mensagemDeErro(erro));
    }
  }

  async function salvarFormulario(evento) {
    evento.preventDefault();
    if (emProcessamento || !elementos.formulario.reportValidity()) return;
    emProcessamento = true;
    elementos.salvar.disabled = true;
    limparErrosFormulario(elementos.editor);
    feedback(elementos.feedbackFormulario, '');
    const campos = elementos.formulario.elements;
    const dados = {
      categoriaId: Number(campos.categoriaId.value),
      descricao: campos.descricao.value.trim(),
      nomeContraparte: campos.nomeContraparte.value.trim() || null,
      valor: campos.valor.value,
      tipoLancamento: tipo,
      dataVencimento: campos.dataVencimento.value,
    };
    const editando = idEmEdicao !== null;
    try {
      if (editando) await api.lancamentos.atualizar(idEmEdicao, dados);
      else await api.lancamentos.cadastrar(dados);
      if (!container.isConnected) return;
      elementos.editor.close();
      if (!editando) numeroPagina = 0;
      await carregarPagina();
      feedback(elementos.feedback, editando ? 'Lançamento atualizado com sucesso.' : 'Lançamento cadastrado com sucesso.', 'sucesso');
    } catch (erro) {
      if (container.isConnected) mostrarErrosFormulario(elementos.editor, erro, elementos.feedbackFormulario);
    } finally {
      emProcessamento = false;
      if (container.isConnected && elementos.editor.open) elementos.salvar.disabled = false;
    }
  }

  function abrirLiquidacao(lancamento) {
    if (emProcessamento || lancamento.situacaoLancamento !== 'PENDENTE') return;
    idEmLiquidacao = lancamento.id;
    elementos.formularioLiquidacao.reset();
    limparErrosFormulario(elementos.liquidacao);
    feedback(elementos.feedbackLiquidacao, '');
    const campo = elementos.formularioLiquidacao.elements.dataLiquidacao;
    campo.max = dataAtualISO();
    campo.value = dataAtualISO();
    $('[data-lf-settlement-description]').textContent =
      `${lancamento.descricao} — ${formatarMoeda(lancamento.valor)}.`;
    elementos.liquidacao.showModal();
  }

  async function confirmarLiquidacao(evento) {
    evento.preventDefault();
    if (emProcessamento || !elementos.formularioLiquidacao.reportValidity()) return;
    emProcessamento = true;
    elementos.confirmarLiquidacao.disabled = true;
    feedback(elementos.feedbackLiquidacao, '');
    limparErrosFormulario(elementos.liquidacao);
    try {
      await api.lancamentos.liquidar(
        idEmLiquidacao,
        elementos.formularioLiquidacao.elements.dataLiquidacao.value,
      );
      if (!container.isConnected) return;
      elementos.liquidacao.close();
      await carregarPagina();
      feedback(elementos.feedback, 'Liquidação registrada com sucesso.', 'sucesso');
    } catch (erro) {
      if (container.isConnected) mostrarErrosFormulario(elementos.liquidacao, erro, elementos.feedbackLiquidacao);
    } finally {
      emProcessamento = false;
      if (container.isConnected) elementos.confirmarLiquidacao.disabled = false;
    }
  }

  async function excluir(lancamento) {
    if (emProcessamento || lancamento.situacaoLancamento !== 'PENDENTE') return;
    const confirmar = window.confirm(
      `Excluir o lançamento “${lancamento.descricao}” (${formatarMoeda(lancamento.valor)})?\n\nEsta ação não pode ser desfeita.`,
    );
    if (!confirmar) return;
    emProcessamento = true;
    try {
      await api.lancamentos.excluir(lancamento.id);
      if (!container.isConnected) return;
      await carregarPagina();
      feedback(elementos.feedback, 'Lançamento excluído com sucesso.', 'sucesso');
    } catch (erro) {
      if (container.isConnected) feedback(elementos.feedback, mensagemDeErro(erro));
    } finally {
      emProcessamento = false;
    }
  }

  elementos.novo.addEventListener('click', () => abrirFormulario());
  elementos.atualizar.addEventListener('click', () => {
    feedback(elementos.feedback, '');
    carregarPagina();
  });
  elementos.ordenar.addEventListener('change', () => {
    numeroPagina = 0;
    feedback(elementos.feedback, '');
    carregarPagina();
  });
  elementos.anterior.addEventListener('click', () => {
    if (numeroPagina > 0) { numeroPagina--; carregarPagina(); }
  });
  elementos.proxima.addEventListener('click', () => {
    if (numeroPagina + 1 < totalPaginas) { numeroPagina++; carregarPagina(); }
  });

  elementos.linhas.addEventListener('click', evento => {
    const botao = evento.target.closest('button[data-lf-action]');
    if (!botao) return;
    const id = Number(botao.dataset.id);
    const lancamento = registros.find(registro => Number(registro.id) === id);
    if (!lancamento || lancamento.situacaoLancamento !== 'PENDENTE') return;
    if (botao.dataset.lfAction === 'edit') abrirFormulario(id);
    if (botao.dataset.lfAction === 'settle') abrirLiquidacao(lancamento);
    if (botao.dataset.lfAction === 'delete') excluir(lancamento);
  });

  elementos.formulario.addEventListener('submit', salvarFormulario);
  elementos.formularioLiquidacao.addEventListener('submit', confirmarLiquidacao);
  container.querySelectorAll('[data-lf-close-editor]').forEach(botao => {
    botao.addEventListener('click', () => { if (!emProcessamento) elementos.editor.close(); });
  });
  container.querySelectorAll('[data-lf-close-settlement]').forEach(botao => {
    botao.addEventListener('click', () => { if (!emProcessamento) elementos.liquidacao.close(); });
  });
  // Não permitir fechar os diálogos via Escape durante uma operação de escrita.
  for (const dialogo of [elementos.editor, elementos.liquidacao]) {
    dialogo.addEventListener('cancel', evento => { if (emProcessamento) evento.preventDefault(); });
  }

  carregarPagina();
}
