package br.com.ddametalurgica.estoque.exception;

// Requisicao valida, mas recusada pela regra de estoque:
// saldo insuficiente ou pastilha inativa (HTTP 422)
public class OperacaoNaoPermitidaException extends RuntimeException {

    public OperacaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
