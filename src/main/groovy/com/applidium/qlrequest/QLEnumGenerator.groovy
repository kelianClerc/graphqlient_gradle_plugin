package com.applidium.qlrequest

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec

import javax.lang.model.element.Modifier


public class QLEnumGenerator {
    static TypeSpec generateEnum(String fileName, String fileContent) {

        ClassName qlenumSuper = ClassName.get(QLClassGenerator.PACKAGE + ".model", "QLEnum");

        TypeSpec.Builder enumClass = TypeSpec.enumBuilder(fileName + "QLEnum")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(qlenumSuper);

        String[] enumFields = splitFileContent(fileContent)

        for(String field : enumFields) {
            enumClass.addEnumConstant(field, TypeSpec.anonymousClassBuilder("\$S", field).build())
        }

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addParameter(String.class, "id")
                .addStatement("this.\$N = \$N", "id", "id")

        MethodSpec.Builder toString = MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return \$N", "id")
                .returns(String.class)
                .addAnnotation(Override.class)

        enumClass.addField(String.class, "id", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(constructor.build())
                .addMethod(toString.build())
        return enumClass.build();
    }

    private static String[] splitFileContent(String fileContent) {
        fileContent = fileContent.replace(" ", "");
        fileContent = fileContent.replace(System.getProperty("line.separator"), "");
        String[] enumFields = fileContent.split(",");
        enumFields
    }
}
