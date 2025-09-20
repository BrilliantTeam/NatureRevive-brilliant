plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev")
}


group = "engineer.skyouo.plugins.naturerevive.spigot.nms"
version = project.rootProject.version

dependencies {
    paperweight.paperDevBundle("1.19.1-R0.1-SNAPSHOT")

    compileOnly("org.spigotmc:spigot-api:1.19.1-R0.1-SNAPSHOT")
    compileOnly(project(":naturerevive-common"))
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }

        sourceCompatibility = JavaVersion.VERSION_21
        java.targetCompatibility = JavaVersion.VERSION_21
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
}