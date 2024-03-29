import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.project
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.version
import no.elhub.devxp.build.configuration.AutoRelease
import no.elhub.devxp.build.configuration.CodeReview
import no.elhub.devxp.build.configuration.ProjectType
import no.elhub.devxp.build.configuration.SonarScan
import no.elhub.devxp.build.configuration.UnitTest

version = "2022.10"

project {
    val projectName = "devxp-elhub-teamcity-plugin"
    val projectId = "no.elhub.devxp:$projectName"
    val projectType = ProjectType.GRADLE
    val artifactoryRepository = "elhub-mvn-release-local"

    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val unitTest = UnitTest(
        UnitTest.Config(
            vcsRoot = DslContext.settingsRoot,
            type = projectType,
            generateAllureReport = false,
        )
    )

    val sonarScanConfig = SonarScan.Config(
        vcsRoot = DslContext.settingsRoot,
        type = projectType,
        sonarId = projectId,
        sonarProjectSources = "elhub-teamcity-agent/src,elhub-teamcity-common/src,elhub-teamcity-server/src",
        sonarProjectTests = null,
        sonarProjectBinaries = "elhub-teamcity-agent/build/classes,elhub-teamcity-common/build/classes,elhub-teamcity-server/build/classes",
        additionalParams = listOf(
            "-Dsonar.dependencyCheck.jsonReportPath=build/reports/dependency-check-report.json",
            "-Dsonar.dependencyCheck.htmlReportPath=build/reports/dependency-check-report.html",
        )
    )

    val sonarScan = SonarScan(sonarScanConfig) {
        dependencies {
            snapshot(unitTest) { }
        }
    }

    val release = AutoRelease(
        AutoRelease.Config(
            vcsRoot = DslContext.settingsRoot,
            type = projectType,
            repository = artifactoryRepository
        )
    ) {
        triggers {
            vcs {
                branchFilter = "+:<default>"
                quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
            }
        }

        dependencies {
            snapshot(sonarScan) { }
        }
    }

    listOf(unitTest, sonarScan, release).forEach { buildType(it) }

    buildType(
        CodeReview(
            CodeReview.Config(
                vcsRoot = DslContext.settingsRoot,
                type = projectType,
                sonarScanConfig = sonarScanConfig,
            )
        )
    )

}
