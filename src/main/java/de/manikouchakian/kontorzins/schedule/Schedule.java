package de.manikouchakian.kontorzins.schedule;

import de.manikouchakian.kontorzins.model.Installment;
import de.manikouchakian.kontorzins.model.LoanTerms;
import de.manikouchakian.kontorzins.model.Rounding;

import java.math.BigDecimal;
import java.util.List;

/**
 * Der fertige Tilgungsplan eines Darlehens.
 *
 * <p><b>Komposition, nicht Vererbung.</b> {@code Schedule} <em>hat</em> eine Liste von
 * {@link Installment}, er <em>ist</em> keine Liste. Der Unterschied ist keine Formalie:
 * {@code extends ArrayList} würde {@code add}, {@code remove} und {@code clear} nach
 * außen geben. Ein Tilgungsplan, dem man nachträglich eine Rate anhängen kann, ist kein
 * Tilgungsplan mehr. Als Feld kann die Liste nur das, was diese Klasse erlaubt.
 *
 * <p><b>Die Prüfung steht im Konstruktor, nicht in den Methoden.</b> Wenn ein
 * {@code Schedule} existiert, gilt:
 *
 * <ul>
 *   <li>die Monate sind lückenlos von 1 bis n nummeriert</li>
 *   <li>jede Restschuld folgt aus der vorherigen minus dem Tilgungsanteil</li>
 *   <li>die Summe aller Tilgungsanteile ist <em>exakt</em> der Darlehensbetrag</li>
 *   <li>die Restschuld der letzten Rate ist 0,00</li>
 * </ul>
 *
 * <p>Das dritte Kriterium ist der Test, an dem sich jede Rundungsregel entscheidet.
 * Es lässt sich nicht ungefähr erfüllen: entweder es kommt auf den Cent hin, oder
 * das Objekt entsteht nicht.
 */
public final class Schedule {

    private final LoanTerms terms;
    private final List<Installment> installments;

    // Einmal beim Bauen ausgerechnet. Das Objekt ist unveraenderlich, also kann sich
    // keine dieser Summen spaeter noch aendern.
    private final BigDecimal totalInterest;
    private final BigDecimal totalPrincipal;
    private final BigDecimal totalPaid;

    /**
     * @param terms        Eckdaten, aus denen dieser Plan gerechnet wurde
     * @param installments Raten in Monatsreihenfolge, mindestens eine
     * @throws IllegalArgumentException wenn der Plan in sich nicht aufgeht
     */
    public Schedule(LoanTerms terms, List<Installment> installments) {
        if (terms == null) {
            throw new IllegalArgumentException("terms must not be null");
        }
        if (installments == null || installments.isEmpty()) {
            throw new IllegalArgumentException("a schedule needs at least one installment");
        }
        this.terms = terms;
        // Defensive Kopie: der Aufrufer behält seine Liste und kann damit machen,
        // was er will. Unsere ist ab hier unveraenderlich. List.copyOf lehnt
        // ausserdem null-Elemente ab.
        this.installments = List.copyOf(installments);
        validate();

        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal principal = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (Installment row : this.installments) {
            interest = interest.add(row.interest());
            principal = principal.add(row.principalPart());
            paid = paid.add(row.payment());
        }
        this.totalInterest = Rounding.money(interest);
        this.totalPrincipal = Rounding.money(principal);
        this.totalPaid = Rounding.money(paid);
    }

    private void validate() {
        BigDecimal expectedDebt = terms.principal();
        BigDecimal totalPrincipal = BigDecimal.ZERO;

        for (int i = 0; i < installments.size(); i++) {
            Installment row = installments.get(i);
            int expectedMonth = i + 1;
            if (row.month() != expectedMonth) {
                throw new IllegalArgumentException(
                        "installments must be numbered 1..n without gaps, found month "
                                + row.month() + " at position " + expectedMonth);
            }
            expectedDebt = expectedDebt.subtract(row.principalPart());
            if (!Rounding.sameAmount(row.remainingDebt(), expectedDebt)) {
                throw new IllegalArgumentException(
                        "remaining debt in month " + row.month() + " does not follow from the previous row: "
                                + row.remainingDebt() + " != " + expectedDebt);
            }
            totalPrincipal = totalPrincipal.add(row.principalPart());
        }

        if (!Rounding.sameAmount(totalPrincipal, terms.principal())) {
            throw new IllegalArgumentException(
                    "the sum of all principal parts must equal the loan amount exactly: "
                            + totalPrincipal + " != " + terms.principal());
        }
    }

    /**
     * @return Eckdaten, aus denen dieser Plan entstanden ist
     */
    public LoanTerms terms() {
        return terms;
    }

    /**
     * Alle Raten in Monatsreihenfolge.
     *
     * <p>Die zurückgegebene Liste ist unveränderlich. Wer sie ändern will, bekommt eine
     * {@link UnsupportedOperationException} — nicht heimlich einen kaputten Plan.
     *
     * @return unveränderliche Liste aller Raten
     */
    public List<Installment> installments() {
        return installments;
    }

    /**
     * @param month Monat, beginnend bei 1
     * @return die Rate dieses Monats
     */
    public Installment installment(int month) {
        if (month < 1 || month > installments.size()) {
            throw new IllegalArgumentException(
                    "month must be between 1 and " + installments.size() + ", was " + month);
        }
        return installments.get(month - 1);
    }

    /**
     * @return die letzte Rate des Plans
     */
    public Installment lastInstallment() {
        return installments.get(installments.size() - 1);
    }

    /**
     * Tatsächliche Laufzeit.
     *
     * <p>Nicht zwangsläufig {@code terms().months()}: mit Sondertilgungen ist ein
     * Darlehen früher abbezahlt. Deshalb steht die Zahl hier und nicht in
     * {@link LoanTerms}.
     *
     * @return Anzahl der Raten
     */
    public int durationInMonths() {
        return installments.size();
    }

    /**
     * @return Summe aller Zinsanteile, die eigentliche Frage jedes Kreditnehmers
     */
    public BigDecimal totalInterest() {
        return totalInterest;
    }

    /**
     * @return Summe aller Tilgungsanteile, per Konstruktion der Darlehensbetrag
     */
    public BigDecimal totalPrincipal() {
        return totalPrincipal;
    }

    /**
     * @return Summe aller Raten, also was das Darlehen insgesamt kostet
     */
    public BigDecimal totalPaid() {
        return totalPaid;
    }

    /**
     * Restschuld nach einem bestimmten Monat.
     *
     * @param month 0 für den Zustand vor der ersten Rate, sonst 1..n
     * @return offene Restschuld zu diesem Zeitpunkt
     */
    public BigDecimal remainingDebtAfter(int month) {
        if (month == 0) {
            return terms.principal();
        }
        if (month > durationInMonths()) {
            return Rounding.money(BigDecimal.ZERO);
        }
        return installment(month).remainingDebt();
    }

    @Override
    public String toString() {
        return "Schedule{months=" + durationInMonths()
                + ", totalPaid=" + totalPaid()
                + ", totalInterest=" + totalInterest() + "}";
    }
}
