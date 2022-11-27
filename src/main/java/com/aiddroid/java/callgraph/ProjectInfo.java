package com.aiddroid.java.callgraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectInfo {
    Map<CallGraphNode, List<CalleeFunction>> callerCallees;
    HashMap<String, BasicFunctionDefNode> definedFunctions;

    public ProjectInfo() {}

    public ProjectInfo(Map<CallGraphNode, List<CalleeFunction>> callerCallees, HashMap<String, BasicFunctionDefNode> definedFunctions) {
        this.callerCallees = callerCallees;
        this.definedFunctions = definedFunctions;
    }

    public Map<CallGraphNode, List<CalleeFunction>> getCallerCallees() {
        return callerCallees;
    }

    public void setCallerCallees(Map<CallGraphNode, List<CalleeFunction>> callerCallees) {
        this.callerCallees = callerCallees;
    }

    public HashMap<String, BasicFunctionDefNode> getDefinedFunctions() {
        return definedFunctions;
    }

    public void setDefinedFunctions(HashMap<String, BasicFunctionDefNode> definedFunctions) {
        this.definedFunctions = definedFunctions;
    }
}
