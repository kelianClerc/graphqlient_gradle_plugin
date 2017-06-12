package com.applidium.qlrequest.Task

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
        return files
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
        println "Hello";
        def settingMaps = settingFiles().collect { computeQuery(it) }
    }

    public void computeQuery(File f) {
        if (f) {
            println f;
            /*def source = QLClassGenerator.build(f).generateSource()
            def outputFile = outputFile()
            if (!outputFile.isFile()) {
                outputFile.delete()
                outputFile.parentFile.mkdirs()
            }

            outputFile.text = "package ${packageName};\n" + source*/
        }
    }

    public void createRequest(String ) {

    }

    public void createResponse() {

    }
}
