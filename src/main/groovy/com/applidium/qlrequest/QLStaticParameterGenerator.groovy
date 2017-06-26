package com.applidium.qlrequest

import com.applidium.qlrequest.Query.QLFragment
import com.applidium.qlrequest.Query.QLQuery
import com.applidium.qlrequest.Query.QLVariablesElement
import com.applidium.qlrequest.Tree.QLElement
import com.applidium.qlrequest.Tree.QLFragmentNode
import com.applidium.qlrequest.Tree.QLLeaf
import com.applidium.qlrequest.Tree.QLNode
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec

import static javax.lang.model.element.Modifier.PUBLIC
import static javax.lang.model.element.Modifier.STATIC

public class QLStaticParameterGenerator {

    QLQuery queryAsTree;
    CodeBlock.Builder queryAsStringWithParameterAdded = new CodeBlock.Builder();
    private List<String> alreadyUsedClassNamesRequest = new ArrayList<>();

    CodeBlock computeStaticParameters(TypeSpec.Builder query, QLQuery queryAsTree) {
        this.queryAsTree = queryAsTree;
        String printedQuery = queryAsTree.printQuery()
        String queryHeader = printedQuery.substring(0, printedQuery.indexOf("{") + 1)
        queryAsStringWithParameterAdded.add("\$L", queryHeader);
        findStaticParameterAndHandleIt(query);
        queryAsStringWithParameterAdded.add("}");
        return queryAsStringWithParameterAdded.build();
    }

    void findStaticParameterAndHandleIt(TypeSpec.Builder query) {
        checkStaticParameterInQuery(query)
        checkStaticParameterInFragment(query)
    }

    private void checkStaticParameterInQuery(TypeSpec.Builder query) {
        for (QLElement element : queryAsTree.getQueryFields()) {
            List<String> fieldDictionaryOfCurrentElement = new ArrayList<>();
            createElementClass(element, query, fieldDictionaryOfCurrentElement, "");
        }
    }

    private boolean createElementClass(
            QLElement element,
            TypeSpec.Builder parent,
            List<String> varNameDictionnary,
            String parentPackage
    ) {
        boolean shouldAddToParent = false;
        String nodeName = computeNodeName(element)
        TypeSpec.Builder subNode = TypeSpec.classBuilder(nodeName).addModifiers(PUBLIC, STATIC);
        String packageNodeName = parentPackage.equals("") ? nodeName : parentPackage + "." + nodeName;

        if (element instanceof QLNode) {
            shouldAddToParent = createNodeClass(element, subNode, varNameDictionnary, packageNodeName)
        } else if (element instanceof QLLeaf) {
            shouldAddToParent = createLeafClass(element, subNode, varNameDictionnary, packageNodeName)
        } else if (element instanceof QLFragmentNode) {
            createFragmentClass(element, parent, varNameDictionnary, packageNodeName, subNode)
        }

        if (shouldAddToParent) {
            QLRequestGenerator.addNestedClassToParent(parent, subNode, parentPackage, nodeName.capitalize())
        } else {
            alreadyUsedClassNamesRequest.remove(packageNodeName);
        }
        return shouldAddToParent;
    }

    private boolean createNodeClass(
            QLNode element,
            TypeSpec.Builder node,
            List<String> nodesfieldRegister,
            String packageToCurrentClass
    ) {
        boolean shouldAddToParent = false;
        shouldAddToParent = hasNodeParameters(element, node, nodesfieldRegister, shouldAddToParent)
        computeBuiltQuery(element, shouldAddToParent, false, node, packageToCurrentClass);
        List<String> alreadyUsedVarName = new ArrayList<>();
        shouldAddToParent = hasChildrenParameters(element, node, alreadyUsedVarName, packageToCurrentClass, shouldAddToParent)
        queryAsStringWithParameterAdded.add("}");
        shouldAddToParent
    }

    private void computeBuiltQuery(QLElement element, boolean shouldAddCustomParams, boolean shouldAddChildren, TypeSpec.Builder associatedClass, String packageToCurrentClass) {
        String querySoFar = queryAsStringWithParameterAdded.build()
        if (querySoFar.length() > 0) {
            if (querySoFar.charAt(querySoFar.length()-1) == '{') {
                queryAsStringWithParameterAdded.add("\$L", element.print(shouldAddCustomParams, shouldAddChildren));
            } else if (querySoFar.charAt(querySoFar.length()-1) != ',') {
                queryAsStringWithParameterAdded.add(",\$L", element.print(shouldAddCustomParams, shouldAddChildren))
            }
        }

        if (shouldAddCustomParams) {
            appendParams(associatedClass, packageToCurrentClass);
        }

        if (element instanceof QLNode) {
            queryAsStringWithParameterAdded.add("{");
        }

    }

    private void appendParams(TypeSpec.Builder associatedClass, String packageToCurrentClass) {
        List<FieldSpec> fields = associatedClass.build().fieldSpecs;
        queryAsStringWithParameterAdded.add("(");
        int i = 0;
        for (FieldSpec fieldSpec : fields) {
            if (fieldSpec.annotations.size() > 0) {
                String annotationValue = fieldSpec.annotations.get(0).members.get("link").get(0).toString();
                println(annotationValue);
                String target = annotationValue.replaceAll("\"", "");
                queryAsStringWithParameterAdded.add("\\\"\$N\\\"", target);
                queryAsStringWithParameterAdded.add(":");
                queryAsStringWithParameterAdded.add("\$L" ,'"+');
                queryAsStringWithParameterAdded.add("\$N.\$N()", packageToCurrentClass, fieldSpec.name);
                queryAsStringWithParameterAdded.add("\$L" ,'+"');
                if (i < fields.size() - 1) {
                    queryAsStringWithParameterAdded.add(",");
                }
                i++;
            }
        }
        queryAsStringWithParameterAdded.add(")");
    }

    private boolean hasNodeParameters(QLNode element, TypeSpec.Builder subNode, List<String> varNameDictionnary, boolean shouldAddToParent) {
        if (element.parameters.size() > 0) {
            if (createParameterField(element, subNode, varNameDictionnary)) {
                shouldAddToParent = true;
            }
        }
        shouldAddToParent
    }

    private boolean hasChildrenParameters(QLNode element, TypeSpec.Builder subNode, ArrayList<String> alreadyUsedVarName, String packageToCurrentClass, boolean shouldAddToParent) {
        for (QLElement child : element.getChildren()) {
            if (createElementClass(child, subNode, alreadyUsedVarName, packageToCurrentClass)) {
                shouldAddToParent = true
            }
        }
        shouldAddToParent
    }

    private boolean createLeafClass(QLLeaf element, TypeSpec.Builder subNode, List<String> varNameDictionnary, String packageToCurrentClass) {
        boolean shouldAddToParent = false;
        if (element.parameters.size() > 0) {
            if (createParameterField(element, subNode, varNameDictionnary)) {
                shouldAddToParent = true;
            }
        }
        computeBuiltQuery(element, shouldAddToParent, false, subNode, packageToCurrentClass);
        shouldAddToParent
    }

    private void createFragmentClass(
            QLElement element,
            TypeSpec.Builder parent,
            List<String> varNameDictionnary,
            String nodeName,
            TypeSpec.Builder subNode
    ) {
        if (element.isInlineFragment()) {
            for (QLElement subElement : element.getChildren()) {
                createElementClass(subElement, parent, varNameDictionnary, nodeName);
            }
        } else {
            computeBuiltQuery(element, false, true, subNode, nodeName)
        }
    }

    private void checkStaticParameterInFragment(TypeSpec.Builder query) {
        for (QLFragment fragment : queryAsTree.fragments) {
            String fragmentName = fragment.name.capitalize()
            TypeSpec.Builder fragmentClass = TypeSpec.classBuilder(fragmentName)
                    .addModifiers(PUBLIC, STATIC);

            if(checkStaticParameterInFragmentChildren(fragment, fragmentClass, fragmentName)) {
                QLRequestGenerator.addNestedClassToParent(query, fragmentClass, "", fragmentName)
            } else {
                alreadyUsedClassNamesRequest.remove(fragmentName);
            }
        }
    }

    private boolean checkStaticParameterInFragmentChildren(
            QLFragment fragment, TypeSpec.Builder fragmentClass, String fragmentName
    ) {
        boolean containsStaticParameter = false;
        String fragmentHeader = "fragment \$N on \$N {"
        queryAsStringWithParameterAdded.add(fragmentHeader, fragment.name, fragment.targetObject);

        for (QLElement element : fragment.children) {
            List<String> varNameDictionnary = new ArrayList<>();
            if (createElementClass(element, fragmentClass, varNameDictionnary, fragmentName)) {
                containsStaticParameter = true;
            }
        }

        queryAsStringWithParameterAdded.add("}");
        return containsStaticParameter
    }

    private String computeNodeName(QLElement element) {
        if (element instanceof QLFragmentNode && element.isInlineFragment()) {
            return element.target;
        }
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
                ClassName paramType = QLClassGenerator.getParameterType(element.parameters.get(key));
                QLClassGenerator.generateFieldSetterGetter(parent, paramType, paramName, true, key);
                parentModelWontBeEmpty = true
            }
        }

        return parentModelWontBeEmpty;

    }

    private String computeParamName(String key, List<String> alreadyUsedVarName, QLElement element) {
        String paramName = key;
        if (alreadyUsedVarName.contains(key)) {
            paramName = paramName + element.getName().capitalize();
        }
        alreadyUsedVarName.add(paramName);
        return paramName;
    }
}
