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
        return files
    }

    @OutputDirectory
    File outputDir() {
        project.file("${project.buildDir}/generated/source/graphql")
    }

    @OutputFile
    File outputRequestFile(String name) {
        name = name.capitalize();
        project.file("${outputDir().absolutePath}/${packageName.replace('.', '/')}/${name}Request.java")
    }

    @OutputFile
    File outputResponseFile(String name) {
        name = name.capitalize();
        project.file("${outputDir().absolutePath}/${packageName.replace('.', '/')}/${name}.java")
    }

    @TaskAction
    def taskAction() {
        println "Hello";
        def settingMaps = settingFiles().collect { computeQuery(it) }
    }

    public void computeQuery(File f) {
        if (f) {
            println f;
            def source = QLClassGenerator.generateSource(f).get(0)
            def outputFile = outputRequestFile(f.getName())
            if (!outputFile.isFile()) {
                outputFile.delete()
                outputFile.parentFile.mkdirs()
            }

            outputFile.text = "package ${packageName};\n" + source.toString()
        }
    }

    public void createRequest() {



    }

    public void createResponse() {

    }
}
