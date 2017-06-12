package com.applidium.qlrequest

import com.applidium.qlrequest.Task.QLTask
import org.gradle.api.Plugin
import org.gradle.api.Project


class QLPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.tasks.create(
                name: "generateSettings",
                type: QLTask) {
            packageName    "com.applidium.qlrequest"
        }
    }
}
