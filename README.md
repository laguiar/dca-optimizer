# dca-optimizer

This project uses [Quarkus](https://quarkus.io) and [Kotlin](https://kotlinlang.org), and it's intended to be just a playground.

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

### Example

Imagine a hypothetical portfolio with 5 assets:

| Ticker | Weight | Target | From ATH |
|:---|---:|---:|---:|
|A|25.0%| 20.0%|18.0%
|B|15.0%| 20.0%|8.0%
|C|15.0%| 25.0%|15.5%
|D|10.0%| 25.0%|17.1%
|F|5.0%| 10.0%|22.0%

In a configuration where you define that the ATH threshold is 10% _(only assets that are below this value will be invested)_:

- The first asset with ticker **A**, won't be invested, because it's over its target.
- The second asset with ticker **B**, won't be invested, because it's below the ATH threshold of 10%.

The point is to help balancing out a portfolio with under/over weighted assets and minimaze just a little bit byuing assets that are very high in price currently. _(against its ATH/52 weeks price)_

This doesn't guarantee any significant portfolio perfomance on the long term, but it **might** do slightly better overall.

### Strategies

- **WEIGHT**: The current asset weights distance from its target is used to determine the DCA distribution. _(Assets with same target might get different results)_
- **TARGET**: The asset targets of the _"selected in"_ assets is used to determine the DCA distribution. _(Assets with same target will get the same result)_

### TODOs

- Add payload examples and strategy configurations.
- Add new strategy: **_PORTFOLIO_** - all assets are going to be invested but the radio weight:target is used to balanced out over and under target assets.
- Add a configuration option to considere the ATH distance in the calculations.
- Payload validations
- Make it available on Heroku _(or maybe test Render out)_
