plugins {
    id("world.phantasmal.js")
}

kotlin {
    js {
        binaries.executable()
    }

    sourceSets {
        getByName("jsMain") {
            dependencies {
                api(project(":web:shared"))
            }
        }

        getByName("jsTest") {
            dependencies {
                implementation(project(":test-utils"))
            }
        }
    }
}

tasks.named("jsBrowserDevelopmentRun") {
    // Not sure why this dependency is necessary, maybe because :web:copyAssemblyWorkerJs depends on
    // :web:assembly-worker:jsBrowserDistribution.
    dependsOn("jsProductionExecutableCompileSync")
}

tasks.named("jsBrowserProductionWebpack") {
    // Not sure why this dependency is necessary, maybe because :web:copyAssemblyWorkerJs depends on
    // :web:assembly-worker:jsBrowserDistribution.
    dependsOn("jsDevelopmentExecutableCompileSync")
}
