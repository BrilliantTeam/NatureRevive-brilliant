plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev")
}


group = "engineer.skyouo.plugins.naturerevive.spigot.nms"
version = project.rootProject.version

// Minecraft 26.x ships only Mojang mappings; the dev bundle provides no reobf
// mappings, so the plain (Mojang-mapped) jar is the production artifact.

dependencies {
    paperweight.paperDevBundle("26.2.build.22-alpha")

    compileOnly("io.papermc.paper:paper-api:26.2.build.22-alpha")
    compileOnly(project(":naturerevive-common"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

configurations.named("compileClasspath") {
    attributes {
        attribute(org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
    }
}

tasks {
    assemble {
        dependsOn(jar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()

        // Kept at 21 (not 25) so the class file version is readable by the plugin
        // remapper on older servers; the 26.2 NMS API is still available at runtime.
        options.release.set(21)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}
