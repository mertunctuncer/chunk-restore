package dev.peopo.buildlogic

import org.gradle.api.JavaVersion

abstract class BuildLogicPluginExt {
    var toolChainVersion = JavaVersion.current()
    var compileVersion = toolChainVersion

    var configureTests: Boolean = true

    val appliedPlugins: MutableSet<String> = mutableSetOf()
}