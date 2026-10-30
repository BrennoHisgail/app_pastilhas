/* Formulario de cadastro e edicao de pastilha */

(function () {

    const form = document.getElementById('formPastilha');
    const erro = document.getElementById('erro');
    const sucesso = document.getElementById('sucesso');
    const salvar = document.getElementById('salvar');
    const titulo = document.getElementById('titulo');
    const rota = document.getElementById('rota');

    const id = window.PASTILHA_ID ? String(window.PASTILHA_ID) : '';
    const edicao = id !== '';

    const CAMPOS = ['codigo', 'descricao', 'categoria', 'unidadeMedida',
        'nivelMinimo', 'custoUnitario', 'leadTimeDias', 'estoqueSeguranca'];

    function limparErros() {
        Ui.esconder(erro);
        Ui.esconder(sucesso);
        document.querySelectorAll('[data-erro]').forEach((span) => {
            span.textContent = '';
            Ui.esconder(span);
        });
        CAMPOS.forEach((campo) => {
            const elemento = document.getElementById(campo);
            if (elemento) {
                elemento.removeAttribute('aria-invalid');
            }
        });
    }

    // Pega os erros de validacao que o backend mandou
    // e mostra a mensagem embaixo de cada campo
    function marcarErrosDeCampo(erros) {
        Object.keys(erros || {}).forEach((campo) => {
            const span = document.querySelector('[data-erro="' + campo + '"]');
            const elemento = document.getElementById(campo);
            if (span) {
                span.textContent = erros[campo];
                Ui.mostrar(span);
            }
            if (elemento) {
                elemento.setAttribute('aria-invalid', 'true');
            }
        });
    }

    function lerFormulario() {
        return {
            codigo: document.getElementById('codigo').value.trim(),
            descricao: document.getElementById('descricao').value.trim(),
            categoria: document.getElementById('categoria').value.trim(),
            unidadeMedida: document.getElementById('unidadeMedida').value,
            nivelMinimo: Number(document.getElementById('nivelMinimo').value || 0),
            custoUnitario: Number(document.getElementById('custoUnitario').value || 0),
            leadTimeDias: Number(document.getElementById('leadTimeDias').value || 0),
            estoqueSeguranca: Number(document.getElementById('estoqueSeguranca').value || 0)
        };
    }

    function preencher(p) {
        document.getElementById('codigo').value = p.codigo;
        document.getElementById('descricao').value = p.descricao;
        document.getElementById('categoria').value = p.categoria;
        document.getElementById('unidadeMedida').value = p.unidadeMedida;
        document.getElementById('nivelMinimo').value = p.nivelMinimo;
        document.getElementById('custoUnitario').value = p.custoUnitario;
        document.getElementById('leadTimeDias').value = p.leadTimeDias;
        document.getElementById('estoqueSeguranca').value = p.estoqueSeguranca;
    }

    async function carregarCategorias() {
        try {
            const lista = await Api.get('/api/pastilhas/categorias');
            const datalist = document.getElementById('categorias');
            lista.forEach((c) => {
                const opcao = document.createElement('option');
                opcao.value = c;
                datalist.appendChild(opcao);
            });
        } catch (problema) {
            // Se as categorias nao carregarem o formulario continua funcionando
        }
    }

    async function carregarPastilha() {
        try {
            const p = await Api.get('/api/pastilhas/' + id);
            preencher(p);
            titulo.textContent = 'Editar ' + p.codigo;
        } catch (problema) {
            Ui.erro(erro, problema.mensagem || 'Pastilha não encontrada.');
            salvar.disabled = true;
        }
    }

    form.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        limparErros();
        Carregando.botao(salvar, true);

        try {
            const dados = lerFormulario();
            const salva = edicao
                ? await Api.put('/api/pastilhas/' + id, dados)
                : await Api.post('/api/pastilhas', dados);

            Carregando.botao(salvar, false);
            sucesso.textContent = 'Pastilha ' + salva.codigo + ' salva. Redirecionando para o catálogo…';
            Ui.mostrar(sucesso);
            Toast.ok('Pastilha ' + salva.codigo + ' salva.');
            setTimeout(() => {
                window.location.href = '/pastilhas';
            }, 900);

        } catch (problema) {
            marcarErrosDeCampo(problema.erros);
            Ui.erro(erro, problema.mensagem || 'Não foi possível salvar a pastilha.');
            Carregando.botao(salvar, false);
            const primeiro = document.querySelector('[aria-invalid="true"]');
            if (primeiro) {
                primeiro.focus();
            }
        }
    });

    if (edicao) {
        rota.textContent = 'PUT /api/pastilhas/' + id;
        carregarPastilha();
    }

    carregarCategorias();
})();
