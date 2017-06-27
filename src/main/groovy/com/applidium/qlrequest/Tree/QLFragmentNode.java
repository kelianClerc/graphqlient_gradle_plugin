package com.applidium.qlrequest.Tree;

public class QLFragmentNode extends QLElement {
    public QLFragmentNode(QLElement element) {
        super(element);
    }

    public QLFragmentNode(String name) {
        super(name);
    }

    @Override
    public String print() {
        return "..." + getName();
    }
}
