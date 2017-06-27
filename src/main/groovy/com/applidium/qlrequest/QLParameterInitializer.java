package com.applidium.qlrequest;

import com.applidium.qlrequest.Query.QLStaticParameter;

public class QLParameterInitializer {
    private boolean shouldAddAnnotationToField;
    private String linkedTo;
    private QLStaticParameter parameter;

    public QLParameterInitializer(boolean shouldAddAnnotationToField) {
        this.shouldAddAnnotationToField = shouldAddAnnotationToField;
    }

    public QLParameterInitializer(
        boolean shouldAddAnnotationToField, String linkedTo, QLStaticParameter parameter
    ) {
        this.shouldAddAnnotationToField = shouldAddAnnotationToField;
        this.linkedTo = linkedTo;
        this.parameter = parameter;
    }

    public boolean isShouldAddAnnotationToField() {
        return shouldAddAnnotationToField;
    }

    public String getLinkedTo() {
        return linkedTo;
    }

    public void setShouldAddAnnotationToField(boolean shouldAddAnnotationToField) {
        this.shouldAddAnnotationToField = shouldAddAnnotationToField;
    }

    public void setLinkedTo(String linkedTo) {
        this.linkedTo = linkedTo;
    }

    public QLStaticParameter getParameter() {
        return parameter;
    }

    public void setParameter(QLStaticParameter parameter) {
        this.parameter = parameter;
    }
}

