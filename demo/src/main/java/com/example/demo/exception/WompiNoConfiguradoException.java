package com.example.demo.exception;

// Se lanza cuando faltan las llaves de Wompi (variables de entorno) y se intenta pagar.
public class WompiNoConfiguradoException extends RuntimeException {

    public WompiNoConfiguradoException(String mensaje) {
        super(mensaje);
    }
}
