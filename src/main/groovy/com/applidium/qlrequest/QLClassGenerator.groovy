package com.applidium.qlrequest

import com.applidium.qlrequest.Query.QLFragment
import com.applidium.qlrequest.Query.QLQuery
import com.applidium.qlrequest.Query.QLType
import com.applidium.qlrequest.Query.QLVariablesElement
import com.applidium.qlrequest.Tree.*
import com.squareup.javapoet.*

import javax.lang.model.element.Modifier

class QLClassGenerator {

    private final String PACKAGE = "com.applidium.graphqlient";
    QLQuery qlQuery;

    def generateSource(File file, String packageName) {
        String fileContent = file.text;
        QLParser parser = new QLParser();
        parser.setToParse(fileContent);
        this.qlQuery = parser.buildQuery();
        def files = []

        String className;
        if (qlQuery.name == null || qlQuery.name.equals("")) {
            className = removeFileExtension(file.getName()).capitalize();
        } else {
            className = qlQuery.name.capitalize();
        }

        files << generateQuery(className, packageName)
        files << generateResponse(className, packageName)
        return files;
    }

    String removeFileExtension(String text) {
        return text[0..<text.lastIndexOf('.')];
    }

    TypeSpec generateQuery(String className, String packageName) {
        MethodSpec.Builder constructor = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder emptyConstructor = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        List<FieldSpec> fields = new ArrayList<>();
        List<MethodSpec> getterAndSetter = new ArrayList<>();
        List<FieldSpec> mandatoryFields = new ArrayList<>();

        computeParams(fields, constructor, getterAndSetter, mandatoryFields)
        computeVarsMap(fields, getterAndSetter);
        boolean areConstructorParam = fields.size() > 0;
        addTarget(fields, getterAndSetter, packageName, className);
        addQuery(fields, getterAndSetter, mandatoryFields);

        ClassName qlRequest = ClassName.get(PACKAGE, "QLRequest");

        TypeSpec.Builder query = TypeSpec.classBuilder(className + "Request")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(constructor.build())
                .addFields(fields)
                .addMethods(getterAndSetter)
                .addSuperinterface(qlRequest)

        if (areConstructorParam) {
            query.addMethod(emptyConstructor.build());
        }

        return query.build();
    }

    private void computeParams(
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

    TypeName getType(QLType qlType) {
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

    MethodSpec generateGetter(ParameterSpec param) {
        MethodSpec.Builder getter = MethodSpec.methodBuilder(param.name)
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

    boolean isCollection(TypeName typeName) {
        final String returnType = typeName.toString();
        return returnType.startsWith("java.util.List<");
    }

    MethodSpec generateSetter(ParameterSpec param) {
        MethodSpec.Builder setter = MethodSpec.methodBuilder(param.name)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(param)
                .returns(param.type);

        setter.addStatement("this.\$N = \$N", param.name, param.name);
        return setter.addStatement("return \$N", param.name).build();
    }

    void computeVarsMap(ArrayList<FieldSpec> fieldSpecs, ArrayList<MethodSpec> methodSpecs) {

        ClassName stringClass = ClassName.get("java.lang", "String");
        ClassName stringBuilder = ClassName.get("java.lang", "StringBuilder");


        MethodSpec.Builder getVars = MethodSpec.methodBuilder("variables");
        getVars.addModifiers(Modifier.PUBLIC);
        getVars.addAnnotation(Override.class)
        getVars.returns(stringClass);
        getVars.addStatement("\$T result = new \$T()", stringBuilder, stringBuilder);
        getVars.addStatement("result.append(\"{\")");

        int i = 0;

        for (FieldSpec fieldSpec : fieldSpecs) {
            getVars.beginControlFlow("if (\$N != null)", fieldSpec.name);
            getVars.addStatement("result.append(\"\\\"\")");
            getVars.addStatement("result.append(\$S)", fieldSpec.name);
            getVars.addStatement("result.append(\"\\\"\")");
            getVars.addStatement("result.append(\":\")");
            getVars.addStatement("result.append(\$N)", fieldSpec.name);
            getVars.endControlFlow();
            if (i < fieldSpecs.size() - 1) {
                getVars.addStatement("result.append(\",\")");
            }
        }
        getVars.addStatement("result.append(\"}\")");

        getVars.addStatement("return result.toString()");
        methodSpecs.add(getVars.build());
    }

    private void addTarget(
            ArrayList<FieldSpec> fieldSpecs,
            ArrayList<MethodSpec> methodSpecs,
            String packageName,
            String className
    ) {
        ClassName targetClassName = ClassName.get(packageName, className + "Response");
        ClassName qlResponse = ClassName.get(PACKAGE, "QLResponseModel");
        TypeName wildcardClass = ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(qlResponse));
        TypeName targetClass = ParameterizedTypeName.get(ClassName.get(Class.class), targetClassName);

        FieldSpec target = FieldSpec
                .builder(wildcardClass, "target", Modifier.PUBLIC, Modifier.FINAL)
                .initializer("\$T.class", targetClassName)
                .build()

        MethodSpec targetMethod = MethodSpec.methodBuilder("target")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(targetClass)
                .addStatement("return (\$T) \$N", targetClass, target)
                .build()

        fieldSpecs.add(target)
        methodSpecs.add(targetMethod)
    }

    private void addQuery(
            ArrayList<FieldSpec> fields,
            ArrayList<MethodSpec> methods,
            ArrayList<FieldSpec> mandatoryFields
    ) {
        FieldSpec.Builder queryField = FieldSpec
                .builder(String.class, "query", Modifier.PRIVATE, Modifier.FINAL);
        queryField.initializer("\$S", qlQuery.printQuery())
        fields.add(queryField.build())
        methods.add(getQuery(mandatoryFields))
    }

    private MethodSpec getQuery(List<FieldSpec> mandatoryFields) {
        ClassName exception = ClassName.get(PACKAGE + ".exceptions", "QLException");

        MethodSpec.Builder statement = MethodSpec.methodBuilder("query")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
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

    TypeSpec generateResponse(String fileName, String packageName) {

        return createModels(fileName, packageName)
    }

    TypeSpec createModels(String className, String packageName) {


        ClassName qlResponse = ClassName.get(PACKAGE, "QLResponseModel");

        MethodSpec.Builder emptyConstructor = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        TypeSpec.Builder queryResponse = TypeSpec
                .classBuilder(className + "Response")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(emptyConstructor.build())
                .addSuperinterface(qlResponse)

        for (QLNode root : qlQuery.getQueryFields()) {
            horizontalTreeReed(root, queryResponse, packageName)
        }
        queryResponse.addMethod(computeToString(queryResponse));

        return queryResponse.build();
    }

    MethodSpec computeToString(TypeSpec.Builder typeSpec) {
        ClassName stringClass = ClassName.get("java.lang", "String");
        ClassName stringBuild = ClassName.get("java.lang", "StringBuilder");
        TypeSpec target = typeSpec.build()
        MethodSpec.Builder toString = MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(stringClass)
                .addStatement("\$T result = new \$T()", stringBuild, stringBuild)
                .addStatement("result.append(\$S)", target.name)
                .addStatement("result.append(\"{\\n\")", target.name)

        int i = 0;
        for (FieldSpec field : target.fieldSpecs) {
            toString.addStatement("result.append(\"\\\"\")");
            toString.addStatement("result.append(\$S)", field.name);
            toString.addStatement("result.append(\"\\\"\")");
            toString.addStatement("result.append(\":\")");
            toString.addStatement("result.append(\$N.toString())", field.name);
            if (i < target.fieldSpecs.size() - 1) {
                toString.addStatement("result.append(\",\")")
            }
        }

        toString.addStatement("result.append(\"\\n}\")", target.name)
        toString.addStatement("return result.toString()")

        return toString.build()
    }

    void horizontalTreeReed(QLElement qlElement, TypeSpec.Builder parent, String packageName) {
        String packageNameChild = packageName + "." + parent.build().name;

        String elementName = qlElement.getAlias() == null ? qlElement.getName() : qlElement.getAlias();
        ClassName qlModel = ClassName.get(PACKAGE + ".model", "QLModel")

        if (qlElement instanceof QLNode) {
            TypeSpec.Builder model = TypeSpec.classBuilder(elementName.capitalize())
                    .addSuperinterface(qlModel)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
            generateFieldSetterGetter(parent, builderType(packageNameChild, elementName, qlElement.isList()), elementName);
            for (QLElement child : qlElement.getChildren()) {
                horizontalTreeReed(child, model, packageNameChild);
            }
            model.addMethod(computeToString(model));
            parent.addType(model.build());
        } else if (qlElement instanceof QLLeaf) {
            QLLeaf leaf = (QLLeaf) qlElement;
            generateFieldSetterGetter(parent, buildListType(leaf.getType(), leaf.isList()), elementName);
        } else if (qlElement instanceof QLFragmentNode) {
            QLFragment fragment = qlQuery.findFragment(qlElement.getName());
            for (QLElement child : fragment.getChildren()) {
                horizontalTreeReed(child, parent, packageNameChild);
            }
        }
    }

    TypeName buildListType(QLType type, boolean isList) {
        final ClassName raw = getType(type);
        if (!isList) {
            return raw;
        }
        ClassName list = ClassName.get("java.util", "List");
        return ParameterizedTypeName.get(list, raw);
    }

    TypeName builderType(String packageName, final String modelName, boolean isList) {
        final ClassName raw = rawBuilderType(modelName.capitalize(), packageName);
        if (!isList) {
            return raw;
        }
        ClassName list = ClassName.get("java.util", "List");
        return ParameterizedTypeName.get(list, raw);
    }

    ClassName rawBuilderType(final String d, String packageName) {
        return ClassName.get(packageName, d);
    }

    private void generateFieldSetterGetter(TypeSpec.Builder parent, TypeName type, String name) {
        parent.addField(FieldSpec.builder(type, name, Modifier.PRIVATE).build());
        ParameterSpec param = ParameterSpec.builder(type, name).build()
        parent.addMethod(generateGetter(param));
        parent.addMethod(generateSetter(param));
    }
}
