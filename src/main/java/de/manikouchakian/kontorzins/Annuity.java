package de.manikouchakian.kontorzins;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Berechnungen rund um das Annuitätendarlehen.
 *
 * <p>Alle Geldbeträge werden mit {@link BigDecimal} und explizitem
 * {@link RoundingMode} gerechnet, niemals mit Fließkommazahlen.
 */
public final class Annuity {

    /** Zwischenschritte mit hoher Genauigkeit, erst das Ergebnis wird auf Cent gerundet. */
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    private Annuity() {
        // Utility-Klasse, keine Instanzen
    }

    /**
     * Monatliche Rate eines Annuitätendarlehens.
     *
     * <pre>A = K * (i * (1+i)^n) / ((1+i)^n - 1)</pre>
     *
     * @param principal  Darlehensbetrag K, muss größer als 0 sein
     * @param annualRate nominaler Jahreszins als Dezimalzahl, z. B. 0.04 für 4 %
     * @param months     Laufzeit n in Monaten, muss größer als 0 sein
     * @return monatliche Rate, gerundet auf zwei Nachkommastellen
     */
    public static BigDecimal monthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        if (principal == null || annualRate == null) {
            throw new IllegalArgumentException("principal and annualRate must not be null");
        }
        if (principal.signum() <= 0) {
            throw new IllegalArgumentException("principal must be positive");
        }
        if (annualRate.signum() < 0) {
            throw new IllegalArgumentException("annualRate must not be negative");
        }
        if (months <= 0) {
            throw new IllegalArgumentException("months must be positive");
        }

        // Sonderfall 0 %: der Betrag wird gleichmäßig auf die Laufzeit verteilt
        if (annualRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), MC);
        BigDecimal growth = BigDecimal.ONE.add(monthlyRate).pow(months, MC);

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(growth);
        BigDecimal denominator = growth.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
