package com.applidium.qlrequest.Query;

public class QLStaticParameter {
    private QLType type;
    private Object value;

    public QLStaticParameter(QLType type) {
        this.type = type;
    }

    public QLStaticParameter(QLType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public QLType getType() {
        return type;
    }

    public void setType(QLType type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
