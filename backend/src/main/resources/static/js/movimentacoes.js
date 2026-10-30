/* Tela de entradas, saidas e historico do estoque */

(function () {

    const form = document.getElementById('formMovimentacao');
    const seletor = document.getElementById('pastilhaId');
    const quantidade = document.getElementById('quantidade');
    const erro = document.getElementById('erro');
    const sucesso = document.getElementById('sucesso');
    const registrar = document.getElementById('registrar');
    const limpar = document.getElementById('limpar');
    const rota = document.getElementById('rota');

    const corpoHistorico = document.getElementById('corpoHistorico');
    const historicoVazio = document.getElementById('historicoVazio');
    const totalMovs = document.getElementById('totalMovs');

    let pastilhas = [];

    function tipoSelecionado() {
        const marcado = document.querySelector('input[name="tipo"]:checked');
        return marcado ? marcado.value : 'entrada';
    }

    function pastilhaSelecionada() {
        const id = seletor.value;
        return pastilhas.filter((p) => String(p.id) === String(id))[0] || null;
    }

    // Mostra no cartao da lateral o saldo da pastilha escolhida
    function atualizarCartaoSaldo() {
        const p = pastilhaSelecionada();
        const situacao = document.getElementById('saldoSituacao');
        const valor = document.getElementById('saldoValor');

        if (!p) {
            document.getElementById('saldoCodigo').textContent = '–';
            document.getElementById('saldoDescricao').textContent = 'Selecione uma pastilha';
            valor.textContent = '–';
            valor.style.color = 'var(--ink)';
            document.getElementById('saldoUnidade').textContent = 'em estoque';
            document.getElementById('saldoMinimo').textContent = 'Nível mínimo: –';
            situacao.textContent = '–';
            situacao.className = 'selo selo--neutro';
            return;
        }

        document.getElementById('saldoCodigo').textContent = p.codigo;
        document.getElementById('saldoDescricao').textContent = p.descricao;
        valor.textContent = Fmt.quantidade(p.saldoAtual);
        valor.style.color = p.abaixoDoMinimo ? 'var(--alert)' : 'var(--ok)';
        document.getElementById('saldoUnidade').textContent = p.unidadeMedida + ' em estoque';
        document.getElementById('saldoMinimo').textContent =
            'Nível mínimo: ' + Fmt.quantidade(p.nivelMinimo);
        situacao.textContent = p.abaixoDoMinimo ? 'Repor' : 'Normal';
        situacao.className = p.abaixoDoMinimo ? 'selo selo--alerta' : 'selo selo--ok';
    }

    function linhaHistorico(m) {
        const entrada = m.tipo === 'ENTRADA';
        const cor = entrada ? 'var(--ok)' : 'var(--alert)';
        const sinal = entrada ? '+' : '−';

        const tr = document.createElement('tr');
        tr.innerHTML =
            '<td style="color: var(--muted); font-size: 12.5px;">' + Fmt.dataHora(m.dataHora) + '</td>' +
            '<td class="codigo">' + Ui.texto(m.pastilhaCodigo) + '</td>' +
            '<td><span class="selo" style="color: ' + cor + '; background: ' +
            (entrada ? 'var(--ok-bg)' : 'var(--alert-bg)') + ';">' +
            (entrada ? 'Entrada' : 'Saída') + '</span></td>' +
            '<td class="num" style="color: ' + cor + '; font-weight: 600;">' +
            sinal + Fmt.quantidade(m.quantidade) + '</td>' +
            '<td class="num">' + Fmt.quantidade(m.saldoApos) + '</td>' +
            '<td style="color: var(--muted); font-size: 12.5px;">' + Ui.texto(m.documento || '–') + '</td>';
        return tr;
    }

    async function carregarPastilhas() {
        try {
            // size grande para trazer todas as pastilhas de uma vez no combo
            const pg = await Api.get('/api/pastilhas?size=200&sort=codigo');
            pastilhas = pg.content || [];

            seletor.replaceChildren();
            if (pastilhas.length === 0) {
                const opcao = document.createElement('option');
                opcao.value = '';
                opcao.textContent = 'Nenhuma pastilha cadastrada';
                seletor.appendChild(opcao);
                registrar.disabled = true;
            } else {
                pastilhas.forEach((p) => {
                    const opcao = document.createElement('option');
                    opcao.value = p.id;
                    opcao.textContent = p.codigo + ' – ' + p.descricao;
                    seletor.appendChild(opcao);
                });
            }
            atualizarCartaoSaldo();

        } catch (problema) {
            Ui.erro(erro, problema.mensagem || 'Falha ao carregar o catálogo.');
            registrar.disabled = true;
        }
    }

    async function carregarHistorico() {
        Carregando.linhasTabela(corpoHistorico, 3, 6);
        try {
            const pg = await Api.get('/api/movimentacoes?size=15');
            const itens = pg.content || [];

            corpoHistorico.replaceChildren();
            itens.forEach((m) => corpoHistorico.appendChild(linhaHistorico(m)));
            Ui.alternar(historicoVazio, itens.length === 0);
            totalMovs.textContent = Fmt.inteiro(pg.totalElements) +
                (pg.totalElements === 1 ? ' lançamento' : ' lançamentos');

        } catch (problema) {
            Ui.erro(erro, problema.mensagem || 'Falha ao carregar o histórico.');
        }
    }

    form.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        Ui.esconder(erro);
        Ui.esconder(sucesso);

        const p = pastilhaSelecionada();
        const qtd = Number(quantidade.value || 0);

        if (!p) {
            Ui.erro(erro, 'Selecione uma pastilha.');
            return;
        }
        if (!(qtd > 0)) {
            Ui.erro(erro, 'Informe uma quantidade maior que zero.');
            quantidade.focus();
            return;
        }

        Carregando.botao(registrar, true);
        const tipo = tipoSelecionado();

        try {
            const criada = await Api.post('/api/movimentacoes/' + tipo, {
                pastilhaId: p.id,
                quantidade: qtd,
                documento: document.getElementById('documento').value.trim() || null,
                origemDestino: document.getElementById('origemDestino').value.trim() || null,
                observacao: document.getElementById('observacao').value.trim() || null
            });

            sucesso.textContent = (tipo === 'entrada' ? 'Entrada' : 'Saída') + ' de ' +
                Fmt.quantidade(criada.quantidade) + ' ' + p.unidadeMedida + ' registrada. Saldo de ' +
                criada.pastilhaCodigo + ' atualizado para ' + Fmt.quantidade(criada.saldoApos) + '.';
            Ui.mostrar(sucesso);
            Toast.ok(sucesso.textContent);

            quantidade.value = '';
            document.getElementById('documento').value = '';
            document.getElementById('origemDestino').value = '';
            document.getElementById('observacao').value = '';

            // Busca de novo para o cartao mostrar o saldo ja atualizado
            await carregarPastilhas();
            seletor.value = String(p.id);
            atualizarCartaoSaldo();
            await carregarHistorico();

        } catch (problema) {
            Ui.erro(erro, problema.mensagem || 'Não foi possível registrar o lançamento.');
            Toast.erro(problema.mensagem || 'Não foi possível registrar o lançamento.');
            quantidade.focus();
        } finally {
            Carregando.botao(registrar, false);
        }
    });

    limpar.addEventListener('click', () => {
        form.reset();
        Ui.esconder(erro);
        Ui.esconder(sucesso);
        atualizarCartaoSaldo();
        rota.textContent = 'POST /api/movimentacoes/entrada';
    });

    seletor.addEventListener('change', () => {
        Ui.esconder(erro);
        Ui.esconder(sucesso);
        atualizarCartaoSaldo();
    });

    document.querySelectorAll('input[name="tipo"]').forEach((radio) => {
        radio.addEventListener('change', () => {
            rota.textContent = 'POST /api/movimentacoes/' + tipoSelecionado();
            Ui.esconder(erro);
            Ui.esconder(sucesso);
        });
    });

    carregarPastilhas().then(carregarHistorico);
})();
