# Phantasmal World

Phantasmal World is a suite of tools for Phantasy Star Online.

## For Developers

See [features](./FEATURES.md) for a list of features, planned features and bugs.

### Getting Started

1. Install Node.js ([https://nodejs.org/](https://nodejs.org/))
2. Install Yarn ([https://yarnpkg.com/](https://yarnpkg.com/))
3. `cd` to the project directory
4. Install dependencies with `yarn`
5. Launch server on [http://localhost:1623/](http://localhost:1623/) with `yarn start`
6. src/index.ts is the application's entry point

### Unit Tests

Run the unit tests with `yarn test` or `yarn test --watch` if you want the relevant tests to be
re-run whenever a file is changed. The testing framework used is Jest.

### Linting and Code Formatting

ESLint and Prettier are used for linting and formatting.

Run with `yarn lint` and/or configure your editor to use the ESLint/Prettier configuration.

### Production Build

Create an optimized production build with `yarn build`.
