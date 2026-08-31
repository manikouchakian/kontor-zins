package de.manikouchakian.kontorzins.model;

import java.math.BigDecimal;

/**
 * Eine Zeile im Tilgungsplan: was in einem Monat passiert.
 *
 * <p>Die Zerlegung der Rate ist der ganze Punkt eines Tilgungsplans. Wer nur die Rate
 * kennt, weiß nicht, wie viel davon Zinsen sind — und genau danach fragt jeder, der
 * einen Kredit aufnimmt.
 *
 * <p>Der Konstruktor prüft die Regel, die immer gelten muss:
 * {@code payment = interest + principalPart}. Eine Zeile, die es gibt, geht auf.
 * Ein Rechenfehler in der Schleife fällt damit sofort auf, und nicht erst in der
 * Summe am Ende, wo man ihn nicht mehr zuordnen kann.
 *
 * @param month         Nummer des Monats, beginnend bei 1
 * @param payment       gesamte Rate des Monats
 * @param interest      Zinsanteil
 * @param principalPart Tilgungsanteil
 * @param remainingDebt Restschuld nach dieser Rate
 */
public record Installment(
        int month,
        BigDecimal payment,
        BigDecimal interest,
        BigDecimal principalPart,
        BigDecimal remainingDebt) {

    public Installment {
        if (month < 1) {
            throw new IllegalArgumentException("month must start at 1, was " + month);
        }
        payment = requireAmount(payment, "payment");
        interest = requireAmount(interest, "interest");
        principalPart = requireAmount(principalPart, "principalPart");
        remainingDebt = requireAmount(remainingDebt, "remainingDebt");

        BigDecimal split = interest.add(principalPart);
        if (!Rounding.sameAmount(payment, split)) {
            throw new IllegalArgumentException(
                    "payment must equal interest + principalPart in month " + month
                            + ": " + payment + " != " + interest + " + " + principalPart);
        }
    }

    /**
     * @return Anteil der Zinsen an der Rate, für die Ausgabe später
     */
    public boolean isInterestOnly() {
        return principalPart.signum() == 0;
    }

    private static BigDecimal requireAmount(BigDecimal value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative, was " + value);
        }
        return Rounding.money(value);
    }
}
