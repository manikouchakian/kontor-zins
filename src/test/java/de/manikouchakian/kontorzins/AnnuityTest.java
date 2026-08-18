package de.manikouchakian.kontorzins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnuityTest {

    @Test
    @DisplayName("Ohne Zinsen wird der Betrag gleichmäßig verteilt")
    void zeroInterestSplitsEvenly() {
        assertEquals(new BigDecimal("100.00"),
                Annuity.monthlyPayment(new BigDecimal("1200"), BigDecimal.ZERO, 12));
    }

    @Test
    @DisplayName("Höherer Zinssatz führt zu höherer Rate")
    void higherRateMeansHigherPayment() {
        BigDecimal low = Annuity.monthlyPayment(new BigDecimal("100000"), new BigDecimal("0.02"), 120);
        BigDecimal high = Annuity.monthlyPayment(new BigDecimal("100000"), new BigDecimal("0.05"), 120);
        assertTrue(high.compareTo(low) > 0);
    }

    @Test
    @DisplayName("Längere Laufzeit führt zu niedrigerer Rate")
    void longerTermMeansLowerPayment() {
        BigDecimal jahre10 = Annuity.monthlyPayment(new BigDecimal("100000"), new BigDecimal("0.04"), 120);
        BigDecimal jahre20 = Annuity.monthlyPayment(new BigDecimal("100000"), new BigDecimal("0.04"), 240);
        assertTrue(jahre20.compareTo(jahre10) < 0);
    }

    @Test
    @DisplayName("Negativer Darlehensbetrag wird abgelehnt")
    void negativePrincipalIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Annuity.monthlyPayment(new BigDecimal("-1"), new BigDecimal("0.04"), 120));
    }

    @Test
    @DisplayName("Laufzeit von null Monaten wird abgelehnt")
    void zeroMonthsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Annuity.monthlyPayment(new BigDecimal("100000"), new BigDecimal("0.04"), 0));
    }

    @Test
    @DisplayName("Negativer Zinssatz wird abgelehnt")
    void negativeRateIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Annuity.monthlyPayment(new BigDecimal("100000"), new BigDecimal("-0.01"), 120));
    }
}
