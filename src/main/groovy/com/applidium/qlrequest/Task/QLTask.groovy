package com.applidium.qlrequest.Task

import com.applidium.qlrequest.QLClassGenerator
import com.squareup.javapoet.TypeSpec
import groovy.io.FileType
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

public class QLTask extends DefaultTask {

    @Input
    def packageName

    @Input
    def variantDirName

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
        project.file("${project.buildDir}/generated/source/graphql/${variantDirName}")
    }

    @OutputFile
    File outputFile(String name) {
        name = name.capitalize();
        project.file("${outputDir().absolutePath}/${packageName.replace('.', '/')}/${name}.java")
    }

    @TaskAction
    def taskAction() {
        println "Hello";
        def settingMaps = settingFiles().collect { computeQuery(it) }
    }

    public void computeQuery(File f) {
        if(!isFileSupported(f.getName())) {
            return;
        }
        if (f) {
            println f;
            QLClassGenerator classGenerator = new QLClassGenerator();
            def qlquery = classGenerator.generateSource(f, packageName)

            qlquery.collect() {
                TypeSpec source = it
                def outputFile = outputFile(source.name)
                if (!outputFile.isFile()) {
                    outputFile.delete()
                    outputFile.parentFile.mkdirs()
                }

                outputFile.text = "package ${packageName};\n" + source.toString()
            }
        }
    }

    boolean isFileSupported(String fileName) {
        return fileName.endsWith(".graphql") || fileName.endsWith(".qlenum");
    }

    public void createRequest() {



    }

    public void createResponse() {

    }
}
