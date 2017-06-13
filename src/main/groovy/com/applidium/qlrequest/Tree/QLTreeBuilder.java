package com.applidium.qlrequest.Tree;

import com.applidium.qlrequest.Query.QLVariablesElement;
import com.applidium.qlrequest.annotations.AliasFor;
import com.applidium.qlrequest.annotations.Argument;
import com.applidium.qlrequest.annotations.Parameters;
import com.applidium.qlrequest.model.QLModel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QLTreeBuilder {

    public QLNode createNodeFromField(Member member) {
        return createNodeFromField(member, true);
    }

    public QLNode createNodeFromField(Member member, boolean shouldBuildTreeRecursively) {
        QLNode node = new QLNode(createElement(member));
        Class<?> fieldType = getFieldType(member);
        node.setType(fieldType);
        if (shouldBuildTreeRecursively) {
            List<QLElement> children = new ArrayList<>();
            Field[] declaredFields = fieldType.getDeclaredFields();
            if (declaredFields.length > 0) {
                for (Field field1: declaredFields) {
                    appendQLElement(children, field1, shouldBuildTreeRecursively);
                }
            } else {
                for (Method method: fieldType.getDeclaredMethods()) {
                    appendQLElement(children, method, shouldBuildTreeRecursively);
                }
            }
            node.addAllChild(children);
        }
        return node;
    }

    private QLElement createElement(Member member) {
        String alias = null;
        String name = member.getName();
        Map<String, Object> parameters = new HashMap<>();
        Member target = getMemberCorrectClass(member);

        for (Annotation annotatedElement : getDeclaredAnnotations(target)) {
            if (annotatedElement instanceof AliasFor) {
                alias = member.getName();
                name = ((AliasFor) annotatedElement).name();
            } else if (annotatedElement instanceof Parameters) {
                createParametersMap(parameters, (Parameters) annotatedElement);
            }
        }
        return new QLElement(name, alias, parameters);
    }

    private Annotation[] getDeclaredAnnotations(Member member) {
        if (member instanceof Field) {
            return ((Field) member).getDeclaredAnnotations();
        } else if (member instanceof Method) {
            return ((Method) member).getDeclaredAnnotations();
        } else {
            // TODO (kelianclerc) 7/6/17 exception
            return null;
        }
    }

    private void createParametersMap(Map<String, Object> parameters, Parameters annotatedElement) {
        for (Argument argument: annotatedElement.table()) {
            if (argument.argumentVariable().length() > 0) {
                parameters.put(argument.argumentName(), new QLVariablesElement(argument.argumentVariable()));
            } else{
                parameters.put(argument.argumentName(), argument.argumentValue());
            }
        }
    }

    private Class<?> getFieldType(Member member) {
        Member target = getMemberCorrectClass(member);
        Class<?> fieldType = getType(target);
        if (Collection.class.isAssignableFrom(fieldType)) {
            ParameterizedType genericType = (ParameterizedType) getGenericType(target);
            fieldType = (Class<?>)(genericType.getActualTypeArguments()[0]);
        }
        return fieldType;
    }

    public void appendQLElement(List<QLElement> result, Member member) {
        appendQLElement(result, member, true);
    }

    public void appendQLElement(
        List<QLElement> result,
        Member member,
        boolean shouldBuildTreeRecursively
    ) {
        Member target = getMemberCorrectClass(member);
        if (target == null) return;
        if (isOfStandardType(target)) {
            result.add(createLeafFromField(target));
        } else if (QLModel.class.isAssignableFrom(getFieldType(target))){
            result.add(createNodeFromField(target, shouldBuildTreeRecursively));
        }

    }

    private Member getMemberCorrectClass(Member member) {
        Member target;
        if (member instanceof Field) {
            target = (Field) member;
        } else if (member instanceof Method) {
            target = (Method) member;
        } else {
            return null;
        }
        return target;
    }

    private Class<?> getType(Member member) {
        if (member instanceof Field) {
            return ((Field) member).getType();
        } else if (member instanceof Method) {
            return ((Method) member).getReturnType();
        } else {
            // TODO (kelianclerc) 7/6/17 exception
            return null;
        }
    }

    private Type getGenericType(Member member) {
        if (member instanceof Field) {
            return ((Field) member).getGenericType();
        } else if (member instanceof Method) {
            return ((Method) member).getGenericReturnType();
        } else {
            // TODO (kelianclerc) 7/6/17 exception
            return null;
        }
    }

    private boolean isOfStandardType(Member field) {
        // TODO (kelianclerc) 23/5/17 How to allow user to add its how enums
        Class<?> fieldType = getFieldType(field);
        return fieldType == String.class
            || fieldType == Float.TYPE
            || fieldType == Float.class
            || fieldType == Boolean.class
            || fieldType == Boolean.TYPE
            || fieldType == Integer.class
            || fieldType == Integer.TYPE
            || fieldType.isEnum();
    }

    private QLLeaf createLeafFromField(Member field) {
        QLElement resultat = createElement(field);
        return new QLLeaf(resultat);
    }

    public void propagateType(QLNode node) {
        Class<?> associatedObject = node.getType();
        if (associatedObject == Object.class) {
            return;
        }

        for (QLElement element : node.getChildren()) {
            if (!(element instanceof QLNode)) {
                break;
            }
            QLNode elementToUpdate = (QLNode) element;
            elementToUpdate.setType(findFieldTypeByName(associatedObject, element.getName(), element.getAlias()));

        }
    }

    private Class<?> findFieldTypeByName(Class<?> type, String name, String alias) {
        for (Field field : type.getFields()) {
            if (field.getName().equals(name) || field.getName().equals(alias)) {
                return field.getType();
            }
        }
        return Object.class;
    }
}
