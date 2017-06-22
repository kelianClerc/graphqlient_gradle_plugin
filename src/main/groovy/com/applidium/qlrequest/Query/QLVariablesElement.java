package com.applidium.qlrequest.Query;

public class QLVariablesElement {
    public static final String MANDATORY_CHARACTER = "!";
    public static final String VARIABLE_CHARACTER = "$";
    private String name;
    private QLType type;
    private String enumName;
    private boolean isMandatory;
    private Object defaultValue;

    public QLVariablesElement() {
    }

    public QLVariablesElement(String name) {
        this.name = name;
    }

    public QLVariablesElement(String name, QLType type) {
        this.name = name;
        this.type = type;
        isMandatory = false;
    }

    public QLVariablesElement(String name, QLType type, boolean isMandatory) {
        this.name = name;
        this.type = type;
        this.isMandatory = isMandatory;
    }

    public QLVariablesElement(String name, QLType type, boolean isMandatory, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.isMandatory = isMandatory;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public QLType getType() {
        return type;
    }

    public void setType(QLType type) {
        this.type = type;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    public String printVariableName() {
        return VARIABLE_CHARACTER + name;
    }

    public String print() {
        String res = "";
        res += printVariableName() + ":";
        res += type.toString();
        if (isMandatory) {
            res += MANDATORY_CHARACTER;
        }
        if (defaultValue != null) {
            res += "=" + defaultValue;
        }
        return res;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getEnumName() {
        return enumName;
    }

    public void setEnumName(String enumName) {
        this.enumName = enumName;
    }
}
