plugins {
    kotlin("jvm") version "1.4.20-RC"
    `java-gradle-plugin`
}

repositories {
    jcenter()
}

gradlePlugin {
    plugins {
        create("pwPlugins") {
            id = "world.phantasmal.gradle.js"
            implementationClass = "world.phantasmal.gradle.PwJsPlugin"
        }
    }
}
