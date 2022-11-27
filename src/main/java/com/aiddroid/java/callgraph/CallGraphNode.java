package com.aiddroid.java.callgraph;

import java.util.Objects;

public class CallGraphNode extends BasicFunctionDefNode {
    private String className;
    private String functionName;
    private String packageName;

    public CallGraphNode() {}

    public CallGraphNode(String functionSignature, String className, String functionName, String packageName, String filePath, int declarationStart, int declarationEnd) {
        super(functionSignature, filePath, declarationStart, declarationEnd);
        this.className = className;
        this.functionName = functionName;
        this.packageName = packageName;
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

    @Override
    public String toString() {
        return "CallGraphNode{" +
                "signature='" + super.getFunctionSignature() + '\'' +
                ", filePath='" + super.getFilePath() + '\'' +
                ", className='" + className + '\'' +
                ", functionName='" + functionName + '\'' +
                ", packageName='" + packageName + '\'' +
                ", declarationStart='" + super.getDeclarationStart() + '\'' +
                ", declarationEnd='" + super.getDeclarationEnd() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CallGraphNode)) return false;
        CallGraphNode that = (CallGraphNode) o;
        return Objects.equals(className, that.className) && Objects.equals(functionName, that.functionName) && Objects.equals(packageName, that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, functionName, packageName);
    }
}
