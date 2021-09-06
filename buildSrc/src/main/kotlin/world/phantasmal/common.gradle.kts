package world.phantasmal

plugins {
    kotlin("plugin.serialization") apply false
}

repositories {
    mavenCentral()
}

project.extra["coroutinesVersion"] = "1.5.2"
project.extra["junitVersion"] = "5.7.1"
project.extra["kotlinLoggingVersion"] = "2.0.11"
project.extra["ktorVersion"] = "1.6.1"
project.extra["log4jVersion"] = "2.14.1"
project.extra["serializationVersion"] = "1.2.2"
