package br.com.ddametalurgica.estoque.exception;

// Dados inconsistentes entre si, por exemplo periodo invertido (HTTP 400)
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
