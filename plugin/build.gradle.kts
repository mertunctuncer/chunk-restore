plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.buildlogic)
}

group = "dev.peopo.chunkrestore"
version = "0.1.0"

buildLogic {
    toolChainVersion = JavaVersion.VERSION_21
    compileVersion = JavaVersion.VERSION_21

    configureTests = false
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
}


dependencies {
    paperweight.paperDevBundle(libs.versions.folia.api)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.core)

    implementation(libs.driver.mongodb.kotlin)
    implementation(libs.mongodb.bson)

    compileOnly(libs.lands)
    compileOnly(libs.worldguard)
    implementation(libs.minimessage)
}

tasks.assemble {
    dependsOn(tasks.getByName("reobfJar"))
}

val foliaServerDir = File(projectDir, "run-folia-server")

tasks.register("runFolia") {

    dependsOn(tasks.getByName("copyPluginJar"))

    doLast {
        javaexec {
            foliaServerDir.mkdirs()
            workingDir = foliaServerDir
            classpath = files(File(rootDir, "folia-server/folia-bundler-1.20.2-R0.1-SNAPSHOT-reobf.jar"))
        }
    }
}
tasks.register("copyPluginJar") {

    dependsOn(tasks.getByName("assemble"))

    doLast {
        val pluginJar = File(projectDir, "build/libs/${project.name}-${project.version}.jar")
        val foliaPluginDir = File(projectDir, "run-folia-server/plugins")
        val existingPluginJar = File(foliaPluginDir, "${rootProject.name}.jar")
        if (existingPluginJar.exists()) existingPluginJar.deleteRecursively()

        pluginJar.copyTo(File(foliaServerDir, "plugins/${rootProject.name}.jar"))
    }
}

