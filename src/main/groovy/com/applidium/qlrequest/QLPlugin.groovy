package com.applidium.qlrequest

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.BaseVariant
import com.applidium.qlrequest.Task.QLTask
import org.gradle.api.Plugin
import org.gradle.api.Project


class QLPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.withId('com.android.application') {
            def android = project.extensions.getByType(AppExtension)
            android.applicationVariants.all() { BaseVariant variant ->

                def task = project.tasks.create(
                        name: "generate${variant.name.capitalize()}GraphQLModels",
                        type: QLTask) {
                    packageName variant.generateBuildConfig.buildConfigPackageName
                    variantDirName variant.dirName
                }

                variant.registerJavaGeneratingTask(task, task.outputDir())
                android.sourceSets[variant.name].java.srcDirs += [task.outputDir()]
            }
        }
    }
}
