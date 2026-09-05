# kontor-zins

Tilgungsrechner in Java 17. Teil der Kontor-Reihe.

Eingabe sind Darlehensbetrag, Nominalzins und Laufzeit. Ausgabe ist der vollständige
Tilgungsplan: für jeden Monat die Rate, wie viel davon Zinsen sind, wie viel getilgt
wird und was noch offen ist.

Alle Geldbeträge werden mit `BigDecimal` und einem ausdrücklichen `RoundingMode`
gerechnet, nie mit Fließkommazahlen. Über 240 Monate summieren sich die Fehler von
`double` zu echtem Geld.

## Was rauskommt

250.000 € zu 3,9 % über 20 Jahre:

```
Rate: 1.501,81 EUR

Monat |       Rate |      Zins |   Tilgung |   Restschuld
    1 |    1501,81 |    812,50 |    689,31 |    249310,69
    2 |    1501,81 |    810,26 |    691,55 |    248619,14
    3 |    1501,81 |    808,01 |    693,80 |    247925,34
  ...
  120 |    1501,81 |    487,65 |   1014,16 |    149032,66
  ...
  239 |    1501,81 |      9,71 |   1492,10 |      1496,99
  240 |    1501,86 |      4,87 |   1496,99 |         0,00

Summe gezahlt : 360.434,45 EUR
davon Zinsen  : 110.434,45 EUR
davon Tilgung : 250.000,00 EUR
```

## Die fünf Cent in der letzten Rate

In Monat 240 steht nicht 1.501,81 sondern **1.501,86**. Das ist kein Fehler, das ist
der interessanteste Teil des Projekts.

Jeder Zinsanteil wird auf Cent gerundet. 239-mal wird also ein winziger Betrag
abgeschnitten oder aufgerundet. Rechnet man den Plan naiv durch, bleiben am Ende ein
paar Cent Restschuld übrig — bei diesem Darlehen genau fünf. Der Kredit wäre nach
20 Jahren nicht abbezahlt.

Banken lösen das genauso wie dieses Programm: **die letzte Rate wird nicht gerechnet,
sie wird gesetzt.** Der letzte Tilgungsanteil ist per Definition die verbliebene
Restschuld, die Rate ergibt sich daraus. Damit ist die Summe aller Tilgungsanteile
zwangsläufig der Darlehensbetrag — auf den Cent, für jede Kombination aus Betrag,
Zins und Laufzeit.

Genau das prüft `Schedule` im Konstruktor, und genau das prüft der wichtigste Test
über 20 verschiedene Darlehen. Details in
[ADR 0003](docs/adr/0003-rounding-strategy-final-installment.md).

## Aufbau

```
model/
  LoanTerms.java     Betrag, Zins, Laufzeit — als ein Wert statt drei Parameter
  Installment.java   eine Zeile des Plans, die immer aufgeht
  Rounding.java      alle Rundungsregeln an einer Stelle
schedule/
  Schedule.java      hat die Liste der Raten und bürgt für ihre Stimmigkeit
Annuity.java         die Formel und die Schleife
```

Drei Gedanken tragen den ganzen Aufbau:

**Ein Objekt, das existiert, ist gültig.** Geprüft wird im Konstruktor, nicht in den
Methoden. `LoanTerms` mit negativem Betrag entsteht gar nicht erst. Ein `Installment`,
bei dem Rate ≠ Zins + Tilgung ist, entsteht nicht. Ein `Schedule`, dessen Summen nicht
aufgehen, entsteht nicht. Danach muss keine Methode mehr misstrauisch sein.

**`Schedule` *hat* eine Liste, er *ist* keine.** Mit `extends ArrayList` wären `add`,
`remove` und `clear` von außen erreichbar. Ein Tilgungsplan, an den man nachträglich
eine Rate anhängen kann, ist kein Tilgungsplan. Als Feld kann die Liste nur das, was
`Schedule` erlaubt — und `List.copyOf` im Konstruktor sorgt dafür, dass auch der
Aufrufer sie nicht mehr von außen ändern kann.

**Rundung ist eine Entscheidung, keine Nebensache.** Sie steht in `Rounding` und
nirgends sonst. Geld auf zwei Stellen `HALF_UP`, Zinssätze auf zwölf. Ein Monatszins
ist kein Geldbetrag — würde man ihn auf Cent runden, wäre er null.

## Was ich beim Bauen gelernt habe

**`BigDecimal.equals` vergleicht auch die Nachkommastellen.** `250000` und `250000.00`
sind zwei verschiedene Objekte, obwohl es derselbe Betrag ist. Deshalb normalisiert
`LoanTerms` im Konstruktor, und für Vergleiche gibt es `Rounding.sameAmount`, das
`compareTo` benutzt. Ohne das wären zwei gleiche Darlehen nicht gleich gewesen.

**Zwei Monatszinsen sind einer zu viel.** Die Ratenformel hatte sich den Monatszins
selbst ausgerechnet, die Schleife im Tilgungsplan auch — mit unterschiedlicher
Genauigkeit. Beide Zahlen waren fast gleich, und "fast" hat gereicht, damit die letzte
Rate nicht mehr aufgeht. Jetzt gibt es `LoanTerms.monthlyRate()`, und beide holen sich
denselben Wert.

**Eine Prüfung war überflüssig.** `Schedule` sollte vier Bedingungen prüfen, unter
anderem "die Restschuld der letzten Rate ist 0,00". Beim Schreiben des Tests dafür ist
mir aufgefallen: wenn die Kette der Restschulden stimmt *und* die Summe der Tilgung
stimmt, ist die letzte Restschuld zwangsläufig null. Es gibt keinen Testfall, der nur
an dieser Prüfung scheitert. Sie ist wieder raus.

**Ein Wächter, den das Wertobjekt schon stellt.** In der Schleife stand eine Abfrage,
die abbricht, wenn die Rate die Zinsen nicht deckt. Sie ist doppelt: wäre das der Fall,
wäre der Tilgungsanteil negativ, und `Installment` lehnt negative Beträge im
Konstruktor ab. Der Fehler fällt dort auf, wo der falsche Wert entsteht, mit einer
Meldung, die den Monat nennt. Beim Annuitätendarlehen kann der Fall ohnehin nicht
eintreten — die Formel liefert immer eine Rate über dem Zinsanteil. Der Fall wird
erst dann echt, wenn die Rate von außen kommt.

**Ein zweiter Konstruktor ging nicht.** `LoanTerms` sollte einen Konstruktor für Monate
und einen für Jahre bekommen. Beide hätten die Signatur `(BigDecimal, BigDecimal, int)`
— das lässt Java nicht zu. Daraus wurde `LoanTerms.ofYears(...)`, und das ist am
Aufrufort sogar deutlich klarer: `new LoanTerms(betrag, zins, 20)` hätte niemand
gelesen, ohne nachzuschlagen, ob 20 Monate oder Jahre sind.

## Build

```bash
mvn verify
```

Java 17, Maven, JUnit 5. Keine weiteren Abhängigkeiten.

## Was noch kommt

- [ ] Tilgungsdarlehen und endfälliges Darlehen hinter einer gemeinsamen Schnittstelle
- [ ] eigene Exceptions statt `IllegalArgumentException`
- [ ] Sondertilgungen
- [ ] Ausgabe als Tabelle und als CSV
- [ ] CLI

## Entscheidungen

Die Überlegungen hinter dem Aufbau stehen als kurze Notizen in [`docs/adr/`](docs/adr/).

## Lizenz

MIT
