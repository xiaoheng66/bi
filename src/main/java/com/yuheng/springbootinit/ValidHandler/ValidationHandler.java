package com.yuheng.springbootinit.ValidHandler;

public abstract class ValidationHandler {
    protected ValidationHandler nextHandler;

    public void setNextHandler(ValidationHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    public void validate(ValidationContext context) {
        if (nextHandler != null) {
            nextHandler.validate(context);
        }
    }
}
