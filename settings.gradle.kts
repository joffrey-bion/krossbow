import com.gradle.scan.plugin.BuildScanExtension

plugins {
    id("com.gradle.enterprise") version "3.2.1"
    id("com.louiscad.complete-kotlin") version "1.0.0" // for autocomplete of Apple libraries on non-macOS systems
    kotlin("jvm") version "1.6.10" apply false // workaround https://github.com/LouisCAD/CompleteKotlin/issues/7
}

rootProject.name = "krossbow"

include("krossbow-stomp-core")
include("krossbow-stomp-kxserialization")
include("krossbow-stomp-jackson")
include("krossbow-websocket-core")
include("krossbow-websocket-ktor")
include("krossbow-websocket-okhttp")
include("krossbow-websocket-sockjs")
include("krossbow-websocket-spring")
include("krossbow-websocket-test")
include("autobahn-tests")

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"

        val isCIBuild = !System.getenv("CI").isNullOrEmpty()
        publishOnFailureIf(isCIBuild)
        tag(if (isCIBuild) "CI" else "local")

        val isGithubActionsBuild = !System.getenv("GITHUB_ACTIONS").isNullOrEmpty()
        if (isGithubActionsBuild) {
            addGithubActionsData()
        }
    }
}

fun BuildScanExtension.addGithubActionsData() {
    value("GitHub Event", System.getenv("GITHUB_EVENT_NAME"))
    value("GitHub Workflow", System.getenv("GITHUB_WORKFLOW"))
    value("GitHub Run ID", System.getenv("GITHUB_RUN_ID"))
    value("GitHub Run number", System.getenv("GITHUB_RUN_NUMBER"))
    value("Commit", System.getenv("GITHUB_SHA"))

    val ref = System.getenv("GITHUB_REF") ?: ""
    val isTagBuild = ref.startsWith("refs/tags/")
    if (isTagBuild) {
        tag("tag")
        value("Tag", ref.removePrefix("refs/tags/"))
    } else {
        value("Branch", ref.removePrefix("refs/heads/"))
    }
}
