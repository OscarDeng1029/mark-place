pluginManagement {
    repositories {
        if (System.getenv("ATLAS_MAVEN_LOCAL") == "1") {
            maven("http://127.0.0.1:37661/google/") { isAllowInsecureProtocol = true }
            maven("http://127.0.0.1:37661/central/") { isAllowInsecureProtocol = true }
            maven("http://127.0.0.1:37661/plugins/") { isAllowInsecureProtocol = true }
        } else {
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
}
dependencyResolutionManagement {
    repositories {
        if (System.getenv("ATLAS_MAVEN_LOCAL") == "1") {
            maven("http://127.0.0.1:37661/google/") { isAllowInsecureProtocol = true }
            maven("http://127.0.0.1:37661/central/") { isAllowInsecureProtocol = true }
        } else {
            google()
            mavenCentral()
        }
    }
}
rootProject.name = "MyLittleAtlas"
include(":composeApp")
