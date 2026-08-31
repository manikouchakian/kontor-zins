package de.manikouchakian.kontorzins.model;

import java.math.BigDecimal;

/**
 * Die Eckdaten eines Darlehens: Betrag, Nominalzins, Laufzeit.
 *
 * <p>Vorher wanderten diese drei Werte einzeln durch jede Methode. Drei Parameter
 * vom Typ {@code (BigDecimal, BigDecimal, int)} kann man vertauschen, ohne dass der
 * Compiler etwas merkt. Als eigener Typ passiert das nicht mehr, und die Prüfung der
 * Werte findet einmal statt statt in jeder Methode neu.
 *
 * <p>Ein {@code LoanTerms}, das existiert, ist gültig. Ungültige Eingaben werden im
 * Konstruktor abgelehnt und kommen gar nicht erst in das Objekt hinein.
 *
 * @param principal  Darlehensbetrag in Euro, größer als 0, auf Cent gerundet
 * @param annualRate nominaler Jahreszins als Dezimalzahl, {@code 0.039} für 3,9 %
 * @param months     Laufzeit in Monaten, zwischen 1 und {@value #MAX_MONTHS}
 */
public record LoanTerms(BigDecimal principal, BigDecimal annualRate, int months) {

    /** 50 Jahre. Längere Laufzeiten sind in der Praxis kein Darlehen mehr, sondern ein Tippfehler. */
    public static final int MAX_MONTHS = 600;

    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    /**
     * Kompakter Konstruktor: prüft und normalisiert, bevor die Felder gesetzt werden.
     */
    public LoanTerms {
        if (principal == null) {
            throw new IllegalArgumentException("principal must not be null");
        }
        if (annualRate == null) {
            throw new IllegalArgumentException("annualRate must not be null");
        }
        if (principal.signum() <= 0) {
            throw new IllegalArgumentException("principal must be positive, was " + principal);
        }
        if (annualRate.signum() < 0) {
            throw new IllegalArgumentException("annualRate must not be negative, was " + annualRate);
        }
        if (months <= 0) {
            throw new IllegalArgumentException("months must be positive, was " + months);
        }
        if (months > MAX_MONTHS) {
            throw new IllegalArgumentException("months must not exceed " + MAX_MONTHS + ", was " + months);
        }

        // Normalisieren, nicht nur prüfen: sonst wären 250000 und 250000.00 zwei
        // verschiedene Objekte. BigDecimal.equals vergleicht auch die Skala.
        principal = Rounding.money(principal);
        annualRate = Rounding.rate(annualRate);
    }

    /**
     * Dasselbe Darlehen, aber die Laufzeit in Jahren.
     *
     * <p>Ein zweiter Konstruktor ginge hier nicht: {@code (BigDecimal, BigDecimal, int)}
     * ist bereits die Signatur des kanonischen Konstruktors. Überladen ist unmöglich,
     * und das ist kein Nachteil — {@code LoanTerms.ofYears(...)} sagt am Aufrufort,
     * was die Zahl bedeutet. Ein zweiter Konstruktor hätte das nie gekonnt.
     *
     * @param principal  Darlehensbetrag
     * @param annualRate nominaler Jahreszins als Dezimalzahl
     * @param years      Laufzeit in Jahren, größer als 0
     * @return Eckdaten mit {@code years * 12} Monaten
     */
    public static LoanTerms ofYears(BigDecimal principal, BigDecimal annualRate, int years) {
        if (years <= 0) {
            throw new IllegalArgumentException("years must be positive, was " + years);
        }
        return new LoanTerms(principal, annualRate, Math.multiplyExact(years, 12));
    }

    /**
     * Monatszins, abgeleitet aus dem Jahreszins.
     *
     * <p>Wird an genau einer Stelle berechnet. Die Ratenformel und die Schleife im
     * Tilgungsplan benutzen denselben Wert — sonst kaeme die Rate aus einer anderen
     * Zahl als der Plan, und die Differenz wuerde erst in der letzten Rate auffallen.
     *
     * <p>Hier wird bewusst <em>nicht</em> auf zwei Stellen gerundet. Der Monatszins ist
     * kein Geldbetrag, sondern ein Zwischenschritt.
     *
     * @return Jahreszins geteilt durch 12
     */
    public BigDecimal monthlyRate() {
        return annualRate.divide(MONTHS_PER_YEAR, Rounding.INTERMEDIATE);
    }

    /**
     * @return {@code true}, wenn es sich um ein zinsloses Darlehen handelt
     */
    public boolean isInterestFree() {
        return annualRate.signum() == 0;
    }
}
