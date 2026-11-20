/**
 * X-Ray Connect — funções para as views.
 * Somente apresentação: cálculos financeiros são responsabilidade do back-end.
 */

const moedaBR = new Intl.NumberFormat('pt-BR', {
  style: 'currency', currency: 'BRL', minimumFractionDigits: 2,
});

/** Valor numérico recebido da API -> "R$ 1.250,00". */
export function formatarMoeda(valor) {
  if (valor === null || valor === undefined || valor === '') return '—';
  const numero = Number(valor);
  return Number.isFinite(numero) ? moedaBR.format(numero) : '—';
}

/** "2026-11-20" -> "20/11/2026" sem interpretar a data em UTC. */
export function formatarData(dataISO) {
  if (!dataISO) return '—';
  const partes = /^([0-9]{4})-([0-9]{2})-([0-9]{2})$/.exec(dataISO);
  if (!partes) return '—';
  return `${partes[3]}/${partes[2]}/${partes[1]}`;
}

/** "2026-11" -> "nov. de 2026" (útil nos gráficos). */
export function formatarMes(mesISO) {
  const partes = /^([0-9]{4})-([0-9]{2})$/.exec(mesISO || '');
  if (!partes) return '—';
  const ano = Number(partes[1]);
  const mes = Number(partes[2]);
  if (mes < 1 || mes > 12) return '—';
  return new Intl.DateTimeFormat('pt-BR', { month: 'short', year: 'numeric' })
    .format(new Date(ano, mes - 1, 1, 12));
}

/** Data local no formato aceito pelo Spring e por <input type="date">. */
export function dataAtualISO() {
  const hoje = new Date();
  const ano = hoje.getFullYear();
  const mes = String(hoje.getMonth() + 1).padStart(2, '0');
  const dia = String(hoje.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

export function inicioMesISO() {
  return `${dataAtualISO().slice(0, 7)}-01`;
}

export function fimMesISO() {
  const hoje = new Date();
  const ultimoDia = new Date(hoje.getFullYear(), hoje.getMonth() + 1, 0).getDate();
  return `${dataAtualISO().slice(0, 7)}-${String(ultimoDia).padStart(2, '0')}`;
}

/** Rótulo de situação; "VENCIDO" é calculado, não persistido no banco. */
export function situacaoExibicao(lancamento, referencia = dataAtualISO()) {
  if (lancamento.situacaoLancamento === 'LIQUIDADO') return 'Liquidado';
  if (lancamento.situacaoLancamento === 'PENDENTE' &&
      lancamento.dataVencimento && lancamento.dataVencimento < referencia) {
    return 'Vencido';
  }
  return 'Pendente';
}

/** Remove caracteres capazes de interferir ao inserir texto da API em innerHTML. */
export function escaparHtml(valor) {
  const mapa = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  return String(valor ?? '').replace(/[&<>"']/g, caractere => mapa[caractere]);
}

/** Para erros ProblemDetail com { erros: { nome: ["..."] } }. */
export function obterErrosPorCampo(erro) {
  const campos = erro?.erros;
  return campos && typeof campos === 'object' && !Array.isArray(campos)
    ? campos : {};
}

/** Une as mensagens devolvidas pelo back-end em um texto legível. */
export function mensagemDeErro(erro) {
  const campos = Object.entries(obterErrosPorCampo(erro));
  if (campos.length > 0) {
    return campos.flatMap(([campo, mensagens]) => {
      const itens = Array.isArray(mensagens) ? mensagens : [mensagens];
      return itens.map(mensagem => `${campo}: ${mensagem}`);
    }).join('\n');
  }
  return erro?.message || 'Não foi possível concluir a operação.';
}

/** Insere uma mensagem em um container já presente na view, sem HTML não confiável. */
export function exibirMensagem(elemento, mensagem, tipo = 'erro') {
  if (!elemento) return;
  elemento.hidden = false;
  elemento.classList.add('notice'); // classe do style.css já existente
  elemento.dataset.tipo = tipo;
  elemento.setAttribute('role', tipo === 'erro' ? 'alert' : 'status');
  elemento.textContent = mensagem;
}

export function limparMensagem(elemento) {
  if (!elemento) return;
  elemento.classList.remove('notice');
  elemento.hidden = true;
  elemento.textContent = '';
  elemento.removeAttribute('data-tipo');
}
