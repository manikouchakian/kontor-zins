package de.manikouchakian.kontorzins.schedule;

import de.manikouchakian.kontorzins.model.Installment;
import de.manikouchakian.kontorzins.model.LoanTerms;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Schedule")
class ScheduleTest {

    private static final LoanTerms TERMS =
            new LoanTerms(new BigDecimal("300.00"), BigDecimal.ZERO, 3);

    private static BigDecimal eur(String value) {
        return new BigDecimal(value);
    }

    /** Ein einfacher, in sich stimmiger Plan: 300 Euro, zinslos, drei Monate. */
    private static List<Installment> validRows() {
        List<Installment> rows = new ArrayList<>();
        rows.add(new Installment(1, eur("100.00"), eur("0.00"), eur("100.00"), eur("200.00")));
        rows.add(new Installment(2, eur("100.00"), eur("0.00"), eur("100.00"), eur("100.00")));
        rows.add(new Installment(3, eur("100.00"), eur("0.00"), eur("100.00"), eur("0.00")));
        return rows;
    }

    @Nested
    @DisplayName("laesst keinen kaputten Plan entstehen")
    class Invariants {

        @Test
        @DisplayName("ohne Raten gibt es keinen Plan")
        void emptyIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> new Schedule(TERMS, List.of()));
            assertThrows(IllegalArgumentException.class, () -> new Schedule(TERMS, null));
        }

        @Test
        @DisplayName("ohne Eckdaten gibt es keinen Plan")
        void nullTermsRejected() {
            assertThrows(IllegalArgumentException.class, () -> new Schedule(null, validRows()));
        }

        @Test
        @DisplayName("Monate duerfen keine Luecke haben")
        void gapsAreRejected() {
            List<Installment> rows = validRows();
            rows.set(1, new Installment(5, eur("100.00"), eur("0.00"), eur("100.00"), eur("100.00")));
            IllegalArgumentException e =
                    assertThrows(IllegalArgumentException.class, () -> new Schedule(TERMS, rows));
            assertTrue(e.getMessage().contains("numbered"), "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("die Summe der Tilgung muss der Darlehensbetrag sein")
        void sumMustMatchPrincipal() {
            List<Installment> rows = validRows();
            // letzte Zeile tilgt zu wenig, der Plan geht um einen Cent nicht auf
            rows.set(2, new Installment(3, eur("99.99"), eur("0.00"), eur("99.99"), eur("0.01")));
            assertThrows(IllegalArgumentException.class, () -> new Schedule(TERMS, rows));
        }

        @Test
        @DisplayName("jede Restschuld muss aus der vorherigen folgen")
        void debtChainMustHold() {
            List<Installment> rows = validRows();
            rows.set(0, new Installment(1, eur("100.00"), eur("0.00"), eur("100.00"), eur("999.00")));
            IllegalArgumentException e =
                    assertThrows(IllegalArgumentException.class, () -> new Schedule(TERMS, rows));
            assertTrue(e.getMessage().contains("month 1"), "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("ein Plan, der zu frueh aufhoert, wird abgelehnt")
        void mustBePaidOff() {
            LoanTerms shortTerms = new LoanTerms(new BigDecimal("300.00"), BigDecimal.ZERO, 2);
            List<Installment> rows = new ArrayList<>();
            rows.add(new Installment(1, eur("100.00"), eur("0.00"), eur("100.00"), eur("200.00")));
            rows.add(new Installment(2, eur("100.00"), eur("0.00"), eur("100.00"), eur("100.00")));
            assertThrows(IllegalArgumentException.class, () -> new Schedule(shortTerms, rows));
        }
    }

    @Nested
    @DisplayName("gibt seine Liste nicht aus der Hand")
    class Encapsulation {

        @Test
        @DisplayName("die zurueckgegebene Liste ist unveraenderlich")
        void listIsImmutable() {
            Schedule schedule = new Schedule(TERMS, validRows());
            List<Installment> rows = schedule.installments();
            assertThrows(UnsupportedOperationException.class,
                    () -> rows.add(new Installment(4, eur("1.00"), eur("0.00"), eur("1.00"), eur("0.00"))));
            assertThrows(UnsupportedOperationException.class, rows::clear);
        }

        @Test
        @DisplayName("wer seine eigene Liste aendert, aendert den Plan nicht mit")
        void defensiveCopy() {
            List<Installment> caller = validRows();
            Schedule schedule = new Schedule(TERMS, caller);

            caller.clear();

            assertEquals(3, schedule.durationInMonths());
            assertEquals(0, schedule.totalPrincipal().compareTo(eur("300.00")));
        }
    }

    @Nested
    @DisplayName("rechnet zusammen")
    class Sums {

        private final Schedule schedule = new Schedule(TERMS, validRows());

        @Test
        @DisplayName("Laufzeit, Summen und letzte Rate")
        void totals() {
            assertEquals(3, schedule.durationInMonths());
            assertEquals(0, schedule.totalPaid().compareTo(eur("300.00")));
            assertEquals(0, schedule.totalInterest().compareTo(BigDecimal.ZERO));
            assertEquals(0, schedule.totalPrincipal().compareTo(eur("300.00")));
            assertEquals(3, schedule.lastInstallment().month());
        }

        @Test
        @DisplayName("Restschuld nach Monat 0 ist der volle Betrag")
        void debtBeforeFirstPayment() {
            assertEquals(0, schedule.remainingDebtAfter(0).compareTo(eur("300.00")));
            assertEquals(0, schedule.remainingDebtAfter(2).compareTo(eur("100.00")));
            assertEquals(0, schedule.remainingDebtAfter(3).compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("einen Monat einzeln herausgreifen")
        void singleMonth() {
            assertEquals(2, schedule.installment(2).month());
            assertThrows(IllegalArgumentException.class, () -> schedule.installment(0));
            assertThrows(IllegalArgumentException.class, () -> schedule.installment(4));
        }

        @Test
        @DisplayName("kennt seine Eckdaten noch")
        void keepsTerms() {
            assertEquals(TERMS, schedule.terms());
        }
    }
}
