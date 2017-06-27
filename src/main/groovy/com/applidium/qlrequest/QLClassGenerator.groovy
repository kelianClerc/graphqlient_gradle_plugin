package com.applidium.qlrequest

import com.applidium.qlrequest.Query.QLQuery
import com.applidium.qlrequest.Query.QLType
import com.applidium.qlrequest.Tree.QLParser
import com.squareup.javapoet.*

import javax.lang.model.element.Modifier

class QLClassGenerator {

    public static final String PACKAGE = "com.applidium.graphqlient";
    private final List<String> alreadyUsedClassNames = new ArrayList<>();

    private QLQuery queryAsTree;
    private String rootClassPackageName;

    def generateSource(File file, String packageName) {
        String fileContent = file.text;
        String fileName = removeFileExtension(file.getName()).capitalize();
        def files = []
        this.rootClassPackageName = packageName;

        if (file.getName().endsWith(".qlenum")) {
            return handleEnum(files, fileName, fileContent)
        }
        return handleQuery(fileContent, fileName, files)
    }

    static String removeFileExtension(String text) {
        return text[0..<text.lastIndexOf('.')];
    }

    private List handleEnum(List files, String fileName, String fileContent) {
        files << QLEnumGenerator.generateEnum(fileName, fileContent);
        return files;
    }

    private List handleQuery(String fileContent, String fileName, List files) {
        QLParser parser = new QLParser();
        parser.setToParse(fileContent);
        this.queryAsTree = parser.buildQuery();
        QLRequestGenerator requestGenerator =
                new QLRequestGenerator(queryAsTree, rootClassPackageName);
        QLResponseGenerator responseGenerator =
                new QLResponseGenerator(queryAsTree, rootClassPackageName);

        String className = computeClassName(fileName)

        files << requestGenerator.generateQuery(className)
        files << responseGenerator.createModels(className)
        return files;
    }

    private String computeClassName(String fileName) {
        String className;
        if (queryAsTree.name == null || queryAsTree.name.equals("")) {
            className = fileName
        } else {
            className = queryAsTree.name.capitalize();
        }
        return className
    }

    static ClassName handleQLTypeParameter (QLType o) {
        switch (o) {
            case QLType.ID:
            case QLType.ENUM:
            case QLType.STRING:
                return ClassName.get("java.lang", "String");
            case QLType.INT:
                return ClassName.get("java.lang", "Integer");
            case QLType.FLOAT:
                return ClassName.get("java.lang", "Float");
            case QLType.BOOLEAN:
                return ClassName.get("java.lang", "Boolean");
        }
    }

    static ClassName getParameterType(Object o) {
        if (o instanceof QLType) {
            return handleQLTypeParameter(o)
        }
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

    static TypeName getType(QLType qlType, String enumName, String rootClassPackageName) {
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
                return ClassName.get(rootClassPackageName, enumName);
        }
    }

    static MethodSpec generateGetter(ParameterSpec param) {
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

    static void generateFieldSetterGetter(TypeSpec.Builder parent, TypeName type, String name) {
        generateFieldSetterGetter(parent, type, name, new QLParameterInitializer(false));
    }

    static void generateFieldSetterGetter(
            TypeSpec.Builder parent,
            TypeName type,
            String name,
            QLParameterInitializer parameterInitializer
    ) {
        FieldSpec.Builder builder = FieldSpec.builder(type, name, Modifier.PRIVATE)
        if (parameterInitializer.shouldAddAnnotationToField) {
            ClassName annotationName = ClassName.get(PACKAGE + ".annotation", "LinkTo");
            AnnotationSpec annotation = AnnotationSpec.builder(annotationName)
                    .addMember("link", "\$S", parameterInitializer.linkedTo)
                    .build()
            builder.addAnnotation(annotation);
        }
        if (parameterInitializer.getParameter() != null && parameterInitializer.getParameter().getValue() != null) {
            Object value = parameterInitializer.getParameter().getValue();
            if (value instanceof String) {
                value = "\""+value+"\"";
            }
            builder.initializer("\$L", value)
        }
        parent.addField(builder.build());
        ParameterSpec param = ParameterSpec.builder(type, name).build()
        parent.addMethod(generateGetter(param));
        parent.addMethod(generateSetter(param));
    }

    public void setPackage ( String packageName ) {
    this.rootClassPackageName = packageName ;
    }

    public void setQueryAsTree(QLQuery query) {
        this.queryAsTree = query;
    }
}
