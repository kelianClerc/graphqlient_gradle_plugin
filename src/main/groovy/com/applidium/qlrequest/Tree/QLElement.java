package com.applidium.qlrequest.Tree;

import com.applidium.qlrequest.Query.QLVariablesElement;

import java.util.HashMap;
import java.util.Map;

public class QLElement {

    private String name;
    private final Map<String, Object> parameters = new HashMap<>();
    private String alias;
    private boolean isList;
    private String include;
    private String skip;

    public QLElement(QLElement element) {
        if (element != null) {
            this.name = element.getName();
            this.alias = element.getAlias();
            this.parameters.clear();
            this.parameters.putAll(element.getParameters());
            this.isList = element.isList;
            this.skip = element.skip;
            this.include = element.include;
        }
    }

    public QLElement(String name) {
        this.name = name;
    }

    public QLElement(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    public QLElement(String name, Map<String, Object> parameters) {
        this.name = name;
        this.parameters.clear();
        this.parameters.putAll(parameters);
    }

    public QLElement(String name, String alias,  Map<String, Object> parameters) {
        this.name = name;
        this.alias = alias;
        this.parameters.clear();
        this.parameters.putAll(parameters);
    }

    public String print() {
        String result = "";
        result += computeAlias();
        result += name;
        result += computeParams();
        return result;
    }

    private String computeAlias() {
        if (alias == null) {
            return "";
        }
        return alias + ":";
    }

    private String computeParams() {
        if (parameters == null || parameters.size() <= 0) {
            return "";
        }
        String result = "";
        result += "(";
        int i = 0;
        for (String key: parameters.keySet()) {
            result += key + ":"+ computeParamValue(key);
            if (i < parameters.size() - 1) {
                result += ",";
            }
            i++;
        }
        result += ")";
        if (include != null) {
            result += "@include(if:";
            result += include;
            result += ")";
        }
        if (skip != null) {
            result += "@skip(if:";
            result += include;
            result += ")";
        }
        return result;
    }

    private String computeParamValue(String key) {
        // TODO (kelianclerc) 19/5/17 handle params being dependent of variable field of request
        // http://graphql.org/learn/queries/#variables
        Object o = parameters.get(key);
        if (o instanceof QLVariablesElement) {
            return ((QLVariablesElement) o).printVariableName();
        } else if (o instanceof String) {
            return "\"" + String.valueOf(o) + "\"";
        }
        return String.valueOf(o);
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias12) {
        this.alias = alias12;
    }

    public void setParameters(Map<String, Object> params) {
        this.parameters.clear();
        this.parameters.putAll(params);
    }

    public QLElement getElement() {
        return this;
    }

    public boolean isList() {
        return isList;
    }

    public void setList(boolean list) {
        isList = list;
    }

    public String getInclude() {
        return include;
    }

    public void setInclude(String include) {
        this.include = include;
    }

    public String getSkip() {
        return skip;
    }

    public void setSkip(String skip) {
        this.skip = skip;
    }
}
