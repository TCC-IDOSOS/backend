package com.core.fisioaging.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email ou senha inválidos");
    }
}
