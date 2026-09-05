package de.manikouchakian.kontorzins;

import de.manikouchakian.kontorzins.model.Installment;
import de.manikouchakian.kontorzins.model.LoanTerms;
import de.manikouchakian.kontorzins.schedule.Schedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Annuity.schedule")
class AnnuityScheduleTest {

    @Test
    @DisplayName("250.000 Euro, 3,9 Prozent, 20 Jahre: 240 Raten, am Ende 0,00")
    void realisticLoan() {
        LoanTerms terms = LoanTerms.ofYears(new BigDecimal("250000"), new BigDecimal("0.039"), 20);
        Schedule schedule = Annuity.schedule(terms);

        assertAll(
                () -> assertEquals(240, schedule.durationInMonths()),
                () -> assertEquals(0, schedule.lastInstallment().remainingDebt().signum()),
                () -> assertEquals(0, schedule.totalPrincipal().compareTo(new BigDecimal("250000.00"))),
                () -> assertEquals(0, schedule.totalPaid().compareTo(
                        schedule.totalPrincipal().add(schedule.totalInterest()))));
    }

    @Test
    @DisplayName("der Zinsanteil im ersten Monat ist Restschuld mal Monatszins")
    void firstMonthInterest() {
        LoanTerms terms = LoanTerms.ofYears(new BigDecimal("250000"), new BigDecimal("0.039"), 20);
        Installment first = Annuity.schedule(terms).installment(1);
        // 250.000,00 * 0,00325 = 812,50
        assertEquals(0, first.interest().compareTo(new BigDecimal("812.50")),
                "was: " + first.interest());
    }

    @Test
    @DisplayName("beim Annuitaetendarlehen sinkt der Zinsanteil Monat fuer Monat")
    void interestShareShrinks() {
        LoanTerms terms = LoanTerms.ofYears(new BigDecimal("250000"), new BigDecimal("0.039"), 20);
        List<Installment> rows = Annuity.schedule(terms).installments();

        for (int i = 1; i < rows.size(); i++) {
            Installment previous = rows.get(i - 1);
            Installment current = rows.get(i);
            assertTrue(current.interest().compareTo(previous.interest()) <= 0,
                    "interest grew in month " + current.month());
            assertTrue(current.remainingDebt().compareTo(previous.remainingDebt()) <= 0,
                    "debt grew in month " + current.month());
        }
    }

    @Test
    @DisplayName("ausser der letzten ist jede Rate gleich hoch")
    void paymentIsConstantExceptTheLastOne() {
        LoanTerms terms = LoanTerms.ofYears(new BigDecimal("250000"), new BigDecimal("0.039"), 20);
        Schedule schedule = Annuity.schedule(terms);
        BigDecimal expected = Annuity.monthlyPayment(terms);

        for (int month = 1; month < schedule.durationInMonths(); month++) {
            assertEquals(0, schedule.installment(month).payment().compareTo(expected),
                    "payment differs in month " + month);
        }
    }

    @Test
    @DisplayName("ein zinsloses Darlehen geht auch auf")
    void interestFreeLoan() {
        LoanTerms terms = new LoanTerms(new BigDecimal("1000"), BigDecimal.ZERO, 7);
        Schedule schedule = Annuity.schedule(terms);

        assertAll(
                () -> assertEquals(0, schedule.totalInterest().signum()),
                () -> assertEquals(0, schedule.totalPrincipal().compareTo(new BigDecimal("1000.00"))),
                () -> assertEquals(0, schedule.lastInstallment().remainingDebt().signum()));
    }

    @ParameterizedTest(name = "{0} Euro, {1} Zins, {2} Monate")
    @DisplayName("die Summe der Tilgung ist immer exakt der Darlehensbetrag")
    @CsvSource({
            "250000, 0.039, 240",
            "250000, 0.039, 360",
            "180000, 0.0175, 300",
            "120000, 0.0425, 180",
            " 75000, 0.0299, 120",
            " 50000, 0.05,    60",
            " 30000, 0.061,   84",
            " 15000, 0.0999,  36",
            " 10000, 0.0,     24",
            "  5000, 0.015,   12",
            "  1200, 0.07,     7",
            "   999, 0.0333,  13",
            "   100, 0.02,     3",
            "    10, 0.05,     5",
            "     1, 0.04,     2",
            "333333.33, 0.0333, 333",
            "1000000, 0.0125, 480",
            "  2500, 0.0,      7",
            " 42000, 0.0289, 111",
            "  8888, 0.0888,  88"
    })
    void principalSumIsExact(String principal, String rate, int months) {
        LoanTerms terms = new LoanTerms(new BigDecimal(principal.trim()), new BigDecimal(rate.trim()), months);
        Schedule schedule = Annuity.schedule(terms);

        assertAll(
                () -> assertEquals(months, schedule.durationInMonths()),
                () -> assertEquals(0, schedule.totalPrincipal().compareTo(terms.principal()),
                        "sum of principal parts was " + schedule.totalPrincipal()),
                () -> assertEquals(0, schedule.lastInstallment().remainingDebt().signum(),
                        "remaining debt was " + schedule.lastInstallment().remainingDebt()),
                () -> assertEquals(0, schedule.totalPaid().compareTo(
                        schedule.totalPrincipal().add(schedule.totalInterest()))));
    }

    @Test
    @DisplayName("laengere Laufzeit heisst mehr Zinsen insgesamt")
    void longerMeansMoreInterest() {
        BigDecimal amount = new BigDecimal("250000");
        BigDecimal rate = new BigDecimal("0.039");
        BigDecimal twentyYears = Annuity.schedule(LoanTerms.ofYears(amount, rate, 20)).totalInterest();
        BigDecimal thirtyYears = Annuity.schedule(LoanTerms.ofYears(amount, rate, 30)).totalInterest();

        assertTrue(thirtyYears.compareTo(twentyYears) > 0,
                thirtyYears + " should be more than " + twentyYears);
    }

    @Test
    @DisplayName("ohne Eckdaten kein Plan")
    void nullTerms() {
        assertThrows(IllegalArgumentException.class, () -> Annuity.schedule(null));
    }

    @Test
    @DisplayName("die alte Signatur und die neue liefern dieselbe Rate")
    void bothOverloadsAgree() {
        LoanTerms terms = LoanTerms.ofYears(new BigDecimal("250000"), new BigDecimal("0.039"), 20);
        assertEquals(0, Annuity.monthlyPayment(terms).compareTo(
                Annuity.monthlyPayment(new BigDecimal("250000"), new BigDecimal("0.039"), 240)));
    }

    @Test
    @DisplayName("ein Plan mit veraenderter Zeile kommt gar nicht erst zustande")
    void tamperedRowIsRejected() {
        LoanTerms terms = new LoanTerms(new BigDecimal("300.00"), BigDecimal.ZERO, 3);
        List<Installment> rows = new ArrayList<>(Annuity.schedule(terms).installments());
        rows.set(0, new Installment(1, new BigDecimal("50.00"), BigDecimal.ZERO,
                new BigDecimal("50.00"), new BigDecimal("250.00")));

        assertThrows(IllegalArgumentException.class, () -> new Schedule(terms, rows));
    }
}
