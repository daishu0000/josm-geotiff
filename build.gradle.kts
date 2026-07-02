plugins {
    id("org.openstreetmap.josm").version("0.8.2")
}

version = "0.0.1"

josm {
    pluginName = "josmtiff"
    josmCompileVersion = "19555"
    manifest {
        description = "TIFF image support for JOSM"
        mainClass = "org.openstreetmap.josm.plugins.josmtiff.JosmTiffPlugin"
        minJosmVersion = "19555"
    }
}
