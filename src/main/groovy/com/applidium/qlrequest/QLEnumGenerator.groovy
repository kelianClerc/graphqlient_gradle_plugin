package com.applidium.qlrequest

import com.squareup.javapoet.TypeSpec

import javax.lang.model.element.Modifier


public class QLEnumGenerator {
    static TypeSpec generateEnum(String fileName, String fileContent) {
        TypeSpec.Builder enumClass = TypeSpec.enumBuilder(fileName + "QLEnum");
        enumClass.addModifiers(Modifier.PUBLIC)

        String[] enumFields = splitFileContent(fileContent)

        for(String field : enumFields) {
            enumClass.addEnumConstant(field);
        }
        return enumClass.build();
    }

    private static String[] splitFileContent(String fileContent) {
        fileContent = fileContent.replace(" ", "");
        fileContent = fileContent.replace(System.getProperty("line.separator"), "");
        String[] enumFields = fileContent.split(",");
        enumFields
    }
}
