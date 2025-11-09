# Agent Development Guidelines

- When interacting, generating comments, commit messages, documentation, or plans, be extremely concise and to the point.

## Code Style

- Use Kotlin official code style for formatting.
- Prefer functional programming approach over OOP.
- Use pure functions and high-order functions.
- Enforce immutability of data objects.
- Use value classes and/or type-aliases for simple domain values used across functions.
- Take in consideration the code's performance and security.
- Every code changed and added should be covered by unit tests.
- Prefer concise and meaningful tests over many overlapping weak tests.

## Architecture

- Keep implementations within their own domain package.
- Use Kotest as test framework.
- Use Mockk as mocking library.
- Never downgrade a library to solve Gradle build issues.
- Always check for the newest version of any newly added library.

## Documentation

- Avoid verbose comments in code.
- Avoid single line comments on self-explanatory code.
- Add multiline comments on the class and function level.
- Every changed or added public interfaces, like Rest endpoints and its payloads, should be updated on `README.md`.
- Be extremely concise and to the point.