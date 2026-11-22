package com.example.demo.dto;

// Link de pago firmado que Angular abre para llevar al cliente a Wompi
public record CheckoutRespuesta(String url, String referencia) {
}
