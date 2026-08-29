package de.manikouchakian.kontorzins.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Alle Rundungsregeln des Projekts an einer Stelle.
 *
 * <p>Es gibt zwei Genauigkeiten, und sie sind bewusst verschieden:
 *
 * <ul>
 *   <li><b>Geld</b> wird auf Cent gerundet, {@link RoundingMode#HALF_UP}. So rechnet
 *       eine Bank auf dem Kontoauszug, und so steht es im Tilgungsplan.</li>
 *   <li><b>Zinssätze</b> sind kein Geld. Ein Monatszins von 3,9 % / 12 hat kein Ende.
 *       Er wird auf {@value #RATE_SCALE} Stellen gehalten, damit der Fehler nicht
 *       über 360 Monate mitwächst.</li>
 * </ul>
 *
 * <p>Solange jede Rundung durch diese Klasse geht, gibt es genau eine Stelle,
 * an der sich eine falsche Regel verstecken kann.
 */
public final class Rounding {

    /** Nachkommastellen für Geldbeträge: Cent. */
    public static final int MONEY_SCALE = 2;

    /** Rundung für Geldbeträge. */
    public static final RoundingMode MONEY_MODE = RoundingMode.HALF_UP;

    /** Nachkommastellen für Zinssätze. */
    public static final int RATE_SCALE = 12;

    /** Genauigkeit für Zwischenschritte, die noch kein Ergebnis sind. */
    public static final MathContext INTERMEDIATE = new MathContext(20, RoundingMode.HALF_UP);

    private Rounding() {
        // Utility-Klasse, keine Instanzen
    }

    /**
     * Rundet einen Geldbetrag auf Cent.
     *
     * @param amount Betrag, nicht {@code null}
     * @return derselbe Betrag mit Skala {@value #MONEY_SCALE}
     */
    public static BigDecimal money(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        return amount.setScale(MONEY_SCALE, MONEY_MODE);
    }

    /**
     * Bringt einen Zinssatz auf die festgelegte Genauigkeit.
     *
     * @param rate Zinssatz als Dezimalzahl, nicht {@code null}
     * @return derselbe Satz mit Skala {@value #RATE_SCALE}
     */
    public static BigDecimal rate(BigDecimal rate) {
        if (rate == null) {
            throw new IllegalArgumentException("rate must not be null");
        }
        return rate.setScale(RATE_SCALE, RoundingMode.HALF_EVEN);
    }

    /**
     * Vergleicht zwei Beträge nach Wert, nicht nach Skala.
     *
     * <p>{@code new BigDecimal("1.0").equals(new BigDecimal("1.00"))} ist {@code false}.
     * Für Geld ist das fast nie gemeint, deshalb steht der Vergleich hier und nicht
     * verstreut im Code.
     *
     * @param a erster Betrag
     * @param b zweiter Betrag
     * @return {@code true}, wenn beide denselben Wert haben
     */
    public static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) == 0;
    }
}
