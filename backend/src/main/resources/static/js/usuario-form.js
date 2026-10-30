/* Formulario de cadastro e edicao de usuario */

(function () {

    const form = document.getElementById('formUsuario');
    const formSenha = document.getElementById('formSenha');
    const erro = document.getElementById('erro');
    const sucesso = document.getElementById('sucesso');
    const salvar = document.getElementById('salvar');
    const salvarSenha = document.getElementById('salvarSenha');
    const titulo = document.getElementById('titulo');
    const rota = document.getElementById('rota');

    const id = window.USUARIO_ID ? String(window.USUARIO_ID) : '';
    const edicao = id !== '';

    function limparErros() {
        Ui.esconder(erro);
        Ui.esconder(sucesso);
        document.querySelectorAll('[data-erro]').forEach((span) => {
            span.textContent = '';
            Ui.esconder(span);
        });
        document.querySelectorAll('[aria-invalid]').forEach((campo) => campo.removeAttribute('aria-invalid'));
    }

    // Mostra embaixo de cada campo o erro de validacao que veio do backend
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

    // Na edicao o email e a senha nao vao junto: o email nao muda
    // e a senha tem o formulario proprio logo abaixo
    function lerFormulario() {
        const dados = {
            nome: document.getElementById('nome').value.trim(),
            perfil: document.getElementById('perfil').value
        };
        if (!edicao) {
            dados.email = document.getElementById('email').value.trim();
            dados.senha = document.getElementById('senha').value;
        }
        return dados;
    }

    async function carregarUsuario() {
        try {
            const u = await Api.get('/api/usuarios/' + id);
            document.getElementById('nome').value = u.nome;
            document.getElementById('email').value = u.email;
            document.getElementById('perfil').value = u.perfil;
            titulo.textContent = 'Editar ' + u.nome;
        } catch (problema) {
            Ui.erro(erro, problema.mensagem || 'Usuário não encontrado.');
            salvar.disabled = true;
        }
    }

    form.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        limparErros();
        Carregando.botao(salvar, true);

        try {
            const dados = lerFormulario();
            const salvo = edicao
                ? await Api.put('/api/usuarios/' + id, dados)
                : await Api.post('/api/usuarios', dados);

            Carregando.botao(salvar, false);
            sucesso.textContent = 'Usuário ' + salvo.nome + ' salvo. Redirecionando…';
            Ui.mostrar(sucesso);
            Toast.ok('Usuário ' + salvo.nome + ' salvo.');
            setTimeout(() => {
                window.location.href = '/usuarios';
            }, 900);

        } catch (problema) {
            marcarErrosDeCampo(problema.erros);
            Ui.erro(erro, problema.mensagem || 'Não foi possível salvar o usuário.');
            Carregando.botao(salvar, false);
            const primeiro = document.querySelector('[aria-invalid="true"]');
            if (primeiro) {
                primeiro.focus();
            }
        }
    });

    formSenha.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        limparErros();
        Carregando.botao(salvarSenha, true);

        try {
            const campo = document.getElementById('novaSenha');
            await Api.patch('/api/usuarios/' + id + '/senha', { novaSenha: campo.value });
            campo.value = '';
            Toast.ok('Senha alterada.');
        } catch (problema) {
            marcarErrosDeCampo(problema.erros);
            Toast.erro(problema.mensagem || 'Não foi possível trocar a senha.');
        }
        Carregando.botao(salvarSenha, false);
    });

    if (edicao) {
        rota.textContent = 'PUT /api/usuarios/' + id;
        document.getElementById('rotaSenha').textContent = 'PATCH /api/usuarios/' + id + '/senha';
        document.getElementById('email').readOnly = true;
        document.getElementById('ajudaEmail').textContent = 'O e-mail não pode ser alterado';
        Ui.esconder(document.getElementById('campoSenha'));
        Ui.mostrar(formSenha);
        carregarUsuario();
    }
})();
