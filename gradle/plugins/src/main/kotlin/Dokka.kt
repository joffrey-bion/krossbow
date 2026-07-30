import org.gradle.api.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.*

fun Project.dokkaExternalDocLink(name: String, docsUrl: String, packageListUrl: String? = null) {
    require(docsUrl.endsWith("/")) {
        "the docs URL to end with '/' because Dokka builds other URLs from it"
    }
    extensions.configure<DokkaExtension> {
        dokkaSourceSets {
            configureEach {
                externalDocumentationLinks.register(name) {
                    url(docsUrl)
                    if (packageListUrl != null) {
                        packageListUrl(packageListUrl)
                    }
                }
            }
        }
    }
}
