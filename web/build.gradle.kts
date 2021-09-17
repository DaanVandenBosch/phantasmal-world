plugins {
    id("world.phantasmal.js")
}

kotlin {
    js {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
            runTask {
                devServer = devServer!!.copy(
                    open = false,
                    port = 1623,
                )
            }
        }
        binaries.executable()
    }
}

dependencies {
    implementation(project(":psolib"))
}
