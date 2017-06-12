package com.applidium.qlrequest.Query;

import com.applidium.qlrequest.Tree.QLElement;

import java.util.ArrayList;
import java.util.List;

public class QLFragment {

    private String name;
    private String targetObject;
    private List<QLElement> children;

    public QLFragment() {
        children = new ArrayList<>();
    }

    public QLFragment(String name, String targetObject) {
        super();
        this.name = name;
        this.targetObject = targetObject;
    }

    public QLFragment(String name, String targetObject, List<QLElement> children) {
        this.name = name;
        this.targetObject = targetObject;
        this.children = children;
    }

    public String printLink() {
        return "..." + name;
    }

    public String printFragment() {
        String result = "";
        result += "fragment " + name + " on " + targetObject + " {";
        int i = 0;
        for (QLElement child: children) {
            result += child.print();
            if (i < children.size()-1) {
                result += ",";
            }
            i++;
        }
        result += "}";

        return result;
    }

    public List<QLElement> getChildren() {
        return children;
    }

    public void addChild(QLElement child) {
        children.add(child);
    }

    public void setChildren(List<QLElement> children) {
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetObject() {
        return targetObject;
    }

    public void setTargetObject(String targetObject) {
        this.targetObject = targetObject;
    }

    @Override
    public String toString() {
        return printFragment();
    }
}
