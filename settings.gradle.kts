pluginManagement{
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "chunk-restore"


rootDir.listFiles().filter { it.isDirectory && it.name != "build-logic" }.forEach { rootProjectFile ->

    if(File(rootProjectFile, "build.gradle.kts").exists() && rootProjectFile.name != "build-logic") include(rootProjectFile.name)

    rootProjectFile.listFiles().filter { it.isDirectory && (File(it, "build.gradle.kts").exists()) }.forEach { projectFile ->

        "${rootProjectFile.name}-${projectFile.name}".also {
            include(it)
            findProject(":$it")?.apply {
                name = it
                projectDir = projectFile
            }
        }
    }
}
