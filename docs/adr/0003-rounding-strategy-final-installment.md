# ADR 0003 — Rundung und die letzte Rate

**Status:** Angenommen · **Datum:** 2026-09-06

## Kontext

Der Tilgungsplan rechnet pro Monat:

```
Zinsanteil     = Restschuld × Monatszins
Tilgungsanteil = Rate − Zinsanteil
Restschuld     = Restschuld − Tilgungsanteil
```

Der Zinsanteil ist ein Geldbetrag und wird auf Cent gerundet. Bei 240 Monaten passiert
das 240-mal. Rechnet man die Schleife naiv bis zum Ende durch, steht in Monat 240 nicht
`0,00`, sondern bei 250.000 € zu 3,9 % über 20 Jahre eine Restschuld von **5 Cent**.

Fünf Cent klingen nach nichts. Fachlich heißt es: der Kredit ist nach Ablauf der
Laufzeit nicht abbezahlt. Und technisch heißt es, dass keine Summe im Plan mehr
zuverlässig aufgeht — die Prüfung "Summe der Tilgung = Darlehensbetrag" ist dann nicht
mehr benutzbar.

## Entscheidung

Drei Festlegungen.

**1. Rundung gehört an eine Stelle.** `Rounding` hält beide Genauigkeiten:

| | Skala | Modus | Begründung |
|---|---|---|---|
| Geld | 2 | `HALF_UP` | so steht es auf dem Kontoauszug |
| Zinssatz | 12 | `HALF_EVEN` | kein Geldbetrag; auf Cent gerundet wäre er 0 |

Zwischenschritte laufen mit `MathContext(20)`. Gerundet wird erst das Ergebnis.

**2. Es gibt genau einen Monatszins.** `LoanTerms.monthlyRate()`. Die Ratenformel und
die Schleife holen sich denselben Wert. Vorher rechnete jede ihren eigenen, mit
unterschiedlicher Genauigkeit — die Differenz war winzig und hat trotzdem gereicht,
damit die letzte Rate nicht mehr aufging.

**3. Die letzte Rate wird gesetzt, nicht gerechnet.** Im letzten Monat ist der
Tilgungsanteil per Definition die verbliebene Restschuld; die Rate ergibt sich als
Restschuld plus deren Zinsen. Genau so machen es Banken. Im Beispiel oben ist die
letzte Rate 1.501,86 € statt 1.501,81 €.

Damit gilt **per Konstruktion**: die Summe aller Tilgungsanteile ist exakt der
Darlehensbetrag. `Schedule` prüft das im Konstruktor als harte Bedingung, und ein
parametrisierter Test prüft es über 20 Kombinationen aus Betrag, Zins und Laufzeit —
von 1 € über zwei Monate bis 1.000.000 € über 40 Jahre.

## Alternativen

**Restschuld am Ende einfach auf 0 setzen.** Dann stimmt zwar die letzte Zeile, aber
die Summe der Tilgungsanteile stimmt nicht mehr mit dem Darlehensbetrag überein. Der
Fehler wäre nicht weg, sondern nur unsichtbar. Genau die Prüfung, die am meisten wert
ist, hätte man damit aufgegeben.

**Den Rundungsfehler auf alle Raten verteilen.** Ein Cent hier, ein Cent da. Rechnerisch
möglich, fachlich falsch: die Rate eines Annuitätendarlehens ist konstant, das ist ihre
Definition. Ein Kunde, der auf seinen Kontoauszug schaut, würde schwankende Raten
sofort als Fehler melden.

**Alles mit `double` und am Ende einmal runden.** Verworfen in
[ADR 0001](0001-bigdecimal-instead-of-double.md).

**In Cent als `long` rechnen.** Verbreitet und schnell, und für Zinssätze trotzdem
unbrauchbar — 3,9 % / 12 ist in Cent nicht darstellbar. Man bräuchte zwei Zahlensysteme
nebeneinander. `BigDecimal` mit klaren Skalen ist hier das kleinere Übel.

## Konsequenzen

**Leichter:** Jede weitere Darlehensart kann dieselbe Regel benutzen — auch beim
endfälligen Darlehen ist die letzte Rate die, die aufräumt. Die Summenprüfung in
`Schedule` gilt dann unverändert weiter.

**Schwerer:** Die Schleife hat einen Sonderfall für den letzten Monat. Der muss beim
Einbau der Sondertilgungen mitgedacht werden, weil das Darlehen dann früher endet und
"der letzte Monat" nicht mehr `terms.months()` ist. Deshalb gibt `Schedule` seine
Laufzeit über `durationInMonths()` aus der Anzahl der Zeilen zurück und nicht aus
`LoanTerms`.

**Nebenbei gefunden:** Die vierte geplante Prüfung in `Schedule` — "Restschuld der
letzten Rate ist 0,00" — ist überflüssig. Wenn die Kette der Restschulden stimmt und
die Summe stimmt, folgt sie zwangsläufig. Es gibt keinen Testfall, der nur an ihr
scheitert. Sie ist wieder entfernt.
