# Phantasmal World

Phantasmal World is a collection of software for Phantasy Star Online.
The [web application](https://www.phantasmal.world/) has a model viewer, quest editor and hunt
optimizer. There is also a work-in-progress [PSO server](psoserv/README.md).

## Developers

Phantasmal World is written in [Kotlin](https://kotlinlang.org/) and uses
the [Gradle](https://gradle.org/) build tool. Much of the code
is [multiplatform](https://kotlinlang.org/docs/multiplatform.html) and reusable as a library.

<a href="https://github.com/DaanVandenBosch/phantasmal-world/actions?query=workflow%3ATests">
<img alt="Tests status" src="https://github.com/DaanVandenBosch/phantasmal-world/workflows/Tests/badge.svg">
</a>

<a href="https://github.com/DaanVandenBosch/phantasmal-world/actions?query=workflow%3ADeploy">
<img alt="Tests status" src="https://github.com/DaanVandenBosch/phantasmal-world/workflows/Deploy/badge.svg">
</a>

### Features and Bugs

See [features](./FEATURES.md) for a list of features, planned features and bugs.

### Getting Started

1. Install Java 11+ ([GraalVM](https://www.graalvm.org/downloads/) is recommended, and necessary if
   you want to produce native builds of the PSO server)
2. Ensure the JAVA_HOME environment variable is set to JDK's location

Then, for the web application:

1. `cd` to the project directory
2. Launch webpack server on [http://localhost:1623/](http://localhost:1623/)
   with `./gradlew :web:run --continuous`
3. [web/src/main/kotlin/world/phantasmal/web/Main.kt](web/src/main/kotlin/world/phantasmal/web/Main.kt)
   is the application's entry point

For the PSO server:

1. `cd` to the project directory
2. Start the server with `./gradlew :psoserv:run`
3. [psoserv/src/main/kotlin/world/phantasmal/psoserv/Main.kt](psoserv/src/main/kotlin/world/phantasmal/psoserv/Main.kt)
   is the server's entry point

[IntelliJ IDEA](https://www.jetbrains.com/idea/download/) is recommended for development. IntelliJ
setup:

1. Use Ctrl-Alt-Shift-S to open the Project Structure window and select a JDK (you can let IntelliJ
   download a JDK if you don't have a compatible one installed)
2. Configure the Gradle run task for the web application:
    1. In the Gradle window, right click web -> Tasks -> other -> run
    2. Click "Modify Run Configuration..."
    3. Add `--continuous` to the arguments field
    4. Click OK
    5. You can now start the webpack server from the main toolbar

### Exploring the Code Base

The code base is divided up into the following gradle subprojects.

#### core

Core contains the basic utilities that all other subprojects directly or indirectly depend on.

#### psolib

Psolib contains PSO file format parsers, compression/decompression code, a PSO script
assembler/disassembler and a work-in-progress script engine/VM. It also has a model of the PSO
scripting bytecode and data flow analysis for it. This subproject can be used as a library in other
projects.

#### cell

A full-fledged multiplatform implementation of the observer pattern.

#### test-utils

Test utilities used by the other subprojects.

#### [web](web/README.md)

The actual Phantasmal World web application.

#### webgui

Web GUI toolkit used by Phantasmal World.

#### [psoserv](psoserv/README.md)

Work-in-progress PSO server and fully functional PSO proxy server.

### Unit Tests

Run the unit tests with `./gradlew check`. JS tests are run with Karma and Mocha, JVM tests with
Junit 5. Tests can also be run per project with e.g. `./gradlew :psolib:check`.

### Code Style and Formatting

The Kotlin [coding conventions](https://kotlinlang.org/docs/coding-conventions.html) are used.

### Production Builds

#### Web Application

Create an optimized production build with `./gradlew :web:browserDistribution`.

#### PSO Server

Create a native production build with `./gradlew :psoserv:nativeBuild`. Cross-compilation is not
possible, this command has to be run on each platform.
