package br.com.ddametalurgica.estoque.exception;

/** Conflito com o estado atual dos dados: codigo/email duplicado, ultimo admin, etc. (HTTP 409). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
