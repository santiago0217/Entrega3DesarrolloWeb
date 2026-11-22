package com.example.demo.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.exception.WompiNoConfiguradoException;
import com.fasterxml.jackson.databind.JsonNode;

// Todo lo que depende de Wompi: arma el link de pago firmado y comprueba la firma de los avisos (webhook).
@Service
public class WompiService {

    private static final String CHECKOUT_URL = "https://checkout.wompi.co/p/";
    private static final String MONEDA = "COP";

    private final String llavePublica;
    private final String secretoIntegridad;
    private final String secretoEventos;
    private final String urlFrontend;

    public WompiService(@Value("${wompi.public-key:}") String llavePublica,
                        @Value("${wompi.integrity-key:}") String secretoIntegridad,
                        @Value("${wompi.events-secret:}") String secretoEventos,
                        @Value("${app.frontend-url:http://localhost:4200}") String urlFrontend) {
        this.llavePublica = llavePublica;
        this.secretoIntegridad = secretoIntegridad;
        this.secretoEventos = secretoEventos;
        this.urlFrontend = urlFrontend.endsWith("/") ? urlFrontend.substring(0, urlFrontend.length() - 1) : urlFrontend;
    }

    // Código único de cada intento de pago. Ej: BB-12-3F2A9C1D
    public String nuevaReferencia(Long reservaId) {
        return "BB-" + reservaId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Wompi pide el monto en centavos: $15.000 pesos = 1500000
    public long aCentavos(Double pesos) {
        return Math.round(pesos * 100);
    }

    // Link de la página de pago de Wompi. La firma de integridad impide que alguien cambie el monto.
    public String urlCheckout(String referencia, long centavos, Long reservaId) {
        if (llavePublica.isBlank() || secretoIntegridad.isBlank()) {
            throw new WompiNoConfiguradoException(
                    "El pago en línea no está configurado. Faltan las llaves de Wompi en el servidor.");
        }
        String firma = sha256(referencia + centavos + MONEDA + secretoIntegridad);
        String retorno = urlFrontend + "/cliente/pago/resultado/" + reservaId;
        return CHECKOUT_URL
                + "?public-key=" + codificar(llavePublica)
                + "&currency=" + MONEDA
                + "&amount-in-cents=" + centavos
                + "&reference=" + codificar(referencia)
                + "&signature%3Aintegrity=" + firma
                + "&redirect-url=" + codificar(retorno);
    }

    // Comprueba que el aviso realmente viene de Wompi.
    // checksum = SHA256( valores de signature.properties + timestamp + secreto de eventos )
    public boolean firmaEventoValida(JsonNode evento, String checksumEncabezado) {
        if (secretoEventos.isBlank()) {
            return false;
        }
        JsonNode propiedades = evento.path("signature").path("properties");
        String recibido = evento.path("signature").path("checksum").asText(checksumEncabezado);
        if (!propiedades.isArray() || recibido == null || recibido.isBlank() || !evento.hasNonNull("timestamp")) {
            return false;
        }

        StringBuilder texto = new StringBuilder();
        for (JsonNode propiedad : propiedades) {
            JsonNode valor = evento.path("data");
            for (String parte : propiedad.asText().split("\\.")) {
                valor = valor.path(parte);
            }
            texto.append(valor.asText());
        }
        texto.append(evento.get("timestamp").asText()).append(secretoEventos);

        return MessageDigest.isEqual(
                sha256(texto.toString()).getBytes(StandardCharsets.UTF_8),
                recibido.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    // Estado de Wompi -> estado del Pago en BarberBook
    public String estadoDesde(String estadoWompi) {
        return switch (estadoWompi) {
            case "APPROVED" -> "COMPLETADO";
            case "DECLINED", "ERROR", "VOIDED" -> "RECHAZADO";
            default -> "PENDIENTE";
        };
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
