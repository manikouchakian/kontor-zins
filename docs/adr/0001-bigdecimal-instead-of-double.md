# ADR 0001 — BigDecimal instead of double for money

Status: accepted · 2026-08-18

## Context

kontor-zins calculates loan payments. A loan runs for 10 to 30 years, so the same
calculation repeats a few hundred times and the results get added up.

`double` cannot represent most decimal fractions exactly. `0.1` is not 0.1, it is
slightly more. One such error is invisible. Three hundred of them, summed over an
amortization schedule, are not: the remaining debt at the end does not reach zero.

There is a second problem. `double` has no scale. It cannot say "this amount is in
cents". Rounding then happens wherever someone remembers to do it, which means it
happens differently in different places.

## Decision

All monetary amounts use `BigDecimal` with an explicit `RoundingMode`.

- Every `divide` gets a scale or a `MathContext`. `BigDecimal.divide` without one
  throws `ArithmeticException` when the result does not terminate, so this is not
  really optional.
- Intermediate steps run at high precision, `MathContext(20, HALF_UP)`. Only the
  result is rounded to two decimal places. Rounding earlier would shift the payment.
- Amounts are compared with `compareTo`, not `equals`. `equals` also compares the
  scale, so `100.00` and `100.0` would count as different.

One exception: in a test that asserts a known result I use `assertEquals`. There
the scale is part of what I want to check, because the method promises two decimal
places.

## Consequences

The code gets longer. `a.add(b)` instead of `a + b`, and no arithmetic operators.
I accept that.

I have to decide the rounding at every division instead of letting the language
decide for me. That is the point of this decision, not a side effect.

`double` for a monetary amount is a defect in this repository, not a matter of
style.
