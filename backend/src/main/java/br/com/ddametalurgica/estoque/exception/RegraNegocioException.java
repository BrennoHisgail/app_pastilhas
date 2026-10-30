package br.com.ddametalurgica.estoque.exception;

/** Violacao de regra de negocio: codigo duplicado, saldo insuficiente, etc. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
