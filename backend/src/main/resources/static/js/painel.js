/* Tela do painel inicial. Busca os numeros em /api/dashboard/indicadores */

(function () {

    const corpoAlertas = document.getElementById('corpoAlertas');
    const alertasVazio = document.getElementById('alertasVazio');
    const listaRecentes = document.getElementById('listaRecentes');
    const recentesVazio = document.getElementById('recentesVazio');

    function linhaAlerta(p) {
        const minimo = Number(p.nivelMinimo) || 0;
        const saldo = Number(p.saldoAtual) || 0;
        const proporcao = minimo > 0 ? Math.min(1, saldo / minimo) : 0;
        const largura = Math.round(proporcao * 120);
        const repor = Math.max(0, minimo - saldo);

        const tr = document.createElement('tr');
        tr.innerHTML =
            '<td class="codigo">' + Ui.texto(p.codigo) + '</td>' +
            '<td style="color: var(--muted);">' + Ui.texto(p.categoria) + '</td>' +
            '<td><span class="barra"><span class="barra__preenchimento" style="width: ' + largura + 'px;"></span></span></td>' +
            '<td class="num texto-alerta">' + Fmt.quantidade(saldo) + '</td>' +
            '<td class="num" style="color: var(--muted);">' + Fmt.quantidade(minimo) + '</td>' +
            '<td class="num"><strong>' + Fmt.quantidade(repor) + '</strong></td>';
        return tr;
    }

    function itemRecente(m) {
        const entrada = m.tipo === 'ENTRADA';
        const cor = entrada ? 'var(--ok)' : 'var(--alert)';
        const sinal = entrada ? '+' : '−';

        const div = document.createElement('div');
        div.style.cssText = 'padding: 11px 20px; display: flex; align-items: center; gap: 12px; border-bottom: 1px solid var(--line-soft);';
        div.innerHTML =
            '<span style="width: 7px; height: 28px; border-radius: 2px; flex-shrink: 0; background: ' + cor + ';"></span>' +
            '<span style="display: flex; flex-direction: column; flex-grow: 1; min-width: 0;">' +
            '  <span class="codigo">' + Ui.texto(m.pastilhaCodigo) + '</span>' +
            '  <span style="font-size: 11.5px; color: var(--muted);">' +
            (entrada ? 'Entrada' : 'Saída') + ' · ' + Fmt.dataHora(m.dataHora) +
            '  </span>' +
            '</span>' +
            '<span style="text-align: right;">' +
            '  <span style="display: block; font-family: var(--mono); font-size: 13px; font-weight: 600; color: ' + cor + ';">' +
            sinal + Fmt.quantidade(m.quantidade) + '</span>' +
            '  <span style="display: block; font-size: 11px; color: var(--muted);">saldo ' +
            Fmt.quantidade(m.saldoApos) + '</span>' +
            '</span>';
        return div;
    }

    function mostrarEsqueletos() {
        ['kpiItens', 'kpiFalta', 'kpiValor', 'kpiMovs']
            .forEach((id) => Carregando.valor(document.getElementById(id)));
        Carregando.linhasTabela(corpoAlertas, 4, 6);
    }

    async function carregar() {
        mostrarEsqueletos();
        try {
            const d = await Api.get('/api/dashboard/indicadores');

            document.getElementById('kpiItens').textContent = Fmt.inteiro(d.itensCadastrados);
            document.getElementById('kpiCategorias').textContent =
                'em ' + Fmt.inteiro(d.categorias) + (d.categorias === 1 ? ' categoria' : ' categorias');
            document.getElementById('kpiFalta').textContent = Fmt.inteiro(d.itensAbaixoDoMinimo);
            document.getElementById('kpiValor').textContent = Fmt.moeda(d.valorEmEstoque);
            document.getElementById('kpiMovs').textContent = Fmt.inteiro(d.movimentacoesUltimos30Dias);
            document.getElementById('totalAlertas').textContent =
                Fmt.inteiro(d.itensAbaixoDoMinimo) + (d.itensAbaixoDoMinimo === 1 ? ' item' : ' itens');

            const alertas = d.alertasReposicao || [];
            corpoAlertas.replaceChildren();
            alertas.forEach((p) => corpoAlertas.appendChild(linhaAlerta(p)));
            Ui.alternar(alertasVazio, alertas.length === 0);

            const recentes = d.movimentacoesRecentes || [];
            listaRecentes.replaceChildren();
            recentes.forEach((m) => listaRecentes.appendChild(itemRecente(m)));
            Ui.alternar(recentesVazio, recentes.length === 0);

        } catch (problema) {
            ['kpiItens', 'kpiFalta', 'kpiValor', 'kpiMovs']
                .forEach((id) => { document.getElementById(id).textContent = '--'; });
            corpoAlertas.replaceChildren();
            document.getElementById('kpiCategorias').textContent = 'indisponivel';
            Toast.erro(problema.mensagem || 'Falha ao carregar os indicadores.');
        }
    }

    carregar();
})();
