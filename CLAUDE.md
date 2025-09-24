# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

### Build and Run
- `./gradlew run` - Run the application (starts Ktor server on port 8080)
- `./gradlew build` - Build the project and run all tests
- `./gradlew clean` - Clean build artifacts

### Testing
- `./gradlew test` - Run all tests using junit framework
- Tests use jUnit 5, Kotest for assertions and Mockk for mocking
- Test classes are located in `src/test/kotlin/`

### Other Useful Commands
- `./gradlew shadowJar` - Create a fat JAR with all dependencies
- `./gradlew runFatJar` - Build and run the fat JAR

## Architecture Overview

This is a Kotlin/Ktor application that provides DCA (Dollar-Cost Averaging) optimization and financial withdrawal calculations.

### Core Structure
- **Main Application**: `src/main/kotlin/io/github/dca/Application.kt` - Entry point with Ktor configuration
- **API Routes**: `src/main/kotlin/io/github/dca/plugins/Routing.kt` - HTTP endpoints
- **Domain Packages**: Code is organized by domain under `src/main/kotlin/io/github/dca/`
  - `strategy/` - DCA optimization strategies (Weight, Portfolio, etc.)
  - `withdrawal/` - Withdrawal calculation functions and models
  - `tax/` - Tax-related calculations
  - `quotes/` - Market data client interfaces

### Key Design Principles
- **Functional Programming**: Prefer pure functions and immutability
- **Domain-Driven**: Each domain has its own package with models and functions
- **Value Classes**: Used for domain values passed between functions
- **Kotest**: Test framework with focus on meaningful, non-overlapping tests

### API Endpoints
- `POST /api/optimize` - DCA optimization with various strategies
- `POST /api/calculate-withdrawal` - Basic withdrawal duration calculation
- `POST /api/calculate-advanced-withdrawal` - Advanced withdrawal with inflation/taxes
- `POST /api/calculate-target-amount` - Calculate initial amount needed
- `POST /api/simulate-withdrawal` - Simulation-based withdrawal calculation

### Strategy Types
- **WEIGHT**: Distribution based on distance from target weight
- **TARGET**: Distribution based on asset targets (excludes over-weighted)
- **PORTFOLIO**: All assets invested, over-weighted get reduced targets
- **RATING**: Distribution based on asset ratings only
- **CONVEX_OPTIMIZATION**: Uses mathematical optimization (linear programming) to find the optimal distribution that minimizes deviation from target weights while respecting constraints.

## Development Guidelines

From AGENTS.md:
- Use Kotlin official code style
- Prefer functional programming over OOP
- Use pure functions and high-order functions
- Enforce immutability of data objects
- Use value classes for simple domain values
- Prefer ForEach/map/filter/fold over loops
- Avoid side effects in functions
- Every code change should be covered by unit tests
- Keep implementations within their own domain package
- Use junit 5 as test framework, Kotest for assertions and mockk for mocking
- Avoid verbose comments, prefer multiline comments at class/function level
- Update README.md for any public interface changes