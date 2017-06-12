package com.applidium.qlrequest.Query;

import java.util.ArrayList;
import java.util.List;

public class QLParameters {
    private List<QLVariablesElement> params;

    public QLParameters() {
        params = new ArrayList<>();
    }

    public QLParameters(List<QLVariablesElement> params) {
        this.params = params;
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    public QLType getType(String key) {
        for (QLVariablesElement element: params) {
            if (key.equals(element.getName())) {
                return element.getType();
            }
        }
        return null;
    }

    public boolean allParametersGiven (QLVariables variables) {
        for (QLVariablesElement element : params) {
            if (element.isMandatory()) {
                if (!variables.isPresent(element.getName())) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<QLVariablesElement> getParams() {
        return params;
    }

    public void addParams(List<QLVariablesElement> params) {
        this.params.addAll(params);
    }

    public void setParams(List<QLVariablesElement> params) {
        this.params.clear();
        this.params = params;
    }

    public String printParameters() {
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        for (QLVariablesElement element: params) {
            stringBuilder.append(element.print());
            if (i < params.size() - 1) {
                stringBuilder.append(",");
            }
            i++;
        }
        return stringBuilder.toString();
    }
}
