package br.com.ddametalurgica.estoque.exception;

/**
 * O pedido esta bem formado, mas a regra do estoque nao deixa executar:
 * saldo insuficiente, pastilha inativa (HTTP 422).
 */
public class OperacaoNaoPermitidaException extends RuntimeException {

    public OperacaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
