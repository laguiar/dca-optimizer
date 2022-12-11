# dca-optimizer

This project uses [Ktor](https://ktor.io) and [Kotlin Language](https://kotlinlang.org), and it's intended to be just a playground project.

> **_NOTE:_**  This project is not a trading-bot nor a financial adviser tool, try it out on your own.

## Dollar-cost averaging and optimization

[Dollar-Cost Averaging](https://www.investopedia.com/terms/d/dollarcostaveraging.asp#toc-what-is-dollar-cost-averaging-dca) is an investment strategy where a person constantly buy more of its assets, _(every month, or week, etc)_, in order to avoid trying to time the market.

> **_From Investopedia:_**
>
> Dollar-cost averaging (DCA) is an investment strategy in which an investor divides up the total amount to be invested across periodic purchases of a target asset in an effort to reduce the impact of volatility on the overall purchase.
The purchases occur regardless of the asset's price and at regular intervals.  

## How optimize the DCA strategy and better balance your portfolio

If you hold multiple assets, you probably should give a weight target for each on them on your portfolio.

The **dca-optimizer** tries to use some criteria to better distribute your dca investment calculation.

Some criteria are:

- The asset weight has to be smaller than its target.
- How far below the weight is from its target.
- The asset _"distance"_ from its ATH (all-time high) or 52 weeks high how also often used for stocks, can be used to define if an asset will be invested.

### Example Scenario

Imagine a hypothetical portfolio with 5 assets:

| Ticker | Weight | Target | From ATH |
|:-------|-------:|-------:|---------:|
| A      |  25.0% |  20.0% |    18.0% |
| B      |  15.0% |  20.0% |     8.0% |
| C      |  15.0% |  25.0% |    15.5% |
| D      |  10.0% |  25.0% |    17.1% |
| F      |   5.0% |  10.0% |    22.0% |

In a configuration where you define that the ATH threshold is 10% _(only assets that are below this value will be invested)_:

- The first asset with ticker **A**, won't be invested, because it's over its target.
- The second asset with ticker **B**, won't be invested, because it's below the ATH threshold of 10%.

The point is to help balance out a portfolio with under/over weighted assets and minimize just a little buying assets that are very high in price currently. _(against its ATH/52 weeks price)_

This doesn't guarantee any significant portfolio performance on the long term, but it **might** do slightly better overall.

### Strategies

- **WEIGHT**: The current asset's weight distance from its target is used to determine the DCA distribution. _(Assets with same target might get different results)_
- **TARGET**: The asset's target is used to determine the DCA distribution, over-weighted assets are discarded. _(Assets with same target will get the same result)_
- **PORTFOLIO**: All assets will be invested, but over-weighted assets will have its target reduced and the difference is distributed among all under-target assets.
- **RATING**: All rated assets will be invested, **ONLY** the rating values will be used to calculate the distribution. _(Think on a 5 stars rating system)_

### Payload examples

`POST http://localhost:8080/api/optimize`

```json
{
    "amount": "1000.00",
    "strategy": {
        "type": "WEIGHT",
        "thresholds": {
            "fromAth": 10.0,
            "overTarget": 0.1
        }
    },
    "assets": [
        {
            "ticker": "A",
            "weight": 25.0,
            "target": 20.0,
            "fromAth": 18.0
        },
        {
            "ticker": "B",
            "weight": 15.0,
            "target": 20.0,
            "fromAth": 8.0
        },
        {
            "ticker": "C",
            "weight": 15.0,
            "target": 25.0,
            "fromAth": 15.5
        },
        {
            "ticker": "D",
            "weight": 10.0,
            "target": 25.0,
            "fromAth": 17.1
        },
        {
            "ticker": "E",
            "weight": 5.0,
            "target": 10.0,
            "fromAth": 22.0
        }
    ]
}
```


```json
{
    "amount": "1000.00",
    "strategy": {
        "type": "RATING"
    },
    "assets": [
        {
            "ticker": "A",
            "rating": 3
        },
        {
            "ticker": "B",
            "rating": 5
        },
        {
            "ticker": "C",
            "rating": 5
        },
        {
            "ticker": "D",
            "fromAth": 2
        },
        {
            "ticker": "E",
            "rating": 4
        }
    ]
}
```

### TODOs

- Add instructions to run the application locally or using a docker image.
- Add a configuration option to consider the ATH distance in the calculations.
- Payload validations.
- Make it available via docker image.
- Fetch data from online sources to calculate the ATH percentage for _actual_ asset tickers _(Stocks, ETFs, Cryptos)_.
