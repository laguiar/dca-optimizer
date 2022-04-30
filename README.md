# dca-optimizer

This project uses [Quarkus](https://quarkus.io) and [Kotlin](https://kotlinlang.org), and it's intended to be just a playground.

> **_NOTE:_**  This project is not a trading-bot nor a financial adviser tool, try it out on your own.

## Dollar-cost averaging and optimization

DCA is an investment strategy where a person constantly buy more of its assets, _(every month, or week, etc)_, in order to avoid trying to time the market and find lows and highs.

It's a very know strategy and said to be the best one for non-professional investors. 

## How optimize the DCA strategy and better balance your portfolio

If you hold multiple assets, you probably should give a weight target for each on them on your portfolio.

The **dca-optimizer** tries to use some criteria to better distribute your dca investment calculation.

Some criteria are:

- The asset weight has to be smaller than its target.
- How far below the weight is from its target.
- The asset _"distance"_ from its ATH (all-time high) or 54 weeks high how also often used for stocks, can be used to define if an asset will be invested.

### Example

Imagine a hypothetical portfolio with 5 assets:

```kotlin
Asset(ticker = "A", weight = 25.0, target = 20.0, belowAth = 20.0), // over-target
Asset(ticker = "B", weight = 15.0, target = 20.0, belowAth = 8.0), // under-target but too close to ATH
Asset(ticker = "C", weight = 15.0, target = 25.0, belowAth = 15.0), // under-target
Asset(ticker = "D", weight = 10.0, target = 25.0, belowAth = 15.0), // under-target
Asset(ticker = "E", weight = 5.0, target = 10.0, belowAth = 22.0)   // under-target
```

In a configuration where you define that the ATH threshold is 10% _(only assets that are below this value will be invested)_:

- The first asset with ticker **A**, won't be invested, because it's over its target. 
- The second asset with ticker **B**, won't be invested, because it's below the ATH threshold of 10%.

### Payload examples and configurations

**TODO**
