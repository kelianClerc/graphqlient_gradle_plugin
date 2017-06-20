package com.applidium.qlrequest.Tree;

import java.util.ArrayList;
import java.util.List;

public class QLFragmentNode extends QLElement {

    private boolean isInlineFragment;
    private String target;
    private final List<QLElement> children = new ArrayList<>();

    public QLFragmentNode(QLElement element) {
        super(element);
    }

    public QLFragmentNode(String name) {
        super(name);
    }

    @Override
    public String print() {
        if (isInlineFragment) {
            String result = "... on " + target + "{";
            int i = 0;
            for (QLElement element : children) {
                result += element.print();
                if (i < children.size() - 1) {
                    result += ",";
                }
            }
            result += "}";
            return result;
        }
        return "..." + getName();
    }

    public void setChildren(List<QLElement> children) {
        this.children.clear();
        this.children.addAll(children);
    }

    public boolean isInlineFragment() {
        return isInlineFragment;
    }

    public void setInlineFragment(boolean inlineFragment) {
        isInlineFragment = inlineFragment;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public List<QLElement> getChildren() {
        return children;
    }
}
