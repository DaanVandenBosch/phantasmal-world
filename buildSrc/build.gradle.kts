plugins {
    kotlin("jvm") version "1.4.32"
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("pwPlugins") {
            id = "world.phantasmal.gradle.js"
            implementationClass = "world.phantasmal.gradle.PwJsPlugin"
        }
    }
}
