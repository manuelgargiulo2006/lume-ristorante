package it.manuel.ristorante.web;

import it.manuel.ristorante.service.AiService;
import it.manuel.ristorante.service.OperazioneNonValidaException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Map;

/**
 * Errori "previsti" (es. tavolo già occupato): nelle pagine si torna indietro mostrando il messaggio,
 * nelle API si risponde con un JSON {"errore": "..."}.
 */
@ControllerAdvice
public class GestioneErrori {

    @ExceptionHandler(OperazioneNonValidaException.class)
    public Object operazioneNonValida(OperazioneNonValidaException e, HttpServletRequest request) {
        return rispondi(e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(AiService.AiNonDisponibileException.class)
    public Object aiNonDisponibile(AiService.AiNonDisponibileException e, HttpServletRequest request) {
        return rispondi(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    private Object rispondi(String messaggio, HttpStatus status, HttpServletRequest request) {
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            return ResponseEntity.status(status).body(Map.of("errore", messaggio));
        }
        var flash = RequestContextUtils.getOutputFlashMap(request);
        if (flash != null) {
            flash.put("errore", messaggio);
        }
        return "redirect:" + Rimando.indietro(request, "/gestione");
    }
}
