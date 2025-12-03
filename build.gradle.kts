version = "0.1.0"

val anvilEngineVersion = "0.1.7"

plugins {
    java
    application
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories { mavenCentral() }

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(files("../anvil-engine/build/libs/anvil-engine-$anvilEngineVersion.jar"))
}

application {
    mainClass.set("dev.badkraft.aurora.Loader")
}

tasks.withType<JavaCompile> { options.release = 21 }

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Premain-Class"] = "dev.badkraft.aurora.agent.RuntimeAgent"
        attributes["Can-Redefine-Classes"] = "true"
        attributes["Can-Retransform-Classes"] = "true"
        attributes["Main-Class"] = "dev.badkraft.aurora.Loader"
    }

    // Include all compiled classes
    from(sourceSets.main.get().output)

    // Include **only** runtime dependencies (anvil-engine comes in here automatically)
    from({
        configurations.runtimeClasspath.get()
            .map { zipTree(it) }
    })
}

tasks.named<JavaExec>("run") {
    jvmArgs = listOf(
        "-javaagent:${layout.buildDirectory.file("libs/aurora-mvp.jar").get().asFile}",
        "-Daurora.project.root=${project.projectDir}")
}

tasks.clean {
    doFirst {
        fileTree("logs").matching {
            include("**/*.log")
        }.forEach { it.delete() }
    }
}