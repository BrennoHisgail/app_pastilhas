/* Cliente HTTP compartilhado pelas telas: fetch com CSRF, tratamento de erro e sessao expirada */

const Api = (function () {

    // Extrai a mensagem do ProblemDetail devolvido pelo ApiExceptionHandler
    async function lerProblema(resposta) {
        let corpo = null;
        try {
            corpo = await resposta.json();
        } catch (e) {
            corpo = null;
        }

        if (!corpo) {
            return { mensagem: 'Falha na comunicação com o servidor (HTTP ' + resposta.status + ').', erros: {} };
        }

        return {
            mensagem: corpo.detail || corpo.title || 'Não foi possível completar a operação.',
            erros: corpo.erros || {},
            status: resposta.status
        };
    }

    // Token CSRF exigido pelo Spring Security em metodos que alteram dados.
    // Lido das <meta> _csrf e _csrf_header que o layout gera.
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

        // Sessao expirada: redireciona para o login
        if (resposta.status === 401) {
            window.location.href = '/login?expirado=true';
            throw { mensagem: 'Sua sessão expirou. Entre novamente.', erros: {}, status: 401 };
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

    // Remove zeros a direita: 4.000 -> 4
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

    // Perfil do usuario logado, lido da <meta> gerada pelo layout
    admin() {
        const perfil = document.querySelector('meta[name="perfil"]');
        return perfil !== null && perfil.content === 'ADMIN';
    },

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

    // Escapa HTML antes de inserir dados do banco na pagina (evita XSS)
    texto(valor) {
        const span = document.createElement('span');
        span.textContent = valor === null || valor === undefined ? '' : String(valor);
        return span.innerHTML;
    },

    // Debounce: so chama a API quando o usuario para de digitar
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

        // Avisos de erro ficam visiveis por mais tempo
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

/* Confirmacao com <dialog>; usa o confirm() nativo se o navegador nao suportar */
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

    // Desabilita o botao durante o envio, mantendo a largura original
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

    // Linhas de carregamento (skeleton) enquanto a API responde
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

/* Alterna a exibicao das rotas da API em cada tela; a escolha fica no localStorage */
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
                // localStorage indisponivel (ex.: aba anonima): mantem o padrao
            }
        });
    });
})();
