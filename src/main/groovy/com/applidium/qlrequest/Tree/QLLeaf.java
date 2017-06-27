package com.applidium.qlrequest.Tree;

import com.applidium.qlrequest.Query.QLType;

import java.util.Map;

public class QLLeaf extends QLElement {

    private QLType type;

    public QLLeaf(String name) {
        super(name);
    }

    public QLLeaf(QLElement element) {
        super(element);
    }
    public QLLeaf(String name, String alias) {
        super(name, alias);
    }
    public QLLeaf(String name, Map<String, Object> params) {
        super(name, params);
    }
    public QLLeaf(String name, String alias, Map<String, Object> params) {
        super(name, alias, params);
    }

    public QLLeaf(QLElement element, QLType type) {
        super(element);
        this.type = type == null ? QLType.STRING : type;
    }

    public QLType getType() {
        return type;
    }

    public void setType(QLType type) {
        this.type = type;
    }
}
