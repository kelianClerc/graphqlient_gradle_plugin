package com.applidium.qlrequest.Query;

import com.applidium.qlrequest.Tree.QLElement;
import com.applidium.qlrequest.exceptions.QLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QLQuery {

    private static final String QUERY_KEYWORD = "query";
    private static final String MUTATION_KEYWORD = "mutation";
    private static final String QUERY_OPENING_CHARACTER = "{";
    private static final String QUERY_CLOSING_CHARACTER = "}";

    private final List<QLFragment> fragments = new ArrayList<>();
    private String name;
    private final QLParameters parameters = new QLParameters();
    private final QLVariables variables = new QLVariables();
    // A request must not be anonymous if it has variables.
    private final List<QLElement> queryFields = new ArrayList<QLElement>();
    private boolean isMutation;

    public QLQuery() {
    }

    public QLQuery(String name) {
        super();
        this.name = name;
    }
    public QLQuery(String name, List<QLVariablesElement> parameters) {
        super();
        this.name = name;
        this.parameters.setParams(parameters);
    }

    public void append(QLElement element) {
        if (element != null) {
            queryFields.add(element);
        }
    }

    public void addVariable(String variableName, Object value) throws QLException {
        QLType varType = parameters.getType(variableName);
        if (varType == null) {
            String message = "The variable being added : \"" + variableName + "\" is not present in the " +
                "query parameter list : [" + parameters.printParameters() + "]";
            throw new QLException(message);
        }
        variables.addVariable(variableName, value);
    }

    public boolean isVariableEmpty() {
        return variables.isEmpty();
    }

    public boolean areAllParametersGiven() {
        return parameters.allParametersGiven(variables);
    }

    public String printQuery() {
        StringBuilder stringBuilder = new StringBuilder();
        appendHeader(stringBuilder);
        for (QLElement element : queryFields) {
            stringBuilder.append(element.print());
        }
        appendEnd(stringBuilder);
        for (QLFragment fragment : fragments) {
            stringBuilder.append(fragment.printFragment());
        }
        return stringBuilder.toString();
    }

    private void appendHeader(StringBuilder stringBuilder) {
        if (name != null) {
            if (isMutation) {
                stringBuilder.append(MUTATION_KEYWORD + " " + name);
            } else {
                stringBuilder.append(QUERY_KEYWORD + " " + name);
            }
        }
        if (parameters != null && !parameters.isEmpty()) {
            appendQueryParams(stringBuilder);
        }
        stringBuilder.append(QUERY_OPENING_CHARACTER);
    }

    private void appendQueryParams(StringBuilder stringBuilder) {
        stringBuilder.append("(");
        stringBuilder.append(parameters.printParameters());
        stringBuilder.append(")");
    }

    public void setQueryFields(List<QLElement> queryFields){
        this.queryFields.clear();
        if (queryFields != null) {
            this.queryFields.addAll(queryFields);
        }
    }

    public List<QLElement> getQueryFields() {
        return queryFields;
    }

    public QLParameters getParameters() {
        return parameters;
    }

    public void setParameters(QLParameters parameters) {
        this.parameters.setParams(parameters.getParams());
    }
    public void setParameters(List<QLVariablesElement> parameters) {
        this.parameters.setParams(parameters);
    }


    private void appendEnd(StringBuilder stringBuilder) {
        stringBuilder.append(QUERY_CLOSING_CHARACTER);
    }


    public String getName() {
        return name;
    }

    public QLVariables getVariables() {
        return variables;
    }

    public void setVariables(QLVariables variables) throws QLException {
        Map<String, Object> vars = variables.getVariables();
        for (String key: vars.keySet()) {
            addVariable(key, vars.get(key));
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<QLFragment> getFragments() {
        return fragments;
    }

    public void setFragments(List<QLFragment> fragments) {
        this.fragments.clear();
        this.fragments.addAll(fragments);
    }

    public QLFragment findFragment(String fragmentName) {
        for (QLFragment fragment : fragments) {
            if (fragment.getName().equals(fragmentName)) {
                return fragment;
            }
        }
        return null;
    }

    public boolean isMutation() {
        return this.isMutation;
    }

    public void isMutation(boolean isMutation) {
        this.isMutation = isMutation;
    }
}
