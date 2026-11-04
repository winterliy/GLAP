import java.util.Locale

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

if (!file(".git").exists()) {
    val errorText = """
        
        =====================[ ERROR ]=====================
         The GLAP project directory is not a properly cloned Git repository.
         
         In order to build GLAP from source you must clone
         the GLAP repository using Git, not download a code
         zip from GitHub.
         
         Built GLAP jars are available on the GitHub releases page:
         https://github.com/winterliy/GLAP/releases
         
         See README.md
         for further information on building and modifying GLAP.
        ===================================================
    """.trimIndent()
    error(errorText)
}

rootProject.name = "glap"
for (name in listOf("glap-api", "glap-server", "glap-checkstyle", "kode")) {
    val projName = name.lowercase(Locale.ENGLISH)
    include(projName)
    findProject(":$projName")!!.projectDir = file(name)
}

optionalInclude("test-plugin")

fun optionalInclude(name: String, op: (ProjectDescriptor.() -> Unit)? = null) {
    val settingsFile = file("$name.settings.gradle.kts")
    if (settingsFile.exists()) {
        apply(from = settingsFile)
        findProject(":$name")?.let { op?.invoke(it) }
    } else {
        settingsFile.writeText(
            """
            // Uncomment to enable the '$name' project
            // include(":$name")

            """.trimIndent()
        )
    }
}

gradle.lifecycle.beforeProject {
    val mcVersion = providers.gradleProperty("mcVersion").get().trim()
    val glapChannel = providers.gradleProperty("channel").get().trim()
    val glapBuildNumber = providers.environmentVariable("BUILD_NUMBER").orNull?.trim()?.toInt()
    val versionString = if (glapBuildNumber == null) {
        "$mcVersion.local-SNAPSHOT"
    } else {
        "$mcVersion.build.$glapBuildNumber-${glapChannel.lowercase()}"
    }
    version = versionString
}
