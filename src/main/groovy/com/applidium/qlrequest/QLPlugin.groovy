package com.applidium.qlrequest

import org.gradle.api.Plugin
import org.gradle.api.Project


class QLPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.task("testTaskKelian") << {
            println "Hello"
        }
    }
}
