# Phantasmal World

[Phantasmal World](https://www.phantasmal.world/) is a suite of tools for Phantasy Star Online.

## Developers

<a href="https://github.com/DaanVandenBosch/phantasmal-world/actions?query=workflow%3ATests">
<img alt="Tests status" src="https://github.com/DaanVandenBosch/phantasmal-world/workflows/Tests/badge.svg">
</a>

See [features](./FEATURES.md) for a list of features, planned features and bugs.

### Getting Started

1. Install Node.js ([https://nodejs.org/](https://nodejs.org/))
2. Install Yarn ([https://yarnpkg.com/](https://yarnpkg.com/))
3. `cd` to the project directory
4. Install dependencies with `yarn`
5. Launch server on [http://localhost:1623/](http://localhost:1623/) with `yarn start`
6. [src/index.ts](src/index.ts) is the application's entry point

### Exploring the Code Base

The code base is divided up into a [core](src/core) module, an [application](src/application) module
and a module per tool (e.g. [quest_editor](src/quest_editor)). The core module contains the base
code that the other modules depend on. The application module contains the
[main application view](src/application/gui/ApplicationView.ts) that provides navigation between the
different tools. The application view lazily loads and initializes the necessary modules. Each other
module represents a tool such as the quest editor or the hunt optimizer.

#### Submodules

All modules have an index.ts file that contains an initialization function. They then have several
common submodules such as controllers, gui, model and stores and some module-specific submodules.

##### GUI

The gui submodule contains views with minimal logic. They simply display what their controller
provides and forward user input to it. Their only dependency is the DOM and a single controller.
Keeping logic out of the views makes the UI easier to test. We don't really need to test the views
as they don't contain complex code, just testing the controller layer gives us confidence that the
UI works. The only automatic tests for the gui layer are
[snapshot tests](https://jestjs.io/docs/en/snapshot-testing).

##### Controllers

The controllers submodule contains the [controllers](src/core/controllers/Controller.ts) on which
views depend. Usually the view-controller relationship is one-to-one, sometimes it's many-to-one
(e.g. when a view has many subviews that work with the same data). A controller usually extracts
data from a shared store and transforms it into a format which the view can easily consume.

##### Model

The model submodule contains observable model objects. Models expose read-only observable properties
and allow their properties to be changed via setters which validate their inputs.

##### Stores

The stores submodule contains shared data [stores](src/core/stores/Store.ts). Stores ensure that
data is loaded when necessary and that the data is deduplicated. Stores also contain ephemeral
shared state such as the currently selected entity in the quest editor.

#### Some Interesting Parts of the Code Base

Phantasmal contains parsers for many of the client's formats in
[src/core/data_formats](src/core/data_formats). A model of the PSO scripting byte code and data flow
analysis for it can be found in [src/core/data_formats/asm](src/core/data_formats/asm). The
[src/quest_editor/scripting](src/quest_editor/scripting) directory contains an assembler,
disassembler and (partly implemented) virtual machine.

### Unit Tests

Run the unit tests with `yarn test` or `yarn test --watch` if you want the relevant tests to be
re-run whenever a file is changed. The testing framework used is Jest.

### Code Style, Linting and Formatting

Class/interface/type names are in `PascalCase` and all other identifiers are in `snake_case`.

ESLint and Prettier are used for linting and formatting. Run with `yarn lint` and/or configure your
editor to use the ESLint/Prettier configuration.

### Production Build

Create an optimized production build with `yarn build`.

### Optional Modules

### prs-rs

Provides faster PRS routines using WebAssembly. Build for WebPack with `yarn build_prs_rs_browser`.
Build for Jest with `yarn build_prs_rs_testing`. Building requires
[wasm-pack](https://github.com/rustwasm/wasm-pack).
