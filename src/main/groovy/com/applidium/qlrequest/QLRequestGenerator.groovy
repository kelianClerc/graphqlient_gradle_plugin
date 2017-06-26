package com.applidium.qlrequest

import com.applidium.qlrequest.Query.QLQuery
import com.applidium.qlrequest.Query.QLVariablesElement
import com.squareup.javapoet.*

import javax.lang.model.element.Modifier

import static javax.lang.model.element.Modifier.FINAL
import static javax.lang.model.element.Modifier.PUBLIC

public class QLRequestGenerator {

    private List<FieldSpec> fields = new ArrayList<>();
    private List<MethodSpec> getterAndSetter = new ArrayList<>();
    private List<FieldSpec> mandatoryFields = new ArrayList<>();
    private QLQuery queryAsTree;
    private CodeBlock queryAsStringWithParameterAdded;
    private String rootClassPackageName;

    QLRequestGenerator(QLQuery queryAsTree, String rootClassPackageName) {
        this.queryAsTree = queryAsTree
        this.rootClassPackageName = rootClassPackageName;
    }

    TypeSpec generateQuery(String className) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC);
        TypeSpec.Builder queryClass = nameQueryClass(className)

        boolean hasFieldsToInit = generateQueryContent(constructor, className, queryClass)

        ClassName qlRequest = ClassName.get(QLClassGenerator.PACKAGE, "QLRequest");

        queryClass.addModifiers(PUBLIC, FINAL)
                .addMethod(constructor.build())
                .addFields(fields)
                .addMethods(getterAndSetter)
                .addSuperinterface(qlRequest)

        if (hasFieldsToInit) {
            queryClass.addMethod(MethodSpec.constructorBuilder().addModifiers(PUBLIC).build());
        }

        return queryClass.build();
    }

    private TypeSpec.Builder nameQueryClass(String className) {
        TypeSpec.Builder queryClass
        if (queryAsTree.isMutation()) {
            queryClass = TypeSpec.classBuilder(className + "Mutation")
        } else {
            queryClass = TypeSpec.classBuilder(className + "Request")
        }
        queryClass
    }


    private boolean generateQueryContent(
            MethodSpec.Builder constructor, String className, TypeSpec.Builder queryClass
    ) {
        computeParams(constructor)
        computeVarsMap();
        boolean areConstructorParam = fields.size() > 0;
        QLStaticParameterGenerator parameterGenerator = new QLStaticParameterGenerator();
        queryAsStringWithParameterAdded = parameterGenerator.computeStaticParameters(
                queryClass, queryAsTree
        )
        addTarget(className);
        addQuery("query");

        return areConstructorParam;
    }

    private void computeParams(MethodSpec.Builder constructor) {
        for (QLVariablesElement element : queryAsTree.getParameters().getParams()) {
            //todo kelian(12/06/17) exception if arg named query
            def type = QLClassGenerator.getType(
                    element.type, element.getEnumName(), rootClassPackageName
            )
            ParameterSpec param = ParameterSpec.builder(type, element.name).build();
            FieldSpec field = FieldSpec.builder(type, element.name, Modifier.PRIVATE).build();
            if (element.mandatory) {
                mandatoryFields.add(field);
            }

            fields.add(field)
            constructor.addParameter(param);
            constructor.addStatement("this.\$N = \$N", param.name, param.name);
            getterAndSetter.add(QLClassGenerator.generateGetter(param));
            getterAndSetter.add(QLClassGenerator.generateSetter(param));
        }
    }

    void computeVarsMap() {
        ClassName stringClass = ClassName.get("java.lang", "String")
        ClassName stringBuilder = ClassName.get("java.lang", "StringBuilder")

        MethodSpec.Builder getVars = MethodSpec.methodBuilder("variables")
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .returns(stringClass)
                .addStatement("\$T result = new \$T()", stringBuilder, stringBuilder)
                .addStatement("result.append(\"{\")");

        int i = 0;
        for (FieldSpec fieldSpec : fields) {
            printVariableMapElement(getVars, fieldSpec, i)
            i++;
        }
        getVars.addStatement("result.append(\"}\")");
        getVars.addStatement("return result.toString()");
        getterAndSetter.add(getVars.build());
    }

    private void printVariableMapElement(MethodSpec.Builder getVars, FieldSpec fieldSpec, int i) {
        getVars.beginControlFlow("if (\$N != null)", fieldSpec.name);
        getVars.addStatement("result.append(\"\\\"\")");
        getVars.addStatement("result.append(\$S)", fieldSpec.name);
        getVars.addStatement("result.append(\"\\\"\")");
        getVars.addStatement("result.append(\":\")");
        getVars.addStatement("result.append(\$N)", fieldSpec.name);
        getVars.endControlFlow();
        if (i < fields.size() - 1) {
            getVars.addStatement("result.append(\",\")");
        }
    }

    private void addTarget(String className) {
        ClassName targetClassName = ClassName.get(rootClassPackageName, className + "Response");
        ClassName qlResponse = ClassName.get(QLClassGenerator.PACKAGE, "QLResponseModel")
        TypeName wildcardClass = ParameterizedTypeName
                .get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(qlResponse));
        TypeName targetClass = ParameterizedTypeName
                .get(ClassName.get(Class.class), targetClassName);

        FieldSpec target = FieldSpec
                .builder(wildcardClass, "target", PUBLIC, Modifier.FINAL)
                .initializer("\$T.class", targetClassName)
                .build()

        MethodSpec targetMethod = MethodSpec.methodBuilder("target")
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .returns(targetClass)
                .addStatement("return (\$T) \$N", targetClass, target)
                .build()

        fields.add(target)
        getterAndSetter.add(targetMethod)
    }

    private void addQuery(String name) {
        FieldSpec.Builder queryField = FieldSpec
                .builder(String.class, name, Modifier.PRIVATE);
        fields.add(queryField.build())
        getterAndSetter.add(getQuery(name))
    }

    private MethodSpec getQuery(String name) {
        String exceptionPackage = QLClassGenerator.PACKAGE + ".exceptions"
        ClassName exception = ClassName.get(exceptionPackage, "QLException");

        MethodSpec.Builder statement = MethodSpec.methodBuilder(name)
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addException(exception);

        for (FieldSpec field : mandatoryFields) {
            statement.beginControlFlow("if(\$N == null)", field.name);
            String throwMessage = "throw new \$T(\"Mandatory field : \$N is not set\")"
            statement.addStatement(throwMessage, exception, field.name);
            statement.endControlFlow();
        }
        statement.addStatement("\$N = \"\$L\"", name, queryAsStringWithParameterAdded)
        statement.addStatement("return \$N", name);
        return statement.build()
    }


    static void addNestedClassToParent(
            TypeSpec.Builder parent, TypeSpec.Builder toAdd, String parentPackage, String fieldName
    ) {
        parent.addType(toAdd.build());
        ClassName fieldType = ClassName.get(parentPackage, toAdd.build().name)
        def field = FieldSpec.builder(fieldType, fieldName, PUBLIC)
                .initializer("new \$T()", fieldType)
                .build();
        parent.addField(field);
    }
}
