package br.com.ddametalurgica.estoque.exception;

// Conflito com o estado atual dos dados: codigo ou e-mail duplicado, ultimo admin (HTTP 409)
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
