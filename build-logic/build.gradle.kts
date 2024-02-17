plugins {
    `kotlin-dsl`
}

group = "dev.peopo.buildlogic"
version = "0.1"

dependencies {
    implementation(libs.gradle.kotlin)
}

gradlePlugin {
    gradlePlugin {
        plugins {
            create("buildlogic") {
                id = "dev.peopo.buildlogic"
                version = project.version
                implementationClass = "dev.peopo.buildlogic.BuildLogicPlugin"
            }
        }
    }
}