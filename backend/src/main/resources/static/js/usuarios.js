/* Tela que lista os usuarios do sistema (so ADMIN abre) */

(function () {

    const busca = document.getElementById('busca');
    const inativos = document.getElementById('inativos');

    const corpoTabela = document.getElementById('corpoTabela');
    const vazio = document.getElementById('vazio');
    const erro = document.getElementById('erro');
    const resumo = document.getElementById('resumo');
    const rota = document.getElementById('rota');

    const anterior = document.getElementById('anterior');
    const proxima = document.getElementById('proxima');
    const infoPagina = document.getElementById('infoPagina');

    let pagina = 0;

    function montarConsulta() {
        const p = new URLSearchParams();
        if (busca.value.trim() !== '') {
            p.set('busca', busca.value.trim());
        }
        if (inativos.checked) {
            p.set('ativo', 'false');
        }
        p.set('page', String(pagina));
        return p;
    }

    function linha(u) {
        const tr = document.createElement('tr');
        const perfil = u.perfil === 'ADMIN'
            ? '<span class="selo selo--neutro">Administrador</span>'
            : '<span class="selo selo--neutro">Operador</span>';
        const situacao = u.ativo
            ? '<span class="selo selo--ok">Ativo</span>'
            : '<span class="selo selo--alerta">Inativo</span>';
        const acao = u.ativo
            ? '<button type="button" class="botao-texto botao-texto--perigo" data-inativar="' + u.id + '">Inativar</button>'
            : '<button type="button" class="botao-texto" data-reativar="' + u.id + '">Reativar</button>';

        tr.innerHTML =
            '<td>' + Ui.texto(u.nome) + '</td>' +
            '<td style="color: var(--muted);">' + Ui.texto(u.email) + '</td>' +
            '<td>' + perfil + '</td>' +
            '<td>' + situacao + '</td>' +
            '<td class="acoes">' +
            '  <a class="botao-texto" href="/usuarios/' + u.id + '/editar">Editar</a>' + acao +
            '</td>';
        return tr;
    }

    async function carregar() {
        Ui.esconder(erro);
        Carregando.linhasTabela(corpoTabela, 4, 5);
        const consulta = montarConsulta();
        rota.textContent = 'GET /api/usuarios?' + consulta.toString();

        try {
            const pg = await Api.get('/api/usuarios?' + consulta.toString());
            const itens = pg.content || [];

            corpoTabela.replaceChildren();
            itens.forEach((u) => corpoTabela.appendChild(linha(u)));
            Ui.alternar(vazio, itens.length === 0);

            resumo.textContent = Fmt.inteiro(pg.totalElements) +
                (pg.totalElements === 1 ? ' usuário' : ' usuários');
            infoPagina.textContent = 'Página ' + (pg.number + 1) + ' de ' + Math.max(1, pg.totalPages);
            anterior.disabled = pg.first;
            proxima.disabled = pg.last;

        } catch (problema) {
            corpoTabela.replaceChildren();
            Ui.esconder(vazio);
            Ui.erro(erro, problema.mensagem || 'Falha ao consultar os usuários.');
        }
    }

    corpoTabela.addEventListener('click', async (evento) => {
        const paraInativar = evento.target.getAttribute('data-inativar');
        const paraReativar = evento.target.getAttribute('data-reativar');

        try {
            if (paraInativar) {
                const confirmado = await Confirmacao.perguntar(
                    'Inativar usuário',
                    'O usuário não consegue mais entrar no sistema. '
                    + 'Os lançamentos feitos por ele continuam no histórico e a inativação pode ser desfeita.',
                    'Inativar');
                if (!confirmado) {
                    return;
                }
                await Api.remove('/api/usuarios/' + paraInativar);
                Toast.ok('Usuário inativado.');
                await carregar();
            } else if (paraReativar) {
                await Api.patch('/api/usuarios/' + paraReativar + '/reativar');
                Toast.ok('Usuário reativado.');
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
    inativos.addEventListener('change', recarregarDoInicio);

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

    carregar();
})();
