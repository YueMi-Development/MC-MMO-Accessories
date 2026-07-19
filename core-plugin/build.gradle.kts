plugins {
    java
    id("com.gradleup.shadow") version "8.3.0" // Shadow plugin
}

val pluginName: String by project
val repoUrl: String by project
val developerId: String by project
val developerName: String by project
val apiVersion: String by project
val authors: String by project
val contributors: String by project
val paperApiVersion: String by project
val yuemiLibsApiVersion: String by project
val pluginVersion: String = project.version.toString()

tasks.processResources {
    val props = mapOf(
        "pluginName" to pluginName,
        "version" to pluginVersion,
        "apiVersion" to apiVersion,
        "authors" to authors,
        "contributors" to contributors
    )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}


dependencies {
    implementation(project(":core-api"))
    implementation("org.yuemi:mc-config-libs:1.0.1")
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("org.yuemi:YueMiLibs-api:$yuemiLibsApiVersion")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    enabled = false
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set(pluginName)
    archiveVersion.set(pluginVersion)
    archiveClassifier.set("")

    relocate("org.yuemi.config", "${project.group}.libs.config")

    manifest {
        attributes(
            "Implementation-Title" to pluginName,
            "Implementation-Version" to pluginVersion,
            "Implementation-Vendor" to developerName,
            "License" to "GPL-3.0"
        )
    }
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}
