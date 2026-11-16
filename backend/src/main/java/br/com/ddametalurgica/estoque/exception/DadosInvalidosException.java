package br.com.ddametalurgica.estoque.exception;

/** O pedido veio com dados que nao fazem sentido juntos, por exemplo periodo invertido (HTTP 400). */
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
