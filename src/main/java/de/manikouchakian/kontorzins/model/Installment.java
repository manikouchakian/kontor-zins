package de.manikouchakian.kontorzins.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Eine Zeile im Tilgungsplan: was in einem Monat passiert.
 *
 * <p>Der Konstruktor prüft die Regel, die immer gelten muss:
 * {@code payment = interest + principalPart}. Eine Zeile, die es gibt, geht auf.
 */
public final class Installment {

    private final int month;
    private final BigDecimal payment;
    private final BigDecimal interest;
    private final BigDecimal principalPart;
    private final BigDecimal remainingDebt;

    /**
     * @param month         Nummer des Monats, beginnend bei 1
     * @param payment       gesamte Rate des Monats
     * @param interest      Zinsanteil
     * @param principalPart Tilgungsanteil
     * @param remainingDebt Restschuld nach dieser Rate
     */
    public Installment(int month, BigDecimal payment, BigDecimal interest,
                       BigDecimal principalPart, BigDecimal remainingDebt) {
        if (month < 1) {
            throw new IllegalArgumentException("month must start at 1, was " + month);
        }
        this.month = month;
        this.payment = requireAmount(payment, "payment");
        this.interest = requireAmount(interest, "interest");
        this.principalPart = requireAmount(principalPart, "principalPart");
        this.remainingDebt = requireAmount(remainingDebt, "remainingDebt");

        BigDecimal split = this.interest.add(this.principalPart);
        if (!Rounding.sameAmount(this.payment, split)) {
            throw new IllegalArgumentException(
                    "payment must equal interest + principalPart in month " + month
                            + ": " + this.payment + " != " + this.interest + " + " + this.principalPart);
        }
    }

    /** @return Nummer des Monats */
    public int month() {
        return month;
    }

    /** @return gesamte Rate des Monats */
    public BigDecimal payment() {
        return payment;
    }

    /** @return Zinsanteil */
    public BigDecimal interest() {
        return interest;
    }

    /** @return Tilgungsanteil */
    public BigDecimal principalPart() {
        return principalPart;
    }

    /** @return Restschuld nach dieser Rate */
    public BigDecimal remainingDebt() {
        return remainingDebt;
    }

    /** @return {@code true}, wenn in diesem Monat nur Zinsen gezahlt werden */
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Installment)) {
            return false;
        }
        Installment that = (Installment) other;
        return month == that.month
                && payment.equals(that.payment)
                && interest.equals(that.interest)
                && principalPart.equals(that.principalPart)
                && remainingDebt.equals(that.remainingDebt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(month, payment, interest, principalPart, remainingDebt);
    }

    @Override
    public String toString() {
        return "Installment{month=" + month
                + ", payment=" + payment
                + ", interest=" + interest
                + ", principalPart=" + principalPart
                + ", remainingDebt=" + remainingDebt + "}";
    }
}
