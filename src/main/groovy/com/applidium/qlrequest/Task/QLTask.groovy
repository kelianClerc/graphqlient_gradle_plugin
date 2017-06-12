package com.applidium.qlrequest.Task

import com.applidium.qlrequest.QLClassGenerator
import groovy.io.FileType
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

public class QLTask extends DefaultTask {

    @Input
    def packageName

    @InputFiles
    def settingFiles() {
        def files = []
        def dir = new File("graphql")
        dir.eachFileRecurse (FileType.FILES) {
            file -> files << file
        }
        project.file(files)
    }

    @OutputDirectory
    File outputDir() {
        project.file("${project.buildDir}/generated/source/graphql")
    }

    @OutputFile
    File outputFile() {
        project.file("${outputDir().absolutePath}/${packageName.replace('.', '/')}/Settings.java")
    }

    @TaskAction
    def taskAction() {
        def settingMaps = settingFiles().collect { computeQuery(it) }
    }

    static public void computeQuery(String f) {
        if (f) {
            def source = QLClassGenerator.build(f).generateSource()
            def outputFile = outputFile()
            if (!outputFile.isFile()) {
                outputFile.delete()
                outputFile.parentFile.mkdirs()
            }

            outputFile.text = "package ${packageName};\n" + source
        }
    }

    static public void createRequest(String ) {

    }

    static public void createResponse() {

    }
}
