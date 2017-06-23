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
    private final List<String> alreadyUsedClassNames = new ArrayList<>();
    private final List<String> alreadyUsedClassNamesRequest = new ArrayList<>();
    private String packageName;

    def generateSource(File file, String packageName) {
        String fileContent = file.text;
        this.packageName = packageName;
        def files = []

        if (file.getName().endsWith(".qlenum")) {
            files << generateEnum(file.getName(), fileContent);
            return files;
        }

        QLParser parser = new QLParser();
        parser.setToParse(fileContent);
        this.qlQuery = parser.buildQuery();

        String className;
        if (qlQuery.name == null || qlQuery.name.equals("")) {
            className = removeFileExtension(file.getName()).capitalize();
        } else {
            className = qlQuery.name.capitalize();
        }

        files << generateQuery(className)
        files << generateResponse(className)
        return files;
    }

    TypeSpec generateEnum(String fileName, String fileContent) {
        TypeSpec.Builder enumFile = TypeSpec.enumBuilder(removeFileExtension(fileName).capitalize() + "QLEnum");
        enumFile.addModifiers(Modifier.PUBLIC)
        fileContent = fileContent.replace(" ", "");
        fileContent = fileContent.replace(System.getProperty("line.separator"), "");
        String[] enumFields = fileContent.split(",");
        for(String field : enumFields) {
            enumFile.addEnumConstant(field);
        }

        return enumFile.build();
    }

    String removeFileExtension(String text) {
        return text[0..<text.lastIndexOf('.')];
    }

    TypeSpec generateQuery(String className) {
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
        addTarget(fields, getterAndSetter, className);
        TypeSpec.Builder query;

        addQuery(fields, getterAndSetter, mandatoryFields, "query");
        ClassName qlRequest = ClassName.get(PACKAGE, "QLRequest");

        if (qlQuery.isMutation()) {
            query = TypeSpec.classBuilder(className + "Mutation")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(constructor.build())
                    .addFields(fields)
                    .addMethods(getterAndSetter)
                    .addSuperinterface(qlRequest)
        } else {
            query = TypeSpec.classBuilder(className + "Request")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(constructor.build())
                    .addFields(fields)
                    .addMethods(getterAndSetter)
                    .addSuperinterface(qlRequest)
        }


        computeTreeQuery(query);

        if (areConstructorParam) {
            query.addMethod(emptyConstructor.build());
        }

        return query.build();
    }

    void computeTreeQuery(TypeSpec.Builder query) {
        for (QLElement element : qlQuery.getQueryFields()) {
            List<String> varNameDictionnary = new ArrayList<>();
            String parentRoot = packageName + "." + query.build().name;
            createNodeClass(element, query, varNameDictionnary, parentRoot);
        }
    }

    private boolean createNodeClass(QLElement element, TypeSpec.Builder parent, List<String> varNameDictionnary, String parentPackage) {
        String nodeName = computeNodeName(element)
        boolean shouldAddToParent = false;
        TypeSpec.Builder subNode = TypeSpec.classBuilder(nodeName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        if (element instanceof QLNode) {
            if (element.parameters.size() > 0) {
                if (createParameterField(element, subNode, varNameDictionnary)) {
                    shouldAddToParent = true;
                }
            }

            List<String> alreadyUsedVarName = new ArrayList<>();
            for (QLElement child : element.getChildren()) {
                if(createNodeClass(child, subNode, alreadyUsedVarName, parentPackage + "." + nodeName)) {
                    shouldAddToParent = true
                }
            }
        } else if (element instanceof QLLeaf) {
            if (element.parameters.size() > 0) {
                if (createParameterField(element, subNode, varNameDictionnary)) {
                    shouldAddToParent = true;
                }
            }
        }

        if (shouldAddToParent) {
            parent.addType(subNode.build());
            ClassName fieldType = ClassName.get(parentPackage, subNode.build().name)
            def field = FieldSpec.builder(fieldType, element.getName().capitalize(), Modifier.PUBLIC)
                    .initializer("new \$T()", fieldType)
                    .build();
            parent.addField(field);
        }
        return shouldAddToParent;
    }

    private String computeNodeName(QLElement element) {
        String nestedClassName = element.getName().capitalize();
        while (alreadyUsedClassNamesRequest.contains(nestedClassName)) {
            nestedClassName = "Sub" + nestedClassName;
        }
        alreadyUsedClassNamesRequest.add(nestedClassName);
        return nestedClassName
    }

    boolean createParameterField(QLElement element, TypeSpec.Builder parent, List<String> varNameDictionnary) {
        boolean parentModelWontBeEmpty = false;
        for (String key :element.parameters.keySet()) {
            if (element.parameters.get(key) instanceof QLVariablesElement) {
                continue;
            } else {
                String paramName = computeParamName(key, varNameDictionnary, element);
                ClassName paramType = getParameterType(element.parameters.get(key));
                generateFieldSetterGetter(parent, paramType, paramName);
                parentModelWontBeEmpty = true
            }
        }

        return parentModelWontBeEmpty;

    }

    ClassName getParameterType(Object o) {
        if (o instanceof String) {
            return ClassName.get("java.lang", "String");
        } else if (o instanceof Integer) {
            return ClassName.get("java.lang", "Integer");
        } else if (o instanceof Float) {
            return ClassName.get("java.lang", "Float");
        } else if (o instanceof Boolean) {
            return ClassName.get("java.lang", "Boolean");
        }
    }

    private String computeParamName(String key, ArrayList<String> alreadyUsedVarName, QLElement element) {
        String paramName = key;
        if (alreadyUsedVarName.contains(key)) {
            paramName = paramName + element.getName().capitalize();
        }
        alreadyUsedVarName.add(paramName);
        return paramName;
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
                    .builder(getType(element.type, element.getEnumName()), element.name)
                    .build();
            FieldSpec field = FieldSpec
                    .builder(getType(element.type, element.getEnumName()), element.name, Modifier.PRIVATE)
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

    TypeName getType(QLType qlType, String enumName) {
        switch (qlType) {
            case QLType.INT:
                return TypeName.get(Integer.class);
            case QLType.FLOAT:
                return TypeName.get(Float.class);
            case QLType.BOOLEAN:
                return TypeName.get(Boolean.class);
            case QLType.ID:
            case QLType.STRING:
                return TypeName.get(String.class);
            case QLType.ENUM:
            default:
                return ClassName.get(packageName, enumName);
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
            ArrayList<FieldSpec> mandatoryFields,
            String name
    ) {
        FieldSpec.Builder queryField = FieldSpec
                .builder(String.class, name, Modifier.PRIVATE, Modifier.FINAL);
        queryField.initializer("\$S", qlQuery.printQuery())
        fields.add(queryField.build())
        methods.add(getQuery(mandatoryFields, name))
    }

    private MethodSpec getQuery(List<FieldSpec> mandatoryFields, String name) {
        ClassName exception = ClassName.get(PACKAGE + ".exceptions", "QLException");

        MethodSpec.Builder statement = MethodSpec.methodBuilder(name)
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
        statement.addStatement("return \$N", name);
        return statement.build()
    }

    TypeSpec generateResponse(String fileName) {

        return createModels(fileName)
    }

    TypeSpec createModels(String className) {


        ClassName qlResponse = ClassName.get(PACKAGE, "QLResponseModel");

        MethodSpec.Builder emptyConstructor = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        TypeSpec.Builder queryResponse = TypeSpec
                .classBuilder(className + "Response")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(emptyConstructor.build())
                .addSuperinterface(qlResponse)

        for (QLElement root : qlQuery.getQueryFields()) {
            alreadyUsedClassNames.clear();
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
            convertNode(elementName, qlElement, qlModel, parent, packageNameChild)
        } else if (qlElement instanceof QLLeaf) {
            convertLeaf(qlElement, parent, elementName)
        } else if (qlElement instanceof QLFragmentNode) {
            convertFragment(qlElement, parent, packageName)
        }
    }

    private void convertNode(String elementName, QLNode qlElement, ClassName qlModel, TypeSpec.Builder parent, String packageNameChild) {
        String typeName = handleUniquenessModelName(elementName, qlElement)
        TypeSpec.Builder model = TypeSpec.classBuilder(typeName.capitalize())
                .addSuperinterface(qlModel)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        generateFieldSetterGetter(parent, builderType(packageNameChild, typeName, qlElement.isList()), elementName);
        for (QLElement child : qlElement.getChildren()) {
            horizontalTreeReed(child, model, packageNameChild);
        }
        model.addMethod(computeToString(model));
        parent.addType(model.build());
    }

    private String handleUniquenessModelName(String elementName, QLNode qlElement) {
        String typeName = elementName;
        if (alreadyUsedClassNames.contains(qlElement.name)) {
            typeName = "sub" + elementName.capitalize();
        }
        alreadyUsedClassNames.add(typeName);
        return typeName
    }

    private void convertLeaf(QLLeaf qlElement, TypeSpec.Builder parent, String elementName) {
        QLLeaf leaf = (QLLeaf) qlElement;
        generateFieldSetterGetter(parent, buildListType(leaf.getType(), leaf.isList(), leaf.getEnumName()), elementName);
    }

    private void convertFragment(QLFragmentNode qlElement, TypeSpec.Builder parent, String packageName) {
        if (qlElement.isInlineFragment()) {
            for (QLElement child: qlElement.getChildren()) {
                horizontalTreeReed(child, parent, packageName)
            }
        } else {
            QLFragment fragment = qlQuery.findFragment(qlElement.getName());
            for (QLElement child : fragment.getChildren()) {
                horizontalTreeReed(child, parent, packageName);
            }
        }
    }

    TypeName buildListType(QLType type, boolean isList) {
        return buildListType(type, isList, null);
    }

    TypeName buildListType(QLType type, boolean isList, String enumName) {
        final ClassName raw = getType(type, enumName);
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
    public void setPackage ( String packageName ) {
    this.packageName = packageName ;
    }
}
