rootProject.name = "NatureReviveRewrite"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("naturerevive-common")

listOf("1_17", "1_18", "1_18_2", "1_19", "1_19_1", "1_19_3", "1_19_4", "1_20_1", "1_20_2", "1_20_4", "1_20_6",
    "1_21", "1_21_3", "1_21_4", "1_21_5", "1_21_7", "1_21_9", "1_21_11", "compat").forEach {
    include(":naturerevive-spigot:nms:nms-$it")

    findProject(":naturerevive-spigot:nms:nms-$it")?.name = "nms-$it"
}

include("naturerevive-spigot")
