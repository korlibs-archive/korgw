import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation

configurations {
    maybeCreate("rtArtifacts")
}

dependencies {
    val gluegenVersion = project.property("gluegenVersion")
    val joglVersion = project.property("joglVersion")

    add("jvmMainApi", "org.jogamp.gluegen:gluegen-rt:$gluegenVersion")
    add("jvmMainApi", "org.jogamp.jogl:jogl-all:$joglVersion")

    for (target in listOf("linux-amd64", "linux-armv6", "linux-armv6hf", "linux-i586", "macosx-universal", "windows-amd64", "windows-i586")) {
        for (config in listOf("rtArtifacts", "jvmMainApi")) {
            add(config, "org.jogamp.gluegen:gluegen-rt:$gluegenVersion:natives-$target")
            add(config, "org.jogamp.jogl:jogl-all:$joglVersion:natives-$target")
        }
    }
}

tasks.create("processrtRtArtifacts") {
    inputs.files(configurations["rtArtifacts"])
    doLast {
        for (file in configurations["rtArtifacts"]) {
            copy {
                from(zipTree(file))
                into("libs")
            }
        }
    }
}

kotlin.sourceSets {
    jvmMain {
        resources.srcDir("libs")
    }
}

for (target in listOf("linuxX64")) {
    (kotlin.targets[target].compilations["main"] as KotlinNativeCompilation).apply {
        cinterops.apply {
            maybeCreate("GL").apply {
            }
        }
    }
}


tasks.getByName("jvmProcessResources").dependsOn("processrtRtArtifacts")
