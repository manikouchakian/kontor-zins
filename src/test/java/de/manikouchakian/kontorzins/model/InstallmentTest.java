package de.manikouchakian.kontorzins.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Installment")
class InstallmentTest {

    private static BigDecimal eur(String value) {
        return new BigDecimal(value);
    }

    @Test
    @DisplayName("eine gueltige Zeile geht auf")
    void validRow() {
        Installment row = new Installment(1, eur("1502.29"), eur("812.50"), eur("689.79"), eur("249310.21"));
        assertEquals(1, row.month());
        assertEquals(0, row.payment().compareTo(eur("1502.29")));
    }

    @Test
    @DisplayName("Rate muss Zins plus Tilgung sein")
    void paymentMustEqualSplit() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Installment(1, eur("1500.00"), eur("812.50"), eur("689.79"), eur("249310.21")));
        assertTrue(e.getMessage().contains("month 1"), "message was: " + e.getMessage());
    }

    @Test
    @DisplayName("unterschiedliche Skalen sind kein Fehler")
    void scaleIsNotAnError() {
        Installment row = new Installment(1, eur("100.0"), eur("40"), eur("60.00"), eur("0"));
        assertEquals(2, row.payment().scale());
        assertEquals(2, row.interest().scale());
    }

    @Test
    @DisplayName("Monate beginnen bei 1")
    void monthStartsAtOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new Installment(0, eur("100.00"), eur("40.00"), eur("60.00"), eur("0.00")));
    }

    @Test
    @DisplayName("negative Betraege werden abgelehnt")
    void noNegativeAmounts() {
        assertThrows(IllegalArgumentException.class,
                () -> new Installment(1, eur("100.00"), eur("-40.00"), eur("140.00"), eur("0.00")));
    }

    @Test
    @DisplayName("null wird abgelehnt")
    void noNulls() {
        assertThrows(IllegalArgumentException.class,
                () -> new Installment(1, null, eur("40.00"), eur("60.00"), eur("0.00")));
        assertThrows(IllegalArgumentException.class,
                () -> new Installment(1, eur("100.00"), eur("40.00"), eur("60.00"), null));
    }

    @Test
    @DisplayName("eine reine Zinsrate hat keinen Tilgungsanteil")
    void interestOnly() {
        Installment row = new Installment(1, eur("812.50"), eur("812.50"), eur("0.00"), eur("250000.00"));
        assertTrue(row.isInterestOnly());
        assertFalse(new Installment(1, eur("100.00"), eur("40.00"), eur("60.00"), eur("0.00")).isInterestOnly());
    }

    @Test
    @DisplayName("zwei gleiche Zeilen sind gleich")
    void equality() {
        Installment a = new Installment(3, eur("100.00"), eur("40.00"), eur("60.00"), eur("0.00"));
        Installment b = new Installment(3, eur("100.0"), eur("40"), eur("60.000"), eur("0"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
