# kontor-zins

![CI](https://github.com/manikouchakian/kontor-zins/actions/workflows/ci.yml/badge.svg)

Loan calculator in Java 17. Part of the [Kontor](https://github.com/manikouchakian) series.

All monetary amounts are calculated with `BigDecimal` and an explicit
`RoundingMode`, never with floating point. Over the lifetime of a loan,
rounding errors in `double` accumulate into real money.

## Build

```
mvn verify
```

## Current state

Monthly payment of an annuity loan, validated and tested.

## Roadmap

- Full amortization schedule (month, interest, principal, remaining debt)
- Constant-amortization and bullet loans behind a shared interface
- Extra repayments
- CLI with table and CSV output

## Design decisions

See [`docs/adr/`](docs/adr/).

## License

MIT
