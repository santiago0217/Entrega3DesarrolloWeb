package com.example.demo.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entities.Pago;
import com.example.demo.service.PagoService;
import com.example.demo.service.WompiService;
import com.fasterxml.jackson.databind.JsonNode;

// Wompi llama a esta dirección cuando un pago cambia de estado (aprobado, rechazado...).
// Es pública a propósito: no pasa por el interceptor de sesión (solo protege /api/admin, /gerente, etc.).
// La seguridad la da la firma del aviso, no la sesión.
@RestController
@RequestMapping("/api/pagos")
public class WompiWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WompiWebhookController.class);

    private final WompiService wompiService;
    private final PagoService pagoService;

    public WompiWebhookController(WompiService wompiService, PagoService pagoService) {
        this.wompiService = wompiService;
        this.pagoService = pagoService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> recibir(@RequestBody JsonNode evento,
                                        @RequestHeader(value = "X-Event-Checksum", required = false) String checksum) {
        if (!wompiService.firmaEventoValida(evento, checksum)) {
            log.warn("Webhook de Wompi con firma inválida");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"transaction.updated".equals(evento.path("event").asText())) {
            return ResponseEntity.ok().build();
        }

        JsonNode transaccion = evento.path("data").path("transaction");
        Pago pago = pagoService.findByReferencia(transaccion.path("reference").asText(""));
        if (pago == null) {
            log.warn("Webhook de Wompi con referencia desconocida");
            return ResponseEntity.ok().build();
        }
        // Un pago ya aprobado no se vuelve a modificar (Wompi puede reenviar avisos)
        if ("COMPLETADO".equals(pago.getEstado())) {
            return ResponseEntity.ok().build();
        }
        // El monto pagado debe coincidir con el de la reserva
        if (transaccion.path("amount_in_cents").asLong() != wompiService.aCentavos(pago.getMonto())) {
            log.warn("Webhook de Wompi con monto distinto para la referencia {}", pago.getReferencia());
            return ResponseEntity.ok().build();
        }

        pago.setEstado(wompiService.estadoDesde(transaccion.path("status").asText()));
        pago.setTransaccionId(transaccion.path("id").asText(null));
        pagoService.save(pago);
        log.info("Pago {} actualizado a {}", pago.getReferencia(), pago.getEstado());
        return ResponseEntity.ok().build();
    }
}
