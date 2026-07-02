plugins {
    id("org.openstreetmap.josm").version("0.8.2")
}

import org.gradle.api.tasks.JavaExec

version = "0.0.1"

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
    }
}

dependencies {
    packIntoJar("org.apache.commons:commons-imaging:1.0.0-alpha5")
}

tasks.named<JavaExec>("runJosm") {
    jvmArgs(josmJvmArgs)
}

tasks.named<JavaExec>("debugJosm") {
    jvmArgs(josmJvmArgs)
}
