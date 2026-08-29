package de.manikouchakian.kontorzins.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LoanTerms")
class LoanTermsTest {

    private static final BigDecimal RATE = new BigDecimal("0.039");

    @Nested
    @DisplayName("lehnt ungueltige Eingaben ab")
    class Validation {

        @Test
        @DisplayName("Betrag null")
        void nullPrincipal() {
            assertThrows(IllegalArgumentException.class, () -> new LoanTerms(null, RATE, 240));
        }

        @Test
        @DisplayName("Zinssatz null")
        void nullRate() {
            assertThrows(IllegalArgumentException.class,
                    () -> new LoanTerms(new BigDecimal("250000"), null, 240));
        }

        @Test
        @DisplayName("Betrag 0 oder negativ")
        void nonPositivePrincipal() {
            assertThrows(IllegalArgumentException.class, () -> new LoanTerms(BigDecimal.ZERO, RATE, 240));
            assertThrows(IllegalArgumentException.class, () -> new LoanTerms(new BigDecimal("-1"), RATE, 240));
        }

        @Test
        @DisplayName("negativer Zinssatz")
        void negativeRate() {
            assertThrows(IllegalArgumentException.class,
                    () -> new LoanTerms(new BigDecimal("250000"), new BigDecimal("-0.01"), 240));
        }

        @Test
        @DisplayName("Laufzeit 0 oder negativ")
        void nonPositiveMonths() {
            assertThrows(IllegalArgumentException.class, () -> new LoanTerms(new BigDecimal("250000"), RATE, 0));
            assertThrows(IllegalArgumentException.class, () -> new LoanTerms(new BigDecimal("250000"), RATE, -12));
        }

        @Test
        @DisplayName("Laufzeit ueber 50 Jahre")
        void tooManyMonths() {
            assertThrows(IllegalArgumentException.class,
                    () -> new LoanTerms(new BigDecimal("250000"), RATE, LoanTerms.MAX_MONTHS + 1));
        }

        @Test
        @DisplayName("die Fehlermeldung nennt den falschen Wert")
        void messageContainsValue() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> new LoanTerms(new BigDecimal("-5"), RATE, 240));
            assertTrue(e.getMessage().contains("-5"), "message was: " + e.getMessage());
        }
    }

    @Nested
    @DisplayName("normalisiert die Werte")
    class Normalisation {

        @Test
        @DisplayName("250000 und 250000.00 sind dasselbe Darlehen")
        void scaleDoesNotCreateTwoObjects() {
            LoanTerms a = new LoanTerms(new BigDecimal("250000"), RATE, 240);
            LoanTerms b = new LoanTerms(new BigDecimal("250000.00"), RATE, 240);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("0.039 und 0.03900 sind derselbe Zinssatz")
        void rateScaleIsNormalised() {
            LoanTerms a = new LoanTerms(new BigDecimal("250000"), new BigDecimal("0.039"), 240);
            LoanTerms b = new LoanTerms(new BigDecimal("250000"), new BigDecimal("0.03900"), 240);
            assertEquals(a, b);
        }

        @Test
        @DisplayName("der Betrag steht auf Cent")
        void principalHasTwoDecimals() {
            LoanTerms terms = new LoanTerms(new BigDecimal("250000"), RATE, 240);
            assertEquals(2, terms.principal().scale());
        }

        @Test
        @DisplayName("verschiedene Laufzeiten sind verschiedene Darlehen")
        void differentMonthsAreNotEqual() {
            assertNotEquals(new LoanTerms(new BigDecimal("250000"), RATE, 240),
                    new LoanTerms(new BigDecimal("250000"), RATE, 120));
        }
    }

    @Nested
    @DisplayName("ofYears")
    class OfYears {

        @Test
        @DisplayName("20 Jahre sind 240 Monate")
        void yearsBecomeMonths() {
            assertEquals(240, LoanTerms.ofYears(new BigDecimal("250000"), RATE, 20).months());
        }

        @Test
        @DisplayName("liefert dasselbe Objekt wie der Konstruktor")
        void sameAsConstructor() {
            assertEquals(new LoanTerms(new BigDecimal("250000"), RATE, 240),
                    LoanTerms.ofYears(new BigDecimal("250000"), RATE, 20));
        }

        @Test
        @DisplayName("0 Jahre werden abgelehnt")
        void zeroYears() {
            assertThrows(IllegalArgumentException.class,
                    () -> LoanTerms.ofYears(new BigDecimal("250000"), RATE, 0));
        }
    }

    @Nested
    @DisplayName("monthlyRate")
    class MonthlyRate {

        @Test
        @DisplayName("3,9 Prozent im Jahr sind 0,00325 im Monat")
        void divideByTwelve() {
            LoanTerms terms = new LoanTerms(new BigDecimal("250000"), RATE, 240);
            assertEquals(0, terms.monthlyRate().compareTo(new BigDecimal("0.00325")),
                    "was: " + terms.monthlyRate());
        }

        @Test
        @DisplayName("ohne Zinsen ist der Monatszins 0")
        void zeroRate() {
            LoanTerms terms = new LoanTerms(new BigDecimal("1200"), BigDecimal.ZERO, 12);
            assertEquals(0, terms.monthlyRate().signum());
            assertTrue(terms.isInterestFree());
        }

        @Test
        @DisplayName("mit Zinsen ist isInterestFree falsch")
        void notInterestFree() {
            assertFalse(new LoanTerms(new BigDecimal("250000"), RATE, 240).isInterestFree());
        }
    }
}
