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

// Tratamento centralizado de erros da API: converte excecoes em respostas
// ProblemDetail (RFC 7807) com o status HTTP correspondente.
//
// Estende ResponseEntityExceptionHandler para cobrir tambem os erros do
// proprio Spring MVC (parametro ausente, corpo ilegivel etc.).
//
// Restrito ao pacote controller.api: as telas (controller.web) continuam
// usando as paginas de erro HTML de templates/error.
@RestControllerAdvice(basePackages = "br.com.ddametalurgica.estoque.controller.api")
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // 404: o id pedido nao existe
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    // 409: conflito com o estado atual (codigo duplicado, ultimo admin...)
    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail regraNegocio(RegraNegocioException ex) {
        return problema(HttpStatus.CONFLICT, "Regra de negócio violada", ex.getMessage());
    }

    // 422: pedido valido, mas o estoque nao permite (saldo insuficiente, pastilha inativa)
    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    public ProblemDetail operacaoNaoPermitida(OperacaoNaoPermitidaException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Operação não permitida", ex.getMessage());
    }

    // 400: dados que nao fazem sentido juntos (periodo invertido)
    @ExceptionHandler(DadosInvalidosException.class)
    public ProblemDetail dadosInvalidos(DadosInvalidosException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Dados inválidos", ex.getMessage());
    }

    // Bean Validation no corpo da requisicao (@RequestBody @Valid): devolve o erro de cada campo
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError campo : ex.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(campo.getField(),
                    campo.getDefaultMessage() == null ? "valor inválido" : campo.getDefaultMessage());
        }
        ProblemDetail detalhe = problema(HttpStatus.BAD_REQUEST, "Dados inválidos", "Verifique os campos informados.");
        detalhe.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(detalhe);
    }

    // JSON malformado ou enum inexistente no corpo da requisicao
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Não foi possível ler o corpo da requisição. Confira o formato dos campos enviados."));
    }

    // Bean Validation nos parametros de URL (@RequestParam @Min...)
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail validacaoDeParametro(ConstraintViolationException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (ConstraintViolation<?> violacao : ex.getConstraintViolations()) {
            String caminho = violacao.getPropertyPath().toString();
            // O caminho vem como "metodo.parametro"; mantem so o nome do parametro
            String parametro = caminho.substring(caminho.lastIndexOf('.') + 1);
            erros.putIfAbsent(parametro, violacao.getMessage());
        }
        ProblemDetail detalhe = problema(HttpStatus.BAD_REQUEST, "Dados inválidos", "Verifique os parâmetros informados.");
        detalhe.setProperty("erros", erros);
        return detalhe;
    }

    // Parametro com o tipo errado, por exemplo /api/pastilhas/abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "O valor informado em '" + ex.getName() + "' não é válido.");
    }

    // Ordenacao pedindo um campo que a entidade nao tem, por exemplo ?sort=inexistente
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail ordenacaoInvalida(PropertyReferenceException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Não existe o campo '" + ex.getPropertyName() + "' para ordenar ou filtrar.");
    }

    // Violacao de constraint do banco (codigo duplicado, saldo negativo). O service
    // ja valida antes; isto cobre requisicoes simultaneas que passem juntas pela validacao.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridade(DataIntegrityViolationException ex) {
        return problema(HttpStatus.CONFLICT, "Operação rejeitada pelo banco",
                "A operação não pode ser concluída porque violaria uma regra do banco de dados. "
                        + "Recarregue a tela e tente de novo.");
    }

    // Outra transacao alterou a mesma pastilha ao mesmo tempo (ver campo versao em Pastilha)
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail conflitoDeVersao(ObjectOptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "Registro desatualizado",
                "Esta pastilha foi alterada por outra operação ao mesmo tempo. "
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
