package com.applidium.qlrequest

import com.applidium.qlrequest.Query.QLQuery
import com.applidium.qlrequest.Query.QLType
import com.applidium.qlrequest.Query.QLVariablesElement
import com.applidium.qlrequest.Tree.QLElement
import com.applidium.qlrequest.Tree.QLLeaf
import com.applidium.qlrequest.Tree.QLNode
import com.applidium.qlrequest.Tree.QLParser
import com.applidium.qlrequest.model.QLModel
import com.squareup.javapoet.*

import javax.lang.model.element.Modifier

class QLClassGenerator {

    static def generateSource(File file, String packageName) {
        String fileContent = file.text;
        QLParser parser = new QLParser();
        parser.setToParse(fileContent);
        QLQuery qlQuery = parser.buildQuery();
        def files = []

        String className;
        if (qlQuery.name == null || qlQuery.name.equals("")) {
            className = removeFileExtension(file.getName()).capitalize();
        } else {
            className = qlQuery.name.capitalize();
        }

        files << generateQuery(qlQuery, className)
        files << generateResponse(qlQuery, className, packageName)
        return files;
    }

    static String removeFileExtension(String text) {
        return text[0..<text.lastIndexOf('.')];
    }

    static TypeSpec generateQuery(QLQuery qlQuery, String className) {
        MethodSpec.Builder constructor = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder emptyConstructor = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        List<FieldSpec> fields = new ArrayList<>();
        List<MethodSpec> getterAndSetter = new ArrayList<>();
        List<FieldSpec> mandatoryFields = new ArrayList<>();

        computeParams(qlQuery, fields, constructor, getterAndSetter, mandatoryFields)
        computeVarsMap(fields, getterAndSetter);

        FieldSpec.Builder queryField = FieldSpec
                .builder(String.class, "query", Modifier.PRIVATE, Modifier.FINAL);
        queryField.initializer("\$S", qlQuery.printQuery())

        TypeSpec.Builder query = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(queryField.build())
                .addFields(fields)
                .addMethod(constructor.build())
                .addMethod(getQuery(mandatoryFields))
                .addMethods(getterAndSetter);

        if (fields.size() > 0) {
            query.addMethod(emptyConstructor.build());
        }

        return query.build();
    }

    private
    static void computeParams(
            QLQuery qlQuery,
            ArrayList<FieldSpec> fields,
            MethodSpec.Builder constructor,
            ArrayList<MethodSpec> getterAndSetter,
            ArrayList<FieldSpec> mandatoryFields
    ) {
        for (QLVariablesElement element : qlQuery.getParameters().getParams()) {
            //todo kelian(12/06/17) exception if arg named query
            ParameterSpec param = ParameterSpec
                    .builder(getType(element.type), element.name)
                    .build();
            FieldSpec field = FieldSpec
                    .builder(getType(element.type), element.name, Modifier.PRIVATE)
                    .build();
            if (element.mandatory) {
                mandatoryFields.add(field);
            }

            fields.add(field)
            constructor.addParameter(param);
            constructor.addStatement("this.\$N = \$N", param.name, param.name);
            getterAndSetter.add(generateGetter(param));
            getterAndSetter.add(generateSetter(param));
        }
    }

    static TypeName getType(QLType qlType) {
        switch (qlType) {
            case QLType.INT:
                return TypeName.get(Integer.class);
            case QLType.FLOAT:
                return TypeName.get(Float.class);
            case QLType.BOOLEAN:
                return TypeName.get(Boolean.class);
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

    static MethodSpec generateSetter(ParameterSpec param) {
        MethodSpec.Builder setter = MethodSpec.methodBuilder(param.name)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(param)
                .returns(param.type);

        setter.addStatement("this.\$N = \$N", param.name, param.name);
        return setter.addStatement("return \$N", param.name).build();
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
            getVars.beginControlFlow("if (\$N != null)", fieldSpec.name);
            getVars.addStatement("result.put(\$S, \$N)", fieldSpec.name, fieldSpec.name);
            getVars.endControlFlow();
        }

        getVars.addStatement("return result");
        methodSpecs.add(getVars.build());
    }

    private static MethodSpec getQuery(List<FieldSpec> mandatoryFields) {
        String packageName = "com.applidium.qlrequest.exceptions"
        ClassName exception = ClassName.get(packageName, "QLException");

        MethodSpec.Builder statement = MethodSpec.methodBuilder("query").
                addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addException(exception);

        for (FieldSpec field : mandatoryFields) {
            statement.beginControlFlow("if(\$N == null)", field.name);
            String throwMessage = "throw new \$T(\"Mandatory field : \$N is not set\")"
            statement.addStatement(throwMessage, exception, field.name);
            statement.endControlFlow();
        }
        statement.addStatement("return \$N", "query");
        return statement.build()
    }

    static TypeSpec generateResponse(QLQuery qlQuery, String fileName, String packageName) {
        MethodSpec.Builder constructor = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder emptyConstructor = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        createModels(qlQuery, fileName, packageName)
    }

    static TypeSpec createModels(QLQuery qlQuery, String className, String packageName) {

        TypeSpec.Builder queryRespose = TypeSpec.classBuilder(className + "Response").addModifiers(Modifier.PUBLIC);

        for (QLNode root : qlQuery.getQueryFields()) {
            horizontalTreeReed(root, queryRespose, packageName)
        }

        return queryRespose.build();
    }

    static void horizontalTreeReed(QLElement qlElement, TypeSpec.Builder parent, String packageName) {
        String packageNameChild = packageName + "." + parent.build().name;
        if (qlElement instanceof QLNode) {
            TypeSpec.Builder model = TypeSpec.classBuilder(qlElement.name.capitalize())
                    .superclass(QLModel.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
            generateFieldSetterGetter(parent, builderType(packageNameChild, qlElement.name.capitalize(), qlElement.isList()), qlElement.getName());
            for (QLElement child : qlElement.getChildren()) {
                horizontalTreeReed(child, model, packageNameChild);
            }
            parent.addType(model.build());
        } else {
            QLLeaf leaf = (QLLeaf) qlElement;
            generateFieldSetterGetter(parent, getType(leaf.getType()), leaf.getName());
        }
    }

    static TypeName builderType(String packageName, final String modelName, boolean isList) {
        final ClassName raw = rawBuilderType(modelName, packageName);
        if (!isList) {
            return raw;
        }
        ClassName list = ClassName.get("java.util", "List");
        return ParameterizedTypeName.get(list, raw);
    }

    static ClassName rawBuilderType(final String d, String packageName) {
        return ClassName.get(packageName, d);
    }

    private static void generateFieldSetterGetter(TypeSpec.Builder parent, TypeName type, String name) {
        parent.addField(FieldSpec.builder(type, name, Modifier.PRIVATE).build());
        ParameterSpec param = ParameterSpec.builder(type, name).build()
        parent.addMethod(generateGetter(param));
        parent.addMethod(generateSetter(param));
    }
}
