import org.gradle.api.Project
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

fun Project.dokkaExternalDocLink(docsUrl: String, packageListUrl: String? = null) {
    require(docsUrl.endsWith("/")) {
        "the docs URL to end with '/' because Dokka builds other URLs from it"
    }
    tasks.withType<AbstractDokkaLeafTask>().configureEach {
        dokkaSourceSets {
            configureEach {
                externalDocumentationLink(docsUrl, packageListUrl)
            }
        }
    }
}
