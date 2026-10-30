package br.com.ddametalurgica.estoque.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Concentra o tratamento de erro da API num lugar so.
// Quando um service lanca uma excecao, o Spring cai aqui e
// devolve a mensagem para a tela com o status HTTP certo.
//
// So vale para os controllers de /api. As telas do pacote controller.web
// continuam caindo nas paginas de erro HTML (templates/error), senao o
// navegador mostraria JSON no lugar da pagina.
@RestControllerAdvice(basePackages = "br.com.ddametalurgica.estoque.controller.api")
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail regraNegocio(RegraNegocioException ex) {
        return problema(HttpStatus.CONFLICT, "Regra de negocio violada", ex.getMessage());
    }

    // Este pega os erros das anotacoes de validacao (@NotNull, @Positive...)
    // aplicadas no corpo da requisicao (@RequestBody @Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError campo : ex.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(campo.getField(),
                    campo.getDefaultMessage() == null ? "valor invalido" : campo.getDefaultMessage());
        }
        ProblemDetail detalhe = problema(HttpStatus.BAD_REQUEST, "Dados invalidos", "Verifique os campos informados.");
        detalhe.setProperty("erros", erros);
        return detalhe;
    }

    // E este pega a validacao dos parametros de URL (@RequestParam @Min...)
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

    // JSON malformado ou enum inexistente no corpo da requisicao
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail corpoInvalido(HttpMessageNotReadableException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Dados invalidos",
                "Nao foi possivel ler o corpo da requisicao. Confira o formato dos campos enviados.");
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

    // Outra pessoa alterou a mesma pastilha ao mesmo tempo (ver campo versao em Pastilha)
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail conflitoDeVersao(ObjectOptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "Registro desatualizado",
                "Esta pastilha foi alterada por outra operacao enquanto voce editava. "
                        + "Recarregue a tela e tente novamente.");
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String mensagem) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(status, mensagem);
        detalhe.setTitle(titulo);
        return detalhe;
    }
}
