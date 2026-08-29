package de.manikouchakian.kontorzins.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Die Eckdaten eines Darlehens: Betrag, Nominalzins, Laufzeit.
 *
 * <p>Vorher wanderten diese drei Werte einzeln durch jede Methode. Drei Parameter
 * vom Typ {@code (BigDecimal, BigDecimal, int)} kann man vertauschen, ohne dass der
 * Compiler etwas merkt. Als eigener Typ passiert das nicht mehr, und die Prüfung der
 * Werte findet einmal statt statt in jeder Methode neu.
 *
 * <p>Die Klasse ist unveränderlich: alle Felder sind {@code final}, es gibt keine
 * Setter, und die Klasse selbst ist {@code final}, damit keine Unterklasse die
 * Zusicherung wieder aufweicht.
 */
public final class LoanTerms {

    /** 50 Jahre. Längere Laufzeiten sind in der Praxis kein Darlehen mehr, sondern ein Tippfehler. */
    public static final int MAX_MONTHS = 600;

    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private final BigDecimal principal;
    private final BigDecimal annualRate;
    private final int months;

    /**
     * @param principal  Darlehensbetrag in Euro, größer als 0
     * @param annualRate nominaler Jahreszins als Dezimalzahl, {@code 0.039} für 3,9 %
     * @param months     Laufzeit in Monaten, zwischen 1 und {@value #MAX_MONTHS}
     */
    public LoanTerms(BigDecimal principal, BigDecimal annualRate, int months) {
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
        this.principal = Rounding.money(principal);
        this.annualRate = Rounding.rate(annualRate);
        this.months = months;
    }

    /**
     * Dasselbe Darlehen, aber die Laufzeit in Jahren.
     *
     * <p>Ein zweiter Konstruktor ginge hier nicht: {@code (BigDecimal, BigDecimal, int)}
     * ist bereits vergeben. Überladen ist unmöglich, und das ist kein Nachteil —
     * {@code LoanTerms.ofYears(...)} sagt am Aufrufort, was die Zahl bedeutet.
     */
    public static LoanTerms ofYears(BigDecimal principal, BigDecimal annualRate, int years) {
        if (years <= 0) {
            throw new IllegalArgumentException("years must be positive, was " + years);
        }
        return new LoanTerms(principal, annualRate, Math.multiplyExact(years, 12));
    }

    /** @return Darlehensbetrag */
    public BigDecimal principal() {
        return principal;
    }

    /** @return nominaler Jahreszins als Dezimalzahl */
    public BigDecimal annualRate() {
        return annualRate;
    }

    /** @return Laufzeit in Monaten */
    public int months() {
        return months;
    }

    /**
     * Monatszins, abgeleitet aus dem Jahreszins. Wird an genau einer Stelle berechnet.
     */
    public BigDecimal monthlyRate() {
        return annualRate.divide(MONTHS_PER_YEAR, Rounding.INTERMEDIATE);
    }

    /** @return {@code true}, wenn es sich um ein zinsloses Darlehen handelt */
    public boolean isInterestFree() {
        return annualRate.signum() == 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LoanTerms)) {
            return false;
        }
        LoanTerms that = (LoanTerms) other;
        return months == that.months
                && principal.equals(that.principal)
                && annualRate.equals(that.annualRate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(principal, annualRate, months);
    }

    @Override
    public String toString() {
        return "LoanTerms{principal=" + principal
                + ", annualRate=" + annualRate
                + ", months=" + months + "}";
    }
}
