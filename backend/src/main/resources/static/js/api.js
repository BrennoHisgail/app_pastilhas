/* Arquivo com as funcoes que chamam a API.
   Todas as telas usam este arquivo para nao repetir fetch em cada pagina. */

const Api = (function () {

    // Pega a mensagem de erro que o backend mandou.
    // O campo 'detail' vem do nosso tratador de excecoes.
    async function lerProblema(resposta) {
        let corpo = null;
        try {
            corpo = await resposta.json();
        } catch (e) {
            corpo = null;
        }

        if (!corpo) {
            return { mensagem: 'Falha na comunicacao com o servidor (HTTP ' + resposta.status + ').', erros: {} };
        }

        return {
            mensagem: corpo.detail || corpo.title || 'Nao foi possivel completar a operacao.',
            erros: corpo.erros || {},
            status: resposta.status
        };
    }

    // O Spring Security exige o token CSRF em tudo que nao e GET.
    // O layout coloca o token em duas <meta> no cabecalho da pagina.
    function cabecalhoCsrf() {
        const token = document.querySelector('meta[name="_csrf"]');
        const nome = document.querySelector('meta[name="_csrf_header"]');
        if (!token || !nome) {
            return {};
        }
        return { [nome.content]: token.content };
    }

    async function requisitar(metodo, url, corpo) {
        const opcoes = { method: metodo, headers: { 'Accept': 'application/json' } };
        if (metodo !== 'GET') {
            Object.assign(opcoes.headers, cabecalhoCsrf());
        }
        if (corpo !== undefined && corpo !== null) {
            opcoes.headers['Content-Type'] = 'application/json';
            opcoes.body = JSON.stringify(corpo);
        }

        const resposta = await fetch(url, opcoes);

        // Sessao expirou: manda de volta para o login
        if (resposta.status === 401) {
            window.location.href = '/login?expirado=true';
            throw { mensagem: 'Sua sessao expirou. Entre novamente.', erros: {}, status: 401 };
        }

        if (!resposta.ok) {
            throw await lerProblema(resposta);
        }

        if (resposta.status === 204) {
            return null;
        }
        return resposta.json();
    }

    return {
        get: (url) => requisitar('GET', url),
        post: (url, corpo) => requisitar('POST', url, corpo),
        put: (url, corpo) => requisitar('PUT', url, corpo),
        patch: (url, corpo) => requisitar('PATCH', url, corpo),
        remove: (url) => requisitar('DELETE', url)
    };
})();

/* Funcoes de formatacao para mostrar os valores na tela */

const Fmt = {

    moeda(valor) {
        const numero = Number(valor || 0);
        return numero.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    },

    // Tira os zeros do final, senao 4 aparece como 4,000
    quantidade(valor) {
        const numero = Number(valor || 0);
        return numero.toLocaleString('pt-BR', { maximumFractionDigits: 3 });
    },

    inteiro(valor) {
        return Number(valor || 0).toLocaleString('pt-BR');
    },

    dataHora(iso) {
        if (!iso) {
            return '–';
        }
        const d = new Date(iso);
        return d.toLocaleString('pt-BR', {
            day: '2-digit', month: '2-digit', year: '2-digit',
            hour: '2-digit', minute: '2-digit'
        });
    }
};

/* Funcoes auxiliares usadas nas telas */

const Ui = {

    mostrar(elemento) {
        if (elemento) {
            elemento.classList.remove('oculto');
        }
    },

    esconder(elemento) {
        if (elemento) {
            elemento.classList.add('oculto');
        }
    },

    alternar(elemento, visivel) {
        if (visivel) {
            Ui.mostrar(elemento);
        } else {
            Ui.esconder(elemento);
        }
    },

    erro(elemento, mensagem) {
        if (!elemento) {
            return;
        }
        elemento.textContent = mensagem;
        Ui.mostrar(elemento);
    },

    // Escapa o texto antes de jogar na tabela, para o
    // navegador nao interpretar HTML que veio do banco
    texto(valor) {
        const span = document.createElement('span');
        span.textContent = valor === null || valor === undefined ? '' : String(valor);
        return span.innerHTML;
    },

    // Espera o usuario parar de digitar para so entao chamar a API,
    // senao faria uma requisicao a cada letra
    debounce(fn, espera) {
        let timer = null;
        return function () {
            const args = arguments;
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(null, args), espera);
        };
    }
};

/* Avisos, confirmacao e indicadores de carregamento usados pelas telas */

const Toast = {

    _icone(tipo) {
        const cor = tipo === 'erro' ? '#A63D17' : (tipo === 'ok' ? '#2E6B4F' : '#0F4C5C');
        const caminho = tipo === 'erro'
            ? '<circle cx="12" cy="12" r="9"></circle><path d="M12 8v5"></path><path d="M12 16h.01"></path>'
            : '<circle cx="12" cy="12" r="9"></circle><path d="m8.5 12 2.5 2.5 4.5-5"></path>';
        return '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="' + cor +
            '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" ' +
            'style="flex-shrink:0;margin-top:1px;">' + caminho + '</svg>';
    },

    mostrar(mensagem, tipo) {
        const area = document.getElementById('avisosFlutuantes');
        if (!area) {
            return;
        }

        const caixa = document.createElement('div');
        caixa.className = 'aviso-flutuante' + (tipo ? ' aviso-flutuante--' + tipo : '');
        caixa.setAttribute('role', tipo === 'erro' ? 'alert' : 'status');
        caixa.innerHTML = Toast._icone(tipo) + '<span>' + Ui.texto(mensagem) + '</span>';
        area.appendChild(caixa);

        // O aviso de erro fica mais tempo na tela para dar tempo de ler
        const duracao = tipo === 'erro' ? 7000 : 4000;
        setTimeout(() => {
            caixa.classList.add('aviso-flutuante--saindo');
            setTimeout(() => caixa.remove(), 240);
        }, duracao);
    },

    ok(mensagem) {
        Toast.mostrar(mensagem, 'ok');
    },

    erro(mensagem) {
        Toast.mostrar(mensagem, 'erro');
    }
};

/* Caixa de confirmacao usando a tag <dialog>.
   Se o navegador for antigo e nao tiver <dialog>, usa o confirm() normal. */
const Confirmacao = {

    perguntar(titulo, texto, rotuloAcao) {
        const dialogo = document.getElementById('dialogoConfirma');

        if (!dialogo || typeof dialogo.showModal !== 'function') {
            return Promise.resolve(window.confirm(texto));
        }

        document.getElementById('dialogoTitulo').textContent = titulo;
        document.getElementById('dialogoTexto').textContent = texto;
        document.getElementById('dialogoConfirmar').textContent = rotuloAcao || 'Confirmar';

        return new Promise((resolver) => {
            dialogo.addEventListener('close', function aoFechar() {
                dialogo.removeEventListener('close', aoFechar);
                resolver(dialogo.returnValue === 'confirmar');
            });
            dialogo.showModal();
        });
    }
};

const Carregando = {

    // Desabilita o botao enquanto salva.
    // Guardamos a largura para o botao nao mudar de tamanho.
    botao(elemento, ocupado) {
        if (!elemento) {
            return;
        }
        if (ocupado) {
            elemento.style.minWidth = elemento.offsetWidth + 'px';
            elemento.dataset.carregando = 'true';
            elemento.disabled = true;
        } else {
            delete elemento.dataset.carregando;
            elemento.disabled = false;
        }
    },

    // Mostra linhas cinza enquanto os dados nao chegam da API
    linhasTabela(corpo, linhas, colunas) {
        corpo.replaceChildren();
        for (let i = 0; i < linhas; i += 1) {
            const tr = document.createElement('tr');
            for (let c = 0; c < colunas; c += 1) {
                const td = document.createElement('td');
                td.innerHTML = '<span class="esqueleto esqueleto-linha"></span>';
                tr.appendChild(td);
            }
            corpo.appendChild(tr);
        }
    },

    valor(elemento) {
        if (elemento) {
            elemento.innerHTML = '<span class="esqueleto">000</span>';
        }
    }
};

/* Botao que mostra ou esconde o endereco da API em cada tela.
   Guardamos a escolha no navegador para nao perder ao trocar de pagina. */
(function prepararRotas() {
    const CHAVE = 'estoque:mostrarRotas';

    function aplicar(ligado) {
        document.body.classList.toggle('mostrar-rotas', ligado);
    }

    document.addEventListener('DOMContentLoaded', () => {
        const alternador = document.getElementById('alternarRotas');
        let ligado = false;
        try {
            ligado = window.localStorage.getItem(CHAVE) === 'true';
        } catch (e) {
            ligado = false;
        }

        aplicar(ligado);
        if (!alternador) {
            return;
        }
        alternador.checked = ligado;
        alternador.addEventListener('change', () => {
            aplicar(alternador.checked);
            try {
                window.localStorage.setItem(CHAVE, String(alternador.checked));
            } catch (e) {
                // Em aba anonima o localStorage da erro, ai so ignoramos
            }
        });
    });
})();
