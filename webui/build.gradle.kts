plugins {
    id("world.phantasmal.js")
}

kotlin {
    sourceSets {
        getByName("jsMain") {
            dependencies {
                api(project(":core"))
                api(project(":cell"))
                implementation(npm("@fortawesome/fontawesome-svg-core", "^1.2.36"))
                implementation(npm("@fortawesome/free-regular-svg-icons", "^5.15.4"))
                implementation(npm("@fortawesome/free-solid-svg-icons", "^5.15.4"))
                implementation(npm("@fortawesome/free-brands-svg-icons", "^5.15.4"))
            }
        }

        getByName("jsTest") {
            dependencies {
                implementation(project(":test-utils"))
            }
        }
    }
}
