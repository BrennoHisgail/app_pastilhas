/* Tela de indicadores: PP, Giro, Cobertura e Curva ABC */

(function () {

    const dias = document.getElementById('dias');
    const corpoTabela = document.getElementById('corpoTabela');
    const vazio = document.getElementById('vazio');
    const erro = document.getElementById('erro');
    const rota = document.getElementById('rota');

    function selo(classe) {
        if (classe === 'A') {
            return '<span class="selo selo--alerta">A</span>';
        }
        if (classe === 'B') {
            return '<span class="selo selo--neutro">B</span>';
        }
        return '<span class="selo selo--ok">C</span>';
    }

    function linha(i) {
        const tr = document.createElement('tr');
        const classeSaldo = i.abaixoDoPontoDePedido ? 'num texto-alerta' : 'num';
        const cobertura = i.coberturaDias === null ? 'sem consumo' : Fmt.quantidade(i.coberturaDias) + ' dias';
        const giro = i.giro === null ? '–' : Fmt.quantidade(i.giro);

        tr.innerHTML =
            '<td>' + selo(i.classeAbc) + '</td>' +
            '<td><span class="codigo">' + Ui.texto(i.codigo) + '</span>' +
            '<div style="font-size: 12px; color: var(--muted);">' + Ui.texto(i.descricao) + '</div></td>' +
            '<td class="num">' + Fmt.quantidade(i.consumoPeriodo) + '</td>' +
            '<td class="num" style="color: var(--muted);">' + Fmt.quantidade(i.consumoMedioDiario) + '</td>' +
            '<td class="num">' + Fmt.quantidade(i.pontoDePedido) + '</td>' +
            '<td class="' + classeSaldo + '">' + Fmt.quantidade(i.saldoAtual) + '</td>' +
            '<td class="num" style="color: var(--muted);">' + cobertura + '</td>' +
            '<td class="num">' + giro + '</td>' +
            '<td class="num" style="color: var(--muted);">' + Fmt.quantidade(i.percentualAcumulado) + '%</td>';
        return tr;
    }

    async function carregar() {
        Ui.esconder(erro);
        Carregando.linhasTabela(corpoTabela, 6, 9);
        ['kpiA', 'kpiB', 'kpiC', 'kpiPp'].forEach((id) => Carregando.valor(document.getElementById(id)));

        const url = '/api/indicadores/estoque?dias=' + dias.value;
        rota.textContent = 'GET ' + url;

        try {
            const r = await Api.get(url);

            document.getElementById('kpiA').textContent = Fmt.inteiro(r.classeA);
            document.getElementById('kpiB').textContent = Fmt.inteiro(r.classeB);
            document.getElementById('kpiC').textContent = Fmt.inteiro(r.classeC);
            document.getElementById('kpiPp').textContent = Fmt.inteiro(r.abaixoDoPontoDePedido);

            corpoTabela.replaceChildren();
            r.itens.forEach((i) => corpoTabela.appendChild(linha(i)));
            Ui.alternar(vazio, r.itens.length === 0);

        } catch (problema) {
            corpoTabela.replaceChildren();
            Ui.erro(erro, problema.mensagem || 'Falha ao calcular os indicadores.');
        }
    }

    dias.addEventListener('change', carregar);
    carregar();
})();
