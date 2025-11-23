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
    implementation(files("../anvil-engine/build/libs/anvil-engine-0.1.3.jar"))
}

application {
    mainClass.set("dev.badkraft.aurora.Loader")
}

tasks.withType<JavaCompile> { options.release = 21 }

tasks.jar {
    manifest {
        //attributes["Premain-Class"] = "com.aurora.agent.ClasspathAgent"
        attributes["Can-Redefine-Classes"] = "true"
        attributes["Can-Retransform-Classes"] = "true"
        attributes["Main-Class"] = "com.aurora.Loader"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)

    // Include all runtime deps + anvil-engine
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })

    // Explicitly merge anvil-engine
    from(zipTree(file("../anvil-engine/build/libs/anvil-engine-0.1.3.jar")))
}