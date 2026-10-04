package dev.dfsantos.myreadings.common;

import dev.dfsantos.myreadings.auth.EmailAlreadyInUseException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("conflict");
        problemDetail.setDetail("Já existe um recurso cadastrado com esses dados");
        return problemDetail;
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ProblemDetail handleEmailAlreadyInUse(EmailAlreadyInUseException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("conflict");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setTitle("unauthorized");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("not-found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ProblemDetail handleInvalidDateRange(InvalidDateRangeException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("validation-error");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    // Cobre o corpo da requisição malformado, incluindo o caso de um valor de enum
    // (ex.: ReadingStatus) fora dos valores aceitos — a desserialização falha antes de
    // chegar ao Bean Validation, então não passa por MethodArgumentNotValidException.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("validation-error");

        if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
            String field = invalidFormatException.getPath().isEmpty()
                    ? null
                    : invalidFormatException.getPath()
                            .get(invalidFormatException.getPath().size() - 1)
                            .getPropertyName();

            String detail = buildInvalidFormatDetail(invalidFormatException, field);
            problemDetail.setDetail(detail);
            if (field != null) {
                problemDetail.setProperty("errors", List.of(new FieldErrorDetail(field, detail)));
            }
            return problemDetail;
        }

        problemDetail.setDetail("Corpo da requisição inválido ou malformado");
        return problemDetail;
    }

    private String buildInvalidFormatDetail(InvalidFormatException ex, String field) {
        Class<?> targetType = ex.getTargetType();
        String fieldDescription = field != null ? " para o campo \"" + field + "\"" : "";

        if (targetType != null && targetType.isEnum()) {
            String allowedValues = Arrays.stream(targetType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return "Valor \"" + ex.getValue() + "\" inválido" + fieldDescription
                    + ". Valores permitidos: " + allowedValues;
        }

        return "Valor \"" + ex.getValue() + "\" inválido" + fieldDescription;
    }

    // Cobre parâmetros de query/path cujo valor não converte para o tipo esperado pelo
    // bind do Spring — ex.: um valor fora do enum ReadingStatus em "status" na listagem
    // de livros (US-13). Diferente de HttpMessageNotReadableException, que trata erro de
    // conversão no corpo da requisição.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("validation-error");

        Class<?> requiredType = ex.getRequiredType();
        String fieldDescription = " para o parâmetro \"" + ex.getName() + "\"";

        if (requiredType != null && requiredType.isEnum()) {
            String allowedValues = Arrays.stream(requiredType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            problemDetail.setDetail("Valor \"" + ex.getValue() + "\" inválido" + fieldDescription
                    + ". Valores permitidos: " + allowedValues);
        } else {
            problemDetail.setDetail("Valor \"" + ex.getValue() + "\" inválido" + fieldDescription);
        }

        return problemDetail;
    }

    // Cobre violações de @Min/@Max (e outras anotações de Bean Validation) aplicadas
    // diretamente em @RequestParam de método de controller (ex.: minRating/maxRating em
    // BookController.list, US-15) — diferente de MethodArgumentNotValidException, que só
    // cobre @Valid em corpo de requisição (DTO).
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorDetail> errors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldErrorDetail(lastPathNode(violation), violation.getMessage()))
                .toList();

        String detail = errors.stream()
                .map(error -> error.field() + ": " + error.message())
                .collect(Collectors.joining("; "));

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("validation-error");
        problemDetail.setDetail(detail);
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    private String lastPathNode(ConstraintViolation<?> violation) {
        String fullPath = violation.getPropertyPath().toString();
        int lastDot = fullPath.lastIndexOf('.');
        return lastDot == -1 ? fullPath : fullPath.substring(lastDot + 1);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        String detail = errors.stream()
                .map(error -> error.field() + ": " + error.message())
                .collect(Collectors.joining("; "));

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("validation-error");
        problemDetail.setDetail(detail);
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    private record FieldErrorDetail(String field, String message) {
    }
}
