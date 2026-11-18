package com.xrayconnect.financeiro.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage());
    }

    @ExceptionHandler(ConflitoNegocioException.class)
    public ProblemDetail handleConflitoNegocio(ConflitoNegocioException ex) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
            Map<String, List<String>> erros = ex.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            LinkedHashMap::new,
                            Collectors.mapping(
                                    FieldError::getDefaultMessage,
                                    Collectors.toList()
                            )
                    ));

            ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Um ou mais campos possuem valores inválidos."
            );

            problema.setProperty("erros", erros);

            return problema;
        }
    }
