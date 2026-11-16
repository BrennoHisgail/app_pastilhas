package br.com.ddametalurgica.estoque.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Concentra o tratamento de erro da API num lugar so.
// Quando um service lanca uma excecao, o Spring cai aqui e
// devolve a mensagem para a tela com o status HTTP certo.
//
// Estende o ResponseEntityExceptionHandler do Spring, que ja converte os
// erros do proprio framework (metodo nao suportado, Content-Type errado,
// parametro obrigatorio faltando...) para o mesmo formato ProblemDetail.
//
// So vale para os controllers de /api. As telas do pacote controller.web
// continuam caindo nas paginas de erro HTML (templates/error), senao o
// navegador mostraria JSON no lugar da pagina.
@RestControllerAdvice(basePackages = "br.com.ddametalurgica.estoque.controller.api")
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // 404: o id pedido nao existe
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage());
    }

    // 409: conflito com o estado atual (codigo duplicado, ultimo admin...)
    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail regraNegocio(RegraNegocioException ex) {
        return problema(HttpStatus.CONFLICT, "Regra de negocio violada", ex.getMessage());
    }

    // 422: pedido valido, mas o estoque nao permite (saldo insuficiente, pastilha inativa)
    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    public ProblemDetail operacaoNaoPermitida(OperacaoNaoPermitidaException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Operacao nao permitida", ex.getMessage());
    }

    // 400: dados que nao fazem sentido juntos (periodo invertido)
    @ExceptionHandler(DadosInvalidosException.class)
    public ProblemDetail dadosInvalidos(DadosInvalidosException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Dados invalidos", ex.getMessage());
    }

    // Erros das anotacoes de validacao (@NotNull, @Positive...)
    // aplicadas no corpo da requisicao (@RequestBody @Valid)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError campo : ex.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(campo.getField(),
                    campo.getDefaultMessage() == null ? "valor invalido" : campo.getDefaultMessage());
        }
        ProblemDetail detalhe = problema(HttpStatus.BAD_REQUEST, "Dados invalidos", "Verifique os campos informados.");
        detalhe.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(detalhe);
    }

    // JSON malformado ou enum inexistente no corpo da requisicao
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "Dados invalidos",
                "Nao foi possivel ler o corpo da requisicao. Confira o formato dos campos enviados."));
    }

    // Validacao dos parametros de URL (@RequestParam @Min...)
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail validacaoDeParametro(ConstraintViolationException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (ConstraintViolation<?> violacao : ex.getConstraintViolations()) {
            String caminho = violacao.getPropertyPath().toString();
            // O caminho vem como "metodo.parametro"; para a tela so interessa o parametro
            String parametro = caminho.substring(caminho.lastIndexOf('.') + 1);
            erros.putIfAbsent(parametro, violacao.getMessage());
        }
        ProblemDetail detalhe = problema(HttpStatus.BAD_REQUEST, "Dados invalidos", "Verifique os parametros informados.");
        detalhe.setProperty("erros", erros);
        return detalhe;
    }

    // Parametro com o tipo errado, por exemplo /api/pastilhas/abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Dados invalidos",
                "O valor informado em '" + ex.getName() + "' nao e valido.");
    }

    // Ordenacao pedindo um campo que a entidade nao tem, por exemplo ?sort=inexistente
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail ordenacaoInvalida(PropertyReferenceException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Dados invalidos",
                "Nao existe o campo '" + ex.getPropertyName() + "' para ordenar ou filtrar.");
    }

    // Ultima rede de protecao: as travas do banco (codigo duplicado, saldo negativo).
    // O service ja barra esses casos antes, mas duas requisicoes simultaneas
    // podem passar pela verificacao e so o banco pegar.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridade(DataIntegrityViolationException ex) {
        return problema(HttpStatus.CONFLICT, "Operacao rejeitada pelo banco",
                "A operacao nao pode ser concluida porque violaria uma regra do banco de dados. "
                        + "Recarregue a tela e tente de novo.");
    }

    // Outra transacao alterou a mesma pastilha ao mesmo tempo (ver campo versao em Pastilha)
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail conflitoDeVersao(ObjectOptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "Registro desatualizado",
                "Esta pastilha foi alterada por outra operacao ao mesmo tempo. "
                        + "Recarregue a tela e tente novamente.");
    }

    // Qualquer erro inesperado: registra no log com o stack trace e devolve
    // uma mensagem generica, sem expor detalhes internos para o cliente
    @ExceptionHandler(Exception.class)
    public ProblemDetail erroInesperado(Exception ex) {
        LOG.error("Erro inesperado na API", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente; se continuar, avise o administrador.");
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String mensagem) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(status, mensagem);
        detalhe.setTitle(titulo);
        return detalhe;
    }
}
