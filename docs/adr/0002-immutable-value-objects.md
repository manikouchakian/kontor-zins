# ADR 0002 — Unveränderliche Wertobjekte statt loser Parameter

**Status:** Angenommen · **Datum:** 2026-08-30

## Kontext

`Annuity.monthlyPayment(BigDecimal, BigDecimal, int)` bekam Betrag, Zinssatz und
Laufzeit einzeln übergeben. Zwei Probleme daran:

1. Die beiden `BigDecimal` lassen sich vertauschen, ohne dass der Compiler etwas merkt.
   `monthlyPayment(zins, betrag, monate)` kompiliert und liefert Unsinn.
2. Die Prüfung der Werte stand in der Methode. Jede weitere Methode, die dieselben
   drei Werte braucht — und der Tilgungsplan braucht sie — hätte dieselben sechs
   `if`-Blöcke noch einmal gebraucht.

Dasselbe gilt für eine Zeile des Tilgungsplans: fünf Werte, die zusammengehören und
zwischen denen eine feste Beziehung gilt.

## Entscheidung

Zwei unveränderliche Wertobjekte: `LoanTerms` und `Installment`.

- Geprüft wird im Konstruktor. Ein Objekt, das existiert, ist gültig.
- Die Werte werden dort auch **normalisiert**: Beträge auf Cent, Zinssätze auf zwölf
  Stellen. Ohne das wären `250000` und `250000.00` zwei verschiedene `LoanTerms`,
  weil `BigDecimal.equals` die Skala mitvergleicht.
- `Installment` prüft zusätzlich die Beziehung zwischen seinen Feldern:
  `payment = interest + principalPart`.
- Abgeleitete Werte wie `monthlyRate()` sind Methoden auf dem Objekt, damit die Zahl
  im ganzen Projekt aus genau einer Formel kommt.

Umgesetzt in zwei Schritten, absichtlich in zwei Commits:

1. von Hand als `final class` mit `private final`-Feldern, Gettern, `equals`,
   `hashCode` und `toString`
2. dieselben Klassen als `record`

## Alternativen

**So lassen und nur die Prüfung in eine Hilfsmethode ziehen.** Löst das
Vertauschproblem nicht. Die Signatur bleibt genauso verwechselbar wie vorher.

**Veränderliche Klassen mit Settern.** Dann gäbe es Objekte in einem halbfertigen
Zustand, und jede Methode müsste wieder prüfen, ob inzwischen jemand etwas geändert
hat. Bei Geld ist das der falsche Weg.

**Direkt als `record` anfangen.** Wäre schneller gewesen. Ich wollte die 158 Zeilen
Handarbeit einmal geschrieben haben, um zu sehen, was `record` eigentlich ersetzt:
`Installment` ging von 81 Zeilen Code auf 36, `LoanTerms` von 77 auf 40 — bei
identischem Verhalten. Die Testdatei ist zwischen beiden Commits **unverändert** und
in beiden grün. Das war der Beweis dafür, dass ein `record` wirklich nur
Schreibarbeit spart und keine Semantik ändert.

## Konsequenzen

**Leichter:** Neue Rechenarten bekommen `LoanTerms` als einen Parameter. Ein
Rechenfehler in der Schleife fällt sofort in der betroffenen Zeile auf, weil
`Installment` sie gar nicht erst entstehen lässt.

**Schwerer:** Änderungen bedeuten immer ein neues Objekt. Für Sondertilgungen später
heißt das: kein Feld nachträglich setzen, sondern einen neuen Plan bauen. Das ist
mehr Tipparbeit und die richtige Richtung.

**Offen:** `record` erlaubt keine Konstruktor-Überladung mit gleicher Signatur.
Die Variante "Laufzeit in Jahren" ist deshalb die statische Fabrikmethode
`LoanTerms.ofYears(...)`. Am Aufrufort ist das ohnehin lesbarer.
