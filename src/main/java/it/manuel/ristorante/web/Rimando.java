package it.manuel.ristorante.web;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;

/** Calcola in modo sicuro la pagina a cui tornare dopo un'azione (solo indirizzi interni). */
public final class Rimando {

    private Rimando() {
    }

    public static String indietro(HttpServletRequest request, String predefinito) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                if (uri.getHost() == null || uri.getHost().equalsIgnoreCase(request.getServerName())) {
                    String path = uri.getRawPath();
                    if (path != null && path.startsWith("/") && !path.startsWith("//")) {
                        return path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // referer non valido: si usa la pagina predefinita
            }
        }
        return predefinito;
    }
}
