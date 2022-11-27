package com.aiddroid.java.callgraph;

public class CalleeFunction {
    private String functionSignature;
    private String className;
    private String functionName;
    private String packageName;
    private int callLine;

    public CalleeFunction() {}

    public CalleeFunction(String functionSignature, String className, String functionName, String packageName, int callLine) {
        this.functionSignature = functionSignature;
        this.className = className;
        this.functionName = functionName;
        this.packageName = packageName;
        this.callLine = callLine;
    }

    public String getFunctionSignature() {
        return functionSignature;
    }

    public void setFunctionSignature(String functionSignature) {
        this.functionSignature = functionSignature;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getCallLine() {
        return callLine;
    }

    public void setCallLine(int callLine) {
        this.callLine = callLine;
    }

    @Override
    public String toString() {
        return "CalleeFunction{" +
                "functionSignature='" + functionSignature + '\'' +
                ", className='" + className + '\'' +
                ", functionName='" + functionName + '\'' +
                ", packageName='" + packageName + '\'' +
                ", callLine=" + callLine +
                '}';
    }
}
