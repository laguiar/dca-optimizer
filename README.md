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

## Withdrawal Duration Calculator

The application includes a feature to calculate how many years a total wealth amount will last, given specific withdrawal and return parameters.

### How It Works

The withdrawal calculator provides two different approaches to determine how long a total wealth amount will last:

1. **Formula-based calculation**: Uses the mathematical formula `n = -ln(1 - r*P/W) / ln(1 + r)` to calculate the duration directly, where:
   - n = number of months
   - r = monthly interest rate (as a decimal)
   - P = principal (total amount)
   - W = monthly withdrawal amount

2. **Simulation-based calculation**: Simulates the withdrawal process month by month, adding returns and subtracting withdrawals until the amount reaches zero.

Both methods handle special cases like infinite duration (when returns exceed withdrawals) and zero return rates.

### Usage Example

```json
{
    "totalAmount": "1000000.00",
    "monthlyWithdraw": "5000.00",
    "expectedYearlyReturn": 4.0
}
```

### Response Example

```json
{
    "years": 25.15,
    "isInfinite": false
}
```

When returns exceed withdrawals, the money will last indefinitely:

```json
{
    "years": Infinity,
    "isInfinite": true
}
```

### Edge Cases Handled

- Zero total amount
- Zero monthly withdrawal
- Zero expected return
- Negative expected return
- Very small monthly withdrawals
- Very large monthly withdrawals
- Cases where returns exceed withdrawals (infinite duration)

## Initial Amount Calculator

The application also includes a feature to calculate the initial amount needed for a specific withdrawal duration.

### How It Works

The initial amount calculator provides two different approaches to determine how much money is needed to last for a specific number of years:

1. **Formula-based calculation**: Rearranges the withdrawal duration formula to solve for the initial amount:
   `P = W * (1 - (1 + r)^(-n)) / r`, where:
   - n = number of months
   - r = monthly interest rate (as a decimal)
   - P = principal (total amount)
   - W = monthly withdrawal amount

2. **Simulation-based calculation**: Uses binary search to find the initial amount that will last for the specified duration.

### Usage Example

```json
{
    "shouldLastForYears": 30.0,
    "monthlyWithdraw": "4000.00",
    "expectedYearlyReturn": 6.0
}
```

### Response Example

```json
{
    "totalAmount": "752487.56"
}
```

### Edge Cases Handled

- Zero years
- Zero monthly withdrawal
- Zero expected return
- Negative expected return

## Advanced Withdrawal Calculator

The application includes an enhanced withdrawal calculator that accounts for inflation and taxes, providing a more realistic projection of financial longevity.

### How It Works

The advanced withdrawal calculator extends the basic withdrawal calculation by incorporating:

1. **Inflation Adjustment**: Reduces the purchasing power of money over time and increases the withdrawal amount annually to maintain the same real value.

2. **Tax Considerations**: Calculates tax on investment returns based on a yearly tax allowance and average tax rate, reducing the effective return.

3. **Real Return Calculation**: Determines the effective return rate after accounting for both inflation and taxes.

4. **Yearly Breakdown**: Provides a detailed year-by-year analysis of the portfolio, showing starting balance, returns, withdrawals, tax paid, inflation impact, and ending balance.

### Usage Example

```json
{
  "totalAmount": 100000.00,
  "monthlyWithdraw": 500.00,
  "expectedYearlyReturn": 7.0,
  "yearlyInflationRate": 2.0,
  "yearlyTaxAllowance": 12000.00,
  "averageTaxRate": 20.0
}
```

### Response Example

```json
{
  "years": 18.75,
  "isInfinite": false,
  "realReturn": 3.6,
  "totalTaxPaid": 2345.67,
  "inflationAdjustedWithdrawal": 750.23,
  "yearlyBreakdown": [
    {
      "year": 1,
      "startingBalance": 100000.00,
      "returns": 7000.00,
      "withdrawals": 6000.00,
      "taxPaid": 0.00,
      "inflationImpact": 2000.00,
      "endingBalance": 99000.00
    },
    // Additional years...
  ]
}
```

### Edge Cases Handled

- Zero total amount
- Zero monthly withdrawal
- Zero expected return
- High inflation scenarios
- Cases where real returns (after inflation and taxes) exceed withdrawals (infinite duration)
- Various tax scenarios including zero tax and high tax rates

### Key Benefits

- More realistic financial planning by accounting for inflation
- Tax-aware calculations for better retirement planning
- Detailed yearly breakdown for deeper analysis
- Comparison between nominal and real (inflation-adjusted) returns

## API Endpoints

The application provides the following API endpoints:

### DCA Optimization

`POST /api/optimize`

Optimizes dollar-cost averaging distribution based on the selected strategy.

### Basic Withdrawal Calculation

`POST /api/calculate-withdrawal`

Calculates how long a total amount will last with regular withdrawals and expected returns.

### Advanced Withdrawal Calculation

`POST /api/calculate-advanced-withdrawal`

Calculates how long a total amount will last, accounting for inflation and taxes.

### Initial Amount Calculation

`POST /api/calculate-target-amount`

Calculates the initial amount needed to sustain withdrawals for a specific duration.

## Running the Application

### Prerequisites

- JDK 21 or higher
- Gradle

### Local Development

1. Clone the repository
2. Run the application:
   ```bash
   ./gradlew run
   ```
3. The application APIs will be available at `http://localhost:8080`
