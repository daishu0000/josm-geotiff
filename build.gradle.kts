plugins {
    id("org.openstreetmap.josm").version("0.8.2")
}

import org.gradle.api.tasks.JavaExec
import org.openstreetmap.josm.gradle.plugin.task.github.PublishToGithubReleaseTask

version = "0.0.3"

val releaseJarName = "josmtiff.jar"
val releaseJarPath = layout.buildDirectory.file("dist/$releaseJarName")

val josmJvmArgs = listOf(
    "--add-exports=java.base/sun.security.action=ALL-UNNAMED",
    "--add-exports=java.desktop/com.sun.imageio.plugins.jpeg=ALL-UNNAMED",
    "--add-exports=java.desktop/com.sun.imageio.spi=ALL-UNNAMED",
)

repositories {
    maven {
        url = uri("https://maven.aliyun.com/repository/public")
    }
    mavenCentral()
}

josm {
    pluginName = "josmtiff"
    josmCompileVersion = "19555"
    manifest {
        description = "TIFF image support for JOSM"
        mainClass = "org.openstreetmap.josm.plugins.josmtiff.JosmTiffPlugin"
        minJosmVersion = "19555"
        author = "smallCat"
    }
    github {
        repositoryOwner = "daishu0000"
        repositoryName = "josm-geotiff"
        targetCommitish = "main"
    }
}

dependencies {
    val twelveMonkeysVersion = "3.12.0"
    packIntoJar("org.apache.commons:commons-imaging:1.0.0-alpha5")
    packIntoJar("com.twelvemonkeys.imageio:imageio-tiff:$twelveMonkeysVersion")
    packIntoJar("com.twelvemonkeys.imageio:imageio-metadata:$twelveMonkeysVersion")
    packIntoJar("com.twelvemonkeys.imageio:imageio-core:$twelveMonkeysVersion")
    packIntoJar("com.twelvemonkeys.common:common-lang:$twelveMonkeysVersion")
    packIntoJar("com.twelvemonkeys.common:common-io:$twelveMonkeysVersion")
    packIntoJar("com.twelvemonkeys.common:common-image:$twelveMonkeysVersion")
}

tasks.named<JavaExec>("runJosm") {
    jvmArgs(josmJvmArgs)
}

tasks.named<JavaExec>("debugJosm") {
    jvmArgs(josmJvmArgs)
}

tasks.named<PublishToGithubReleaseTask>("publishToGithubRelease") {
    dependsOn("dist")
    mustRunAfter("createGithubRelease")
    localJarPath = releaseJarPath.get().asFile.absolutePath
    remoteJarName = releaseJarName
}

tasks.register("release") {
    group = "release"
    description = "Build dist JAR, create GitHub release, and upload $releaseJarName"
    dependsOn("createGithubRelease", "publishToGithubRelease")
}
