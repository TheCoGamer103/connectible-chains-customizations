import java.util.zip.ZipFile

plugins {
    // Minecraft 26.x ships unobfuscated, so this is the non-remapping Loom plugin (no mappings needed).
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT"
}

version = property("mod_version") as String
group = "chainitemhangings"
base.archivesName = "connectible-chains-customizations"

repositories {
    mavenCentral()
    maven("https://maven.isxander.dev/releases") {
        name = "Xander Maven (YACL)"
        content { includeGroupAndSubgroups("dev.isxander") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    // Only YACL's own classes are needed to compile; the game already has YACL and its libraries.
    compileOnly("dev.isxander:yet-another-config-lib:${property("yacl_version")}") { isTransitive = false }

    // Put the Reconnectible Chains Fabric 26.3 jar from your modpack into libs/ (any file name).
    // It's only needed to compile; it is NOT bundled into this mod.
    compileOnly(fileTree("libs") { include("*.jar") })
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "loader_version" to project.property("loader_version"),
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

// Helper: lists the screen-related methods Minecraft 26.3 really has. Run: gradlew.bat probeApi -q
tasks.register("probeApi") {
    doLast {
        val classes = listOf("net.minecraft.client.Minecraft", "net.minecraft.client.gui.Gui")
        val jar = sourceSets["main"].compileClasspath.files.first { f ->
            f.isFile && f.name.endsWith(".jar") &&
                ZipFile(f).use { it.getEntry("net/minecraft/client/Minecraft.class") != null }
        }
        val javaHome = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }.get().metadata.installationPath.asFile
        val javap = File(javaHome, if (System.getProperty("os.name").startsWith("Windows")) "bin/javap.exe" else "bin/javap")
        for (cls in classes) {
            println("== $cls")
            val p = ProcessBuilder(javap.absolutePath, "-public", "-cp", jar.absolutePath, cls).redirectErrorStream(true).start()
            p.inputStream.bufferedReader().readLines()
                .filter { it.contains("creen", ignoreCase = true) || it.contains("chat", ignoreCase = true) || it.contains("message", ignoreCase = true) }
                .forEach { println(it.trim()) }
            p.waitFor()
        }
    }
}
