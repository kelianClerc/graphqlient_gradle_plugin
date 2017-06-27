package com.applidium.qlrequest.annotations;

public @interface Argument {
    public String argumentName();
    public String argumentValue() default "";
    public String argumentVariable() default "";
    // TODO (kelianclerc) 22/5/17 handle not string argument
}
