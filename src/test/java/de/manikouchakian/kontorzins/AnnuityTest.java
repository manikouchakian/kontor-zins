package de.manikouchakian.kontorzins;

import de.manikouchakian.kontorzins.model.LoanTerms;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Annuity mit LoanTerms")
class AnnuityTermsTest {

    @Test
    @DisplayName("die neue Signatur liefert dieselbe Rate wie die alte")
    void bothOverloadsAgree() {
        LoanTerms terms = LoanTerms.ofYears(new BigDecimal("250000"), new BigDecimal("0.039"), 20);
        assertEquals(0, Annuity.monthlyPayment(terms).compareTo(
                Annuity.monthlyPayment(new BigDecimal("250000"), new BigDecimal("0.039"), 240)));
    }

    @Test
    @DisplayName("250.000 Euro, 3,9 Prozent, 20 Jahre: 1.501,81 Euro im Monat")
    void knownValue() {
        LoanTerms terms = LoanTerms.ofYears(new BigDecimal("250000"), new BigDecimal("0.039"), 20);
        assertEquals(0, Annuity.monthlyPayment(terms).compareTo(new BigDecimal("1501.81")),
                "was: " + Annuity.monthlyPayment(terms));
    }

    @Test
    @DisplayName("ohne Eckdaten keine Rate")
    void nullTerms() {
        assertThrows(IllegalArgumentException.class, () -> Annuity.monthlyPayment((LoanTerms) null));
    }
}
