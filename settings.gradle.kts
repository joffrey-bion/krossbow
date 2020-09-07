import com.gradle.scan.plugin.BuildScanExtension

pluginManagement {
    repositories {
        gradlePluginPortal()
        // for Dokka 1.4.0 (not yet in Gradle plugin portal)
        // https://kotlin.github.io/dokka/1.4.0/user_guide/gradle/usage/
        jcenter()
    }
}

plugins {
    id("com.gradle.enterprise") version "3.2.1"
}

rootProject.name = "krossbow"

include("krossbow-stomp-core")
include("krossbow-stomp-kxserialization")
include("krossbow-stomp-jackson")
include("krossbow-websocket-core")
include("krossbow-websocket-okhttp")
include("krossbow-websocket-sockjs")
include("krossbow-websocket-spring")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"

        val isCIBuild = !System.getenv("CI").isNullOrEmpty()
        publishOnFailureIf(isCIBuild)
        tag(if (isCIBuild) "CI" else "local")

        val isTravisBuild = !System.getenv("TRAVIS").isNullOrEmpty()
        if (isTravisBuild) {
            addTravisData()
        }

        val isGithubActionsBuild = !System.getenv("GITHUB_ACTIONS").isNullOrEmpty()
        if (isGithubActionsBuild) {
            addGithubActionsData()
        }
    }
}

fun BuildScanExtension.addTravisData() {
    value("Build number", System.getenv("TRAVIS_BUILD_NUMBER"))
    value("Branch", System.getenv("TRAVIS_BRANCH"))
    value("Commit", System.getenv("TRAVIS_COMMIT"))
    value("Commit msg", System.getenv("TRAVIS_COMMIT_MESSAGE"))
    value("JDK", System.getenv("TRAVIS_JDK_VERSION"))

    val tag = System.getenv("TRAVIS_TAG")
    val isTagBuild = !tag.isNullOrEmpty()
    if (isTagBuild) {
        tag("tag")
        value("Tag", tag)
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
