package com.applidium.qlrequest

import com.applidium.qlrequest.Query.QLQuery
import com.applidium.qlrequest.Query.QLType
import com.applidium.qlrequest.Query.QLVariablesElement
import com.applidium.qlrequest.Tree.QLParser
import com.squareup.javapoet.*

import javax.lang.model.element.Modifier

class QLClassGenerator {

    static def generateSource(File file) {
        String fileContent = file.text;
        QLParser parser = new QLParser();
        parser.setToParse(fileContent);
        QLQuery qlQuery = parser.buildQuery();
        def files = []
        files << generateQuery(qlQuery, file.name)
        //files << generateResponse(qlQuery, file.name)
        return files;

    }

    static TypeSpec generateResponse(QLQuery qlQuery, String fileName) {
        return "";
    }

    static TypeSpec generateQuery(QLQuery qlQuery, String fileName) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder emptyConstructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        List<FieldSpec> fields = new ArrayList<>();
        List<MethodSpec> getterAndSetter = new ArrayList<>();

        computeParams(qlQuery, fields, constructor, getterAndSetter)
        computeVarsMap(fields, getterAndSetter);

        TypeSpec.Builder query = TypeSpec.classBuilder(qlQuery.name == null || qlQuery.name.equals("") ? fileName : qlQuery.name)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(FieldSpec.builder(String.class, "query", Modifier.PRIVATE, Modifier.FINAL).initializer("\$S", qlQuery.printQuery()).build())
            .addFields(fields)
            .addMethod(constructor.build())
            .addMethod(MethodSpec.methodBuilder("query").addModifiers(Modifier.PUBLIC).returns(String.class).addStatement("return \$N", "query").build())
            .addMethods(getterAndSetter);

        if (fields.size() > 0) {
            query.addMethod(emptyConstructor.build());
        }

        return query.build();
    }

    static void computeVarsMap(ArrayList<FieldSpec> fieldSpecs, ArrayList<MethodSpec> methodSpecs) {

        ClassName map = ClassName.get("java.util", "Map");
        ClassName hashmap = ClassName.get("java.util", "HashMap");
        ClassName key = ClassName.get("java.lang", "String");
        ClassName value = ClassName.get("java.lang", "Object");
        TypeName mapVars = ParameterizedTypeName.get(map, key, value);

        MethodSpec.Builder getVars = MethodSpec.methodBuilder("getVariables");
        getVars.addModifiers(Modifier.PUBLIC);
        getVars.returns(Map.class);
        getVars.addStatement("\$T result = new \$T<>()", mapVars, hashmap)
        for (FieldSpec fieldSpec : fieldSpecs) {
            getVars.addStatement("result.put(\$S, \$N)", fieldSpec.name, fieldSpec.name);
        }

        getVars.addStatement("return result");
        methodSpecs.add(getVars.build());
    }

    private
    static void computeParams(
            QLQuery qlQuery,
            ArrayList<FieldSpec> fields,
            MethodSpec.Builder constructor,
            ArrayList<MethodSpec> getterAndSetter
    ) {
        for (QLVariablesElement element : qlQuery.getParameters().getParams()) {
            //todo kelian(12/06/17) exception if arg name query
            ParameterSpec param = ParameterSpec.builder(getType(element.type), element.name).build();
            fields.add(FieldSpec.builder(getType(element.type), element.name, Modifier.PRIVATE).build())
            constructor.addParameter(param);
            constructor.addStatement("this.\$N = \$N", param.name, param.name);
            getterAndSetter.add(generateGetter(param));
            getterAndSetter.add(generateSetter(param));
        }
    }

    static TypeName getType(QLType qlType) {
        switch (qlType) {
            case QLType.INT:
                return TypeName.get(int.class);
            case QLType.FLOAT:
                return TypeName.get(float.class);
            case QLType.BOOLEAN:
                return TypeName.get(boolean.class);
            case QLType.ID:
            case QLType.STRING:
            default:
                return TypeName.get(String.class);
        }
    }

    static MethodSpec generateGetter(ParameterSpec param) {
        MethodSpec.Builder getter = MethodSpec.methodBuilder("get" + param.name.capitalize())
                .addModifiers(Modifier.PUBLIC)
                .returns(param.type);

        if (isCollection(param.type)) {

            ClassName arrayList = ClassName.get("java.util", "ArrayList");

            getter.beginControlFlow("if (this.\$N == null)", param.name)
                    .addStatement("this.\$N = new \$T()", param.name, arrayList)
                    .endControlFlow();
        }
        getter.addStatement("return \$N", param.name);

        return getter.build();
    }

    static boolean isCollection(TypeName typeName) {
        final String returnType = typeName.toString();
        return returnType.startsWith("java.util.List<");
    }

    static def generateSetter(ParameterSpec param) {
        MethodSpec.Builder setter = MethodSpec.methodBuilder(param.name)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(param)
                .returns(param.type);

        setter.addStatement("this.\$N = \$N", param.name, param.name);
        return setter.addStatement("return \$N", param.name).build();
    }

    static def generateQueryFields() {

    }
}
