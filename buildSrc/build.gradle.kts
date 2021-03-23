plugins {
    kotlin("jvm") version "1.4.31"
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
