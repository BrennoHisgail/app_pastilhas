/* Tela que lista as pastilhas cadastradas */

(function () {

    const busca = document.getElementById('busca');
    const categoria = document.getElementById('categoria');
    const abaixoDoMinimo = document.getElementById('abaixoDoMinimo');
    const inativas = document.getElementById('inativas');

    const corpoTabela = document.getElementById('corpoTabela');
    const vazio = document.getElementById('vazio');
    const erro = document.getElementById('erro');
    const resumo = document.getElementById('resumo');
    const rota = document.getElementById('rota');

    const anterior = document.getElementById('anterior');
    const proxima = document.getElementById('proxima');
    const infoPagina = document.getElementById('infoPagina');

    let pagina = 0;

    // So o ADMIN edita, inativa e reativa. O OPERADOR so consulta.
    const admin = Ui.admin();

    function montarConsulta() {
        const p = new URLSearchParams();
        if (busca.value.trim() !== '') {
            p.set('busca', busca.value.trim());
        }
        if (categoria.value !== '') {
            p.set('categoria', categoria.value);
        }
        if (abaixoDoMinimo.checked) {
            p.set('abaixoDoMinimo', 'true');
        }
        if (inativas.checked) {
            p.set('ativo', 'false');
        }
        p.set('page', String(pagina));
        return p;
    }

    function linha(p) {
        const tr = document.createElement('tr');
        const selo = p.abaixoDoMinimo
            ? '<span class="selo selo--alerta">Repor</span>'
            : '<span class="selo selo--ok">Normal</span>';
        const classeSaldo = p.abaixoDoMinimo ? 'num texto-alerta' : 'num';

        const acao = p.ativo
            ? '<button type="button" class="botao-texto botao-texto--perigo" data-inativar="' + p.id + '">Inativar</button>'
            : '<button type="button" class="botao-texto" data-reativar="' + p.id + '">Reativar</button>';

        tr.innerHTML =
            '<td class="codigo">' + Ui.texto(p.codigo) + '</td>' +
            '<td>' + Ui.texto(p.descricao) + '</td>' +
            '<td style="color: var(--muted);">' + Ui.texto(p.categoria) + '</td>' +
            '<td style="color: var(--muted); font-size: 12px;">' + Ui.texto(p.unidadeMedida) + '</td>' +
            '<td class="' + classeSaldo + '">' + Fmt.quantidade(p.saldoAtual) + '</td>' +
            '<td class="num" style="color: var(--muted);">' + Fmt.quantidade(p.nivelMinimo) + '</td>' +
            '<td class="num">' + Fmt.moeda(p.valorEmEstoque) + '</td>' +
            '<td>' + selo + '</td>' +
            (admin
                ? '<td class="acoes">' +
                  '  <a class="botao-texto" href="/pastilhas/' + p.id + '/editar">Editar</a>' + acao +
                  '</td>'
                : '');
        return tr;
    }

    async function carregar() {
        Ui.esconder(erro);
        Carregando.linhasTabela(corpoTabela, 6, admin ? 9 : 8);
        const consulta = montarConsulta();
        rota.textContent = 'GET /api/pastilhas?' + consulta.toString();

        try {
            const pg = await Api.get('/api/pastilhas?' + consulta.toString());
            const itens = pg.content || [];

            corpoTabela.replaceChildren();
            itens.forEach((p) => corpoTabela.appendChild(linha(p)));
            Ui.alternar(vazio, itens.length === 0);

            resumo.textContent = Fmt.inteiro(pg.totalElements) +
                (pg.totalElements === 1 ? ' item encontrado' : ' itens encontrados');
            infoPagina.textContent = 'Página ' + (pg.number + 1) + ' de ' + Math.max(1, pg.totalPages);
            anterior.disabled = pg.first;
            proxima.disabled = pg.last;

        } catch (problema) {
            corpoTabela.replaceChildren();
            Ui.esconder(vazio);
            Ui.erro(erro, problema.mensagem || 'Falha ao consultar o catálogo.');
        }
    }

    async function carregarCategorias() {
        try {
            const lista = await Api.get('/api/pastilhas/categorias');
            lista.forEach((c) => {
                const opcao = document.createElement('option');
                opcao.value = c;
                opcao.textContent = c;
                categoria.appendChild(opcao);
            });
        } catch (problema) {
            // Se o filtro de categoria falhar a lista continua funcionando
        }
    }

    corpoTabela.addEventListener('click', async (evento) => {
        const paraInativar = evento.target.getAttribute('data-inativar');
        const paraReativar = evento.target.getAttribute('data-reativar');

        try {
            if (paraInativar) {
                const confirmado = await Confirmacao.perguntar(
                    'Inativar pastilha',
                    'A pastilha deixa de aparecer no catálogo e não aceita novas movimentações. '
                    + 'O histórico de lançamentos é preservado e a inativação pode ser desfeita.',
                    'Inativar');
                if (!confirmado) {
                    return;
                }
                await Api.remove('/api/pastilhas/' + paraInativar);
                Toast.ok('Pastilha inativada.');
                await carregar();
            } else if (paraReativar) {
                await Api.patch('/api/pastilhas/' + paraReativar + '/reativar');
                Toast.ok('Pastilha reativada.');
                await carregar();
            }
        } catch (problema) {
            Toast.erro(problema.mensagem || 'Não foi possível concluir a ação.');
        }
    });

    const recarregarDoInicio = () => {
        pagina = 0;
        carregar();
    };

    busca.addEventListener('input', Ui.debounce(recarregarDoInicio, 300));
    categoria.addEventListener('change', recarregarDoInicio);
    abaixoDoMinimo.addEventListener('change', recarregarDoInicio);
    inativas.addEventListener('change', recarregarDoInicio);

    anterior.addEventListener('click', () => {
        if (pagina > 0) {
            pagina -= 1;
            carregar();
        }
    });

    proxima.addEventListener('click', () => {
        pagina += 1;
        carregar();
    });

    // Quando o usuario clica no alerta do painel ele cai aqui
    // ja com o filtro de reposicao marcado
    const entrada = new URLSearchParams(window.location.search);
    if (entrada.get('abaixoDoMinimo') === 'true') {
        abaixoDoMinimo.checked = true;
    }

    carregarCategorias();
    carregar();
})();
