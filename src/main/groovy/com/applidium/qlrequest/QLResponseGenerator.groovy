package com.applidium.qlrequest

import com.applidium.qlrequest.Query.QLFragment
import com.applidium.qlrequest.Query.QLQuery
import com.applidium.qlrequest.Query.QLType
import com.applidium.qlrequest.Tree.QLElement
import com.applidium.qlrequest.Tree.QLFragmentNode
import com.applidium.qlrequest.Tree.QLLeaf
import com.applidium.qlrequest.Tree.QLNode
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec

import javax.lang.model.element.Modifier


public class QLResponseGenerator {

    private QLQuery queryAsTree;
    private List<String> alreadyUsedClassNames = new ArrayList<>();
    private rootClassPackageName;

    QLResponseGenerator(QLQuery queryAsTree, rootClassPackageName) {
        this.queryAsTree = queryAsTree
        this.rootClassPackageName = rootClassPackageName
    }

    TypeSpec createModels(String className) {
        ClassName qlResponse = ClassName.get(QLClassGenerator.PACKAGE, "QLResponseModel")

        MethodSpec.Builder emptyConstructor = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        TypeSpec.Builder queryResponse = TypeSpec
                .classBuilder(className + "Response")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(emptyConstructor.build())
                .addSuperinterface(qlResponse)

        for (QLElement root : queryAsTree.getQueryFields()) {
            alreadyUsedClassNames.clear();
            horizontalTreeReed(root, queryResponse, rootClassPackageName)
        }
        queryResponse.addMethod(computeToString(queryResponse));

        return queryResponse.build();
    }

    MethodSpec computeToString(TypeSpec.Builder typeSpec) {
        ClassName stringClass = ClassName.get("java.lang", "String");
        ClassName stringBuild = ClassName.get("java.lang", "StringBuilder")
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

        String elementName = qlElement.getAlias() == null ? qlElement.getName() : qlElement.getAlias()
        ClassName qlModel = ClassName.get(QLClassGenerator.PACKAGE + ".model", "QLModel")
        if (qlElement instanceof QLNode) {
            convertNode(elementName, qlElement, qlModel, parent, packageNameChild)
        } else if (qlElement instanceof QLLeaf) {
            convertLeaf(qlElement, parent, elementName)
        } else if (qlElement instanceof QLFragmentNode) {
            convertFragment(qlElement, parent, packageName)
        }
    }

    private void convertNode(
            String elementName,
            QLNode qlElement,
            ClassName qlModel,
            TypeSpec.Builder parent,
            String packageNameChild
    ) {
        String typeName = handleUniquenessModelName(elementName, qlElement)
        TypeSpec.Builder model = TypeSpec.classBuilder(typeName.capitalize())
                .addSuperinterface(qlModel)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        def type = builderType(packageNameChild, typeName, qlElement.isList())
        QLClassGenerator.generateFieldSetterGetter(parent, type, elementName);
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
        def type = buildListType(leaf.getType(), leaf.isList(), leaf.getEnumName())
        QLClassGenerator.generateFieldSetterGetter(parent, type, elementName);
    }

    private void convertFragment(
            QLFragmentNode qlElement, TypeSpec.Builder parent, String packageName
    ) {
        if (qlElement.isInlineFragment()) {
            for (QLElement child: qlElement.getChildren()) {
                horizontalTreeReed(child, parent, packageName)
            }
        } else {
            QLFragment fragment = queryAsTree.findFragment(qlElement.getName());
            for (QLElement child : fragment.getChildren()) {
                horizontalTreeReed(child, parent, packageName);
            }
        }
    }

    TypeName buildListType(QLType type, boolean isList) {
        return buildListType(type, isList, null);
    }

    TypeName buildListType(QLType type, boolean isList, String enumName) {
        final ClassName raw = QLClassGenerator.getType(type, enumName, rootClassPackageName);
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
}
