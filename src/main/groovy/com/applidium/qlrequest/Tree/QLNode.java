package com.applidium.qlrequest.Tree;

import com.applidium.qlrequest.model.QLModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QLNode extends QLElement {

    private static final String OPENING_CHARACTER = "{";
    private static final String CLOSING_CHARACTER = "}";
    private static final String SEPARATION_SUBFIELD_CHARACTER = ",";

    private final ArrayList<QLElement> children = new ArrayList<>();
    private Class<QLModel> type;
    private boolean isList;

    public QLNode(QLElement element) {
        super(element);
    }
    public QLNode(String name) {
        super(name);
    }
    public QLNode(String name, String alias) {
        super(name, alias);
    }
    public QLNode(String name, Map<String, Object> params) {
        super(name, params);
    }
    public QLNode(String name, String alias, Map<String, Object> params) {
        super(name, alias, params);
    }public QLNode(String name, String alias, Map<String, Object> params, Class<QLModel> nodeClass) {
        super(name, alias, params);
        this.type = nodeClass;
    }

    public QLNode(String name, String alias, Map<String, Object> parameters, boolean isList) {
        super(name, alias, parameters);
        this.isList = isList;
    }

    public void addChild(QLElement child) {
        children.add(child);
    }
    public void setAllChild(List<QLElement> allChild) {
        children.clear();
        children.addAll(allChild);
    }
    public void addAllChild(List<QLElement> allChild) {
        children.addAll(allChild);
    }

    public void removeChild(QLElement child) {
        children.remove(child);
    }

    public List<QLElement> getChildren() {
        return children;
    }

    public String getElementInfo() {
        return super.print();
    }

    @Override
    public String print() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getElementInfo());
        if (getChildren().size() > 0) {
            appendChildrens(getChildren(), stringBuilder);
        }
        return stringBuilder.toString();
    }

    private static void appendChildrens(List<QLElement> node, StringBuilder stringBuilder) {
        stringBuilder.append(OPENING_CHARACTER);
        for (int i = 0; i < node.size() - 1; i++) {
            QLElement nodeChild = node.get(i);
            stringBuilder.append(nodeChild.print());
            stringBuilder.append(SEPARATION_SUBFIELD_CHARACTER);
        }
        stringBuilder.append(node.get(node.size()-1).print());
        stringBuilder.append(CLOSING_CHARACTER);
    }

    public Class<QLModel> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = (Class<QLModel>) type;
    }

    public boolean isList() {
        return isList;
    }

    public void setList(boolean list) {
        isList = list;
    }
}
