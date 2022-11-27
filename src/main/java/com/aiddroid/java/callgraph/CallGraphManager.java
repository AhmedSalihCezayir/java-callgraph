package com.aiddroid.java.callgraph;

import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.Neo4jException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CallGraphManager implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(CallGraphManager.class.getName());
    private final Driver driver;

    public CallGraphManager(String uri, String user, String password, Config config) {
        // The driver is a long living object and should be opened during the start of your application
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password), config);
    }

    @Override
    public void close() {
        // The driver object should be closed before the application ends.
        driver.close();
    }

    public void createCallGraph(ProjectInfo projectInfo) {
        Map<CallGraphNode, List<CalleeFunction>> methodCallRelation = projectInfo.getCallerCallees();
        HashMap<String, BasicFunctionDefNode> definedFunctions = projectInfo.getDefinedFunctions();

        methodCallRelation.forEach((caller, calleeList) -> {

            // If this function does not call another one, do not create a node for it
            if (!calleeList.isEmpty()) {
                calleeList.forEach((calleeFunction -> {
                    // If this method is not user-defined, skip it
                    if (!definedFunctions.containsKey(calleeFunction.getFunctionSignature())) {
                        return;
                    }

                    BasicFunctionDefNode functionDefNode = definedFunctions.get(calleeFunction.getFunctionSignature());
                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put("callerFunctionName", caller.getFunctionName());
                    parameters.put("callerSignature", caller.getFunctionSignature());
                    parameters.put("callerFilePath", caller.getFilePath());
                    parameters.put("callerClassName", caller.getClassName());
                    parameters.put("callerPackageName", caller.getPackageName());
                    parameters.put("callerDeclarationStart", caller.getDeclarationStart());
                    parameters.put("callerDeclarationEnd", caller.getDeclarationEnd());
                    parameters.put("calleeFunctionName", calleeFunction.getFunctionName());
                    parameters.put("calleeSignature", calleeFunction.getFunctionSignature());
                    parameters.put("calleeFilePath", functionDefNode.getFilePath());
                    parameters.put("calleeClassName", calleeFunction.getClassName());
                    parameters.put("calleePackageName", calleeFunction.getPackageName());
                    parameters.put("calleeDeclarationStart", functionDefNode.getDeclarationStart());
                    parameters.put("calleeDeclarationEnd", functionDefNode.getDeclarationEnd());
                    parameters.put("callLine", calleeFunction.getCallLine());

                    var query = new Query(
                            """
                                    MERGE (callerFunc: Function {
                                                         functionName: $callerFunctionName,
                                                         signature: $callerSignature,
                                                         filePath: $callerFilePath,
                                                         className: $callerClassName,
                                                         packageName: $callerPackageName,
                                                         declarationStart: $callerDeclarationStart,
                                                         declarationEnd: $callerDeclarationEnd })
                                    
                                    MERGE (calleeFunc: Function {
                                                            functionName: $calleeFunctionName,
                                                            signature: $calleeSignature,
                                                            filePath: $calleeFilePath,
                                                            className: $calleeClassName,
                                                            packageName: $calleePackageName,
                                                            declarationStart: $calleeDeclarationStart,
                                                            declarationEnd: $calleeDeclarationEnd })
                                                                                  
                                    CREATE (callerFunc)-[:CALLS { line: $callLine }]->(calleeFunc)
                                    RETURN callerFunc, calleeFunc
                                 """, parameters
                    );

                    try (var session = driver.session(SessionConfig.forDatabase("neo4j"))) {
                        session.executeWrite(tx -> tx.run(query).single());
                    } catch (Neo4jException ex) {
                        LOGGER.log(Level.SEVERE, query + " raised an exception", ex);
                        throw ex;
                    }
                }));
            }
        });

    }
}