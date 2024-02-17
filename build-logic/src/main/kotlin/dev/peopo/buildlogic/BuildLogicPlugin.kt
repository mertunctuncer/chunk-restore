package dev.peopo.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

class BuildLogicPlugin : Plugin<Project> {

    override fun apply(target: Project) {

        val ext = registerExtension(target)

        configureKotlin(target, ext.toolChainVersion.ordinal, ext.compileVersion.ordinal)

        if(ext.configureTests)configureTests(target)
    }

    private fun registerExtension(target: Project) =
        target.extensions.create<BuildLogicPluginExt>("buildLogic")


    private fun configureKotlin(target: Project, toolchainVersion: Int, compileVersion: Int) {
        target.plugins.withType<KotlinBasePlugin> {
            target.extensions.configure<KotlinJvmProjectExtension> {
                jvmToolchain(toolchainVersion)
                explicitApi()

                compilerOptions.jvmTarget.set(JvmTarget.valueOf("JVM_$compileVersion"))
            }
        }
    }
    private fun configureTests(target: Project) {
        target.tasks.withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
    }
}