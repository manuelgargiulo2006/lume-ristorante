package it.manuel.ristorante.dto;

import java.math.BigDecimal;

/** Riga della classifica dei piatti più venduti. */
public record PiattoVenduto(String nome, String categoria, int porzioni, BigDecimal incasso, int percentuale) {
}
