package org.hildan.krossbow.gradle

import org.gradle.api.Project

val Project.github: GitHubInfo
    get() = GitHubInfo(
        user = property("githubUser") as String,
        repoName = rootProject.name,
    )

data class GitHubInfo(
    val user: String,
    val repoName: String,
    val repoSlug: String = "$user/$repoName",
    val repoUrl: String = "https://github.com/$repoSlug",
)
