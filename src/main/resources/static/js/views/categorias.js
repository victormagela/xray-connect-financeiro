/**
 * X-Ray Connect — cadastro e manutenção de categorias financeiras.
 * CRUD consumindo /api/categorias; paginação e ordenação feitas pelo servidor.
 */
import api from '../api.js';
import { escaparHtml, mensagemDeErro, obterErrosPorCampo } from '../utils.js';

const TAMANHO_PAGINA = 10;

export function renderCategorias() {
  return `
    <div class="cat-page">
      <div class="lf-toolbar">
        <div>
          <p class="lf-eyebrow">CONFIGURAÇÃO · CATEGORIAS</p>
          <p class="lf-toolbar-description">Organize os lançamentos em categorias de receitas e despesas.</p>
        </div>
        <button type="button" class="button primary" data-cat-new>+ Nova categoria</button>
      </div>

      <div class="lf-feedback" data-cat-feedback role="status" aria-live="polite" hidden></div>

      <section class="panel" aria-label="Categorias financeiras">
        <div class="panel-header lf-panel-header">
          <div>
            <h2 class="panel-title">Categorias cadastradas</h2>
            <p class="panel-subtitle" data-cat-count>Carregando categorias...</p>
          </div>
          <div class="lf-controls">
            <label for="cat-sort">Ordenar por</label>
            <select id="cat-sort" data-cat-sort>
              <option value="nome,asc">Nome (A–Z)</option>
              <option value="nome,desc">Nome (Z–A)</option>
              <option value="tipo,asc">Tipo</option>
              <option value="id,desc">Mais recentes</option>
            </select>
            <button type="button" class="button lf-button-small" data-cat-refresh>Atualizar</button>
          </div>
        </div>
        <div class="table-wrap">
          <table class="data-table cat-table">
            <thead>
              <tr>
                <th scope="col">Nome</th>
                <th scope="col">Tipo</th>
                <th scope="col">Descrição</th>
                <th scope="col">Ações</th>
              </tr>
            </thead>
            <tbody data-cat-rows>
              <tr><td class="empty-table" colspan="4">Carregando categorias...</td></tr>
            </tbody>
          </table>
        </div>
        <div class="lf-pagination">
          <span class="lf-page-info" data-cat-page-info>—</span>
          <div class="lf-page-buttons">
            <button type="button" class="button lf-button-small" data-cat-prev disabled>Anterior</button>
            <button type="button" class="button lf-button-small" data-cat-next disabled>Próxima</button>
          </div>
        </div>
      </section>

      <p class="lf-footnote">Categorias utilizadas em lançamentos não podem ser excluídas. O tipo também não pode ser alterado enquanto a categoria estiver em uso.</p>

      <dialog class="lf-dialog cat-dialog" data-cat-dialog aria-labelledby="cat-dialog-title">
        <form data-cat-form>
          <div class="lf-dialog-header">
            <div>
              <span class="lf-eyebrow">CADASTRO DE CATEGORIAS</span>
              <h2 id="cat-dialog-title" data-cat-title>Nova categoria</h2>
            </div>
            <button type="button" class="lf-close" data-cat-close aria-label="Fechar formulário">×</button>
          </div>
          <div class="lf-dialog-body">
            <div class="lf-feedback" data-cat-form-feedback role="alert" hidden></div>
            <div class="lf-field">
              <label for="cat-name">Nome <span aria-hidden="true">*</span></label>
              <input id="cat-name" name="nome" type="text" maxlength="100" required placeholder="Ex.: Venda de equipamentos" autocomplete="off">
              <small class="lf-field-error" data-cat-field-error="nome" hidden></small>
            </div>
            <div class="lf-field">
              <label for="cat-type">Tipo <span aria-hidden="true">*</span></label>
              <select id="cat-type" name="tipo" required>
                <option value="">Selecione...</option>
                <option value="RECEITA">Receita</option>
                <option value="DESPESA">Despesa</option>
              </select>
              <small class="lf-field-error" data-cat-field-error="tipo" hidden></small>
            </div>
            <div class="lf-field">
              <label for="cat-description">Descrição (opcional)</label>
              <textarea id="cat-description" name="descricao" maxlength="255" rows="3" placeholder="Explique quando essa categoria deve ser utilizada"></textarea>
              <small class="lf-field-error" data-cat-field-error="descricao" hidden></small>
              <p class="lf-help">Até 255 caracteres. O tipo deve corresponder ao dos lançamentos associados.</p>
            </div>
          </div>
          <div class="lf-dialog-actions">
            <button type="button" class="button" data-cat-close>Cancelar</button>
            <button type="submit" class="button primary" data-cat-save>Salvar categoria</button>
          </div>
        </form>
      </dialog>
    </div>
  `;
}

function linhaCategoria(categoria) {
  const id = Number(categoria.id);
  // IDs de entidades são números; jamais interpolar identificadores arbitrários em atributos HTML.
  const idValido = Number.isSafeInteger(id) && id > 0;
  const identificador = idValido ? String(id) : '';
  const receita = categoria.tipo === 'RECEITA';
  const tipo = receita ? 'Receita' : categoria.tipo === 'DESPESA' ? 'Despesa' : 'Desconhecido';
  const classe = receita ? 'cat-type-receita' : 'cat-type-despesa';
  const acoes = idValido
    ? `<div class="lf-row-actions">
         <button type="button" class="lf-action" data-cat-action="edit" data-id="${identificador}" aria-label="Editar categoria ${identificador}">Editar</button>
         <button type="button" class="lf-action lf-action-danger" data-cat-action="delete" data-id="${identificador}" aria-label="Excluir categoria ${identificador}">Excluir</button>
       </div>`
    : '<span class="lf-no-actions">Indisponível</span>';

  return `<tr>
    <td><strong class="lf-description">${escaparHtml(categoria.nome ?? '—')}</strong>
        <span class="lf-secondary">ID ${escaparHtml(identificador || '—')}</span></td>
    <td><span class="cat-type ${classe}">${escaparHtml(tipo)}</span></td>
    <td class="cat-description">${escaparHtml(categoria.descricao || 'Sem descrição')}</td>
    <td>${acoes}</td>
  </tr>`;
}

export function inicializarCategorias(container) {
  const $ = seletor => container.querySelector(seletor);
  const elementos = {
    linhas: $('[data-cat-rows]'),
    quantidade: $('[data-cat-count]'),
    pagina: $('[data-cat-page-info]'),
    anterior: $('[data-cat-prev]'),
    proxima: $('[data-cat-next]'),
    ordenar: $('[data-cat-sort]'),
    atualizar: $('[data-cat-refresh]'),
    novo: $('[data-cat-new]'),
    feedback: $('[data-cat-feedback]'),
    dialogo: $('[data-cat-dialog]'),
    formulario: $('[data-cat-form]'),
    titulo: $('[data-cat-title]'),
    salvar: $('[data-cat-save]'),
    feedbackFormulario: $('[data-cat-form-feedback]'),
  };

  let paginaAtual = 0;
  let totalPaginas = 0;
  let registros = [];
  let idEmEdicao = null;
  let processando = false;
  let sequenciaConsulta = 0;

  function feedback(elemento, texto, tipo = 'erro') {
    elemento.textContent = texto;
    elemento.className = `lf-feedback ${tipo === 'sucesso' ? 'lf-feedback-success' : 'lf-feedback-error'}`;
    elemento.hidden = !texto;
  }

  function limparErros() {
    elementos.dialogo.querySelectorAll('[data-cat-field-error]').forEach(campo => {
      campo.textContent = '';
      campo.hidden = true;
    });
  }

  function exibirErros(erro) {
    feedback(elementos.feedbackFormulario, mensagemDeErro(erro));
    const erros = obterErrosPorCampo(erro);
    for (const [nome, mensagens] of Object.entries(erros)) {
      const campo = [...elementos.dialogo.querySelectorAll('[data-cat-field-error]')]
        .find(elemento => elemento.dataset.catFieldError === nome);
      if (!campo) continue;
      campo.textContent = (Array.isArray(mensagens) ? mensagens : [mensagens]).join(' ');
      campo.hidden = false;
    }
  }

  async function carregarPagina() {
    const consultaAtual = ++sequenciaConsulta;
    elementos.linhas.innerHTML = '<tr><td class="empty-table" colspan="4">Carregando categorias...</td></tr>';
    elementos.quantidade.textContent = 'Consultando registros...';
    elementos.pagina.textContent = 'Carregando...';
    elementos.anterior.disabled = true;
    elementos.proxima.disabled = true;
    elementos.atualizar.disabled = true;

    try {
      const resposta = await api.categorias.listar({
        pagina: paginaAtual,
        tamanho: TAMANHO_PAGINA,
        ordenar: elementos.ordenar.value,
      });
      if (!container.isConnected || consultaAtual !== sequenciaConsulta) return;

      totalPaginas = Number(resposta.totalPages) || 0;
      if (totalPaginas > 0 && paginaAtual >= totalPaginas) {
        paginaAtual = totalPaginas - 1;
        return carregarPagina();
      }

      registros = Array.isArray(resposta.content) ? resposta.content : [];
      const total = Number(resposta.totalElements) || 0;
      elementos.quantidade.textContent = `${total.toLocaleString('pt-BR')} categoria(s) cadastrada(s)`;
      elementos.pagina.textContent = totalPaginas
        ? `Página ${paginaAtual + 1} de ${totalPaginas}` : 'Nenhuma categoria';
      elementos.anterior.disabled = paginaAtual === 0;
      elementos.proxima.disabled = totalPaginas === 0 || paginaAtual + 1 >= totalPaginas;
      elementos.linhas.innerHTML = registros.length
        ? registros.map(linhaCategoria).join('')
        : '<tr><td class="empty-table" colspan="4">Ainda não existem categorias cadastradas.<br>Use “Nova categoria” para começar.</td></tr>';
      if (elementos.feedback.classList.contains('lf-feedback-error')) feedback(elementos.feedback, '');
    } catch (erro) {
      if (!container.isConnected || consultaAtual !== sequenciaConsulta) return;
      registros = [];
      elementos.quantidade.textContent = 'Não foi possível consultar as categorias';
      elementos.pagina.textContent = '—';
      elementos.linhas.innerHTML = '<tr><td class="empty-table" colspan="4">Falha ao carregar categorias. Clique em “Atualizar” para tentar novamente.</td></tr>';
      feedback(elementos.feedback, mensagemDeErro(erro));
    } finally {
      if (container.isConnected && consultaAtual === sequenciaConsulta) elementos.atualizar.disabled = false;
    }
  }

  function abrirFormulario(categoria = null) {
    if (processando) return;
    idEmEdicao = categoria ? categoria.id : null;
    elementos.formulario.reset();
    limparErros();
    feedback(elementos.feedbackFormulario, '');
    elementos.titulo.textContent = categoria ? 'Editar categoria' : 'Nova categoria';
    elementos.salvar.textContent = categoria ? 'Salvar alterações' : 'Salvar categoria';
    if (categoria) {
      elementos.formulario.elements.nome.value = categoria.nome ?? '';
      elementos.formulario.elements.tipo.value = categoria.tipo ?? '';
      elementos.formulario.elements.descricao.value = categoria.descricao ?? '';
    }
    elementos.dialogo.showModal();
  }

  async function salvarFormulario(evento) {
    evento.preventDefault();
    if (processando || !elementos.formulario.reportValidity()) return;
    processando = true;
    elementos.salvar.disabled = true;
    limparErros();
    feedback(elementos.feedbackFormulario, '');

    const campos = elementos.formulario.elements;
    const dados = {
      nome: campos.nome.value.trim(),
      descricao: campos.descricao.value.trim() || null,
      tipo: campos.tipo.value,
    };
    const editando = idEmEdicao !== null;

    try {
      if (editando) await api.categorias.atualizar(idEmEdicao, dados);
      else await api.categorias.cadastrar(dados);
      if (!container.isConnected) return;
      elementos.dialogo.close();
      if (!editando) paginaAtual = 0;
      await carregarPagina();
      feedback(elementos.feedback,
        editando ? 'Categoria atualizada com sucesso.' : 'Categoria cadastrada com sucesso.',
        'sucesso');
    } catch (erro) {
      if (container.isConnected) exibirErros(erro);
    } finally {
      processando = false;
      if (container.isConnected) elementos.salvar.disabled = false;
    }
  }

  async function excluirCategoria(categoria) {
    if (processando) return;
    const confirmou = window.confirm(
      `Excluir a categoria “${categoria.nome}”?\n\nSe houver lançamentos vinculados, o servidor impedirá a exclusão.`,
    );
    if (!confirmou) return;
    processando = true;
    elementos.novo.disabled = true;
    try {
      await api.categorias.excluir(categoria.id);
      if (!container.isConnected) return;
      await carregarPagina();
      feedback(elementos.feedback, 'Categoria excluída com sucesso.', 'sucesso');
    } catch (erro) {
      if (container.isConnected) feedback(elementos.feedback, mensagemDeErro(erro));
    } finally {
      processando = false;
      if (container.isConnected) elementos.novo.disabled = false;
    }
  }

  elementos.novo.addEventListener('click', () => abrirFormulario());
  elementos.atualizar.addEventListener('click', () => {
    feedback(elementos.feedback, '');
    carregarPagina();
  });
  elementos.ordenar.addEventListener('change', () => {
    paginaAtual = 0;
    feedback(elementos.feedback, '');
    carregarPagina();
  });
  elementos.anterior.addEventListener('click', () => {
    if (paginaAtual > 0) { paginaAtual--; carregarPagina(); }
  });
  elementos.proxima.addEventListener('click', () => {
    if (paginaAtual + 1 < totalPaginas) { paginaAtual++; carregarPagina(); }
  });

  elementos.linhas.addEventListener('click', evento => {
    const botao = evento.target.closest('button[data-cat-action]');
    if (!botao || processando) return;
    const id = Number(botao.dataset.id);
    const categoria = registros.find(item => Number(item.id) === id);
    if (!categoria) return;
    if (botao.dataset.catAction === 'edit') abrirFormulario(categoria);
    if (botao.dataset.catAction === 'delete') excluirCategoria(categoria);
  });

  elementos.formulario.addEventListener('submit', salvarFormulario);
  container.querySelectorAll('[data-cat-close]').forEach(botao => {
    botao.addEventListener('click', () => { if (!processando) elementos.dialogo.close(); });
  });
  elementos.dialogo.addEventListener('cancel', evento => {
    if (processando) evento.preventDefault();
  });

  carregarPagina();
}
