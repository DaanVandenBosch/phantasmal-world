# Phantasmal World

Phantasmal World is a suite of tools for Phantasy Star Online.

## For Developers

See [features](./FEATURES.md) for a list of features, planned features and bugs.

### Getting Started

1. Install Yarn ([https://yarnpkg.com/](https://yarnpkg.com/))
2. cd to the project directory
3. Install dependencies with `yarn`
4. Launch server on [http://localhost:1623/](http://localhost:1623/) with `yarn start`
5. src/index.ts is the application's entry point

### Tests

Run tests with `yarn test` (or `yarn test --watch`). The testing framework used is Jest.

### Linting and Code Formatting

ESLint and Prettier are used for linting and formatting.

Run with `yarn lint` and/or configure your editor to use the ESLint/Prettier configuration.

### Production Build

Create an optimized production build with `yarn build`.
