package com.aiddroid.java.callgraph;

public class BasicFunctionDefNode {
    private String functionSignature;
    private String filePath;
    private int declarationStart;
    private int declarationEnd;

    public BasicFunctionDefNode() {
    }

    public BasicFunctionDefNode(String functionSignature, String filePath, int declarationStart, int declarationEnd) {
        this.functionSignature = functionSignature;
        this.filePath = filePath;
        this.declarationStart = declarationStart;
        this.declarationEnd = declarationEnd;
    }

    public String getFunctionSignature() {
        return functionSignature;
    }

    public void setFunctionSignature(String functionSignature) {
        this.functionSignature = functionSignature;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getDeclarationStart() {
        return declarationStart;
    }

    public void setDeclarationStart(int declarationStart) {
        this.declarationStart = declarationStart;
    }

    public int getDeclarationEnd() {
        return declarationEnd;
    }

    public void setDeclarationEnd(int declarationEnd) {
        this.declarationEnd = declarationEnd;
    }
}
