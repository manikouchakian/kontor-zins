package de.manikouchakian.kontorzins;

import de.manikouchakian.kontorzins.model.Installment;
import de.manikouchakian.kontorzins.model.LoanTerms;
import de.manikouchakian.kontorzins.model.Rounding;
import de.manikouchakian.kontorzins.schedule.Schedule;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

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
     * @param principal Darlehensbetrag K, muss größer als 0 sein
     * @param annualRate nominaler Jahreszins als Dezimalzahl, z. B. 0.04 für 4 %
     * @param months Laufzeit n in Monaten, muss größer als 0 sein
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

    /**
     * Dieselbe Rechnung, aber mit den Eckdaten als einem Objekt.
     *
     * <p>Die alte Signatur bleibt bestehen, diese hier ruft sie auf. Die Formel steht
     * damit weiterhin an genau einer Stelle im Projekt.
     *
     * @param terms Eckdaten des Darlehens
     * @return monatliche Rate
     */
    public static BigDecimal monthlyPayment(LoanTerms terms) {
        if (terms == null) {
            throw new IllegalArgumentException("terms must not be null");
        }
        return monthlyPayment(terms.principal(), terms.annualRate(), terms.months());
    }

    /**
     * Vollständiger Tilgungsplan eines Annuitätendarlehens.
     *
     * <p>Pro Monat gilt:
     *
     * <pre>
     * Zinsanteil    = Restschuld * Monatszins   (auf Cent gerundet)
     * Tilgungsanteil = Rate - Zinsanteil
     * Restschuld    = Restschuld - Tilgungsanteil
     * </pre>
     *
     * <p><b>Die letzte Rate ist der interessante Teil.</b> Weil jeder Zinsanteil auf
     * Cent gerundet wird, landet die Restschuld nach n Monaten nicht bei exakt 0,00,
     * sondern ein paar Cent daneben. Banken lösen das, indem die letzte Rate genau die
     * Restschuld plus deren Zinsen ist. Genau das passiert hier: der letzte
     * Tilgungsanteil wird nicht gerechnet, sondern <em>gesetzt</em>. Damit ist die
     * Summe aller Tilgungsanteile per Konstruktion der Darlehensbetrag, und
     * {@link Schedule} kann das als harte Bedingung prüfen.
     *
     * @param terms Eckdaten des Darlehens
     * @return Tilgungsplan über die gesamte Laufzeit
     */
    public static Schedule schedule(LoanTerms terms) {
        if (terms == null) {
            throw new IllegalArgumentException("terms must not be null");
        }

        BigDecimal payment = monthlyPayment(terms);
        BigDecimal monthlyRate = terms.monthlyRate();
        BigDecimal remainingDebt = terms.principal();
        int lastMonth = terms.months();

        List<Installment> rows = new ArrayList<>(lastMonth);

        for (int month = 1; month <= lastMonth; month++) {
            BigDecimal interest = Rounding.money(remainingDebt.multiply(monthlyRate));
            BigDecimal principalPart;

            if (month == lastMonth) {
                // Die letzte Rate raeumt auf, was die Rundung uebrig gelassen hat.
                principalPart = remainingDebt;
            } else {
                // Kein eigener Waechter noetig: waere die Rate kleiner als der Zins,
                // waere principalPart negativ, und Installment lehnt das im Konstruktor
                // ab. Der Fehler faellt genau dort auf, wo der falsche Wert entsteht.
                principalPart = Rounding.money(payment.subtract(interest));
                if (principalPart.compareTo(remainingDebt) > 0) {
                    principalPart = remainingDebt;
                }
            }

            BigDecimal actualPayment = interest.add(principalPart);
            remainingDebt = remainingDebt.subtract(principalPart);
            rows.add(new Installment(month, actualPayment, interest, principalPart, remainingDebt));
        }

        return new Schedule(terms, rows);
    }
}
