package com.aiddroid.java.callgraph;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.jgrapht.Graph;
import org.jgrapht.graph.*;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.neo4j.driver.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用程序主类
 * @author allen
 */
public class Application {
    
    private static Logger logger = LoggerFactory.getLogger(Application.class);
    
    /**
     * 主方法
     * @param args 
     */
    public static void main(String[] args) {
        // 初始化配置
        Settings settings = new Settings();
        settings.initFromCmdArgs(args);
        
        // 获取方法调用关系
        MethodCallExtractor extractor = new MethodCallExtractor(settings);
        ProjectInfo projectInfo = extractor.getMethodCallRelationByDefault();

        Map<CallGraphNode, List<CalleeFunction>> methodCallRelation = projectInfo.getCallerCallees();
        HashMap<String, BasicFunctionDefNode> definedFunctions = projectInfo.getDefinedFunctions();

        DirectedWeightedPseudograph<CallGraphNode, DefaultWeightedEdge> graph =
                new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);

        methodCallRelation.forEach((caller, calleeList) -> {
            if (!graph.containsVertex(caller) && !calleeList.isEmpty()) {
                graph.addVertex(caller);
            }
            calleeList.forEach((calleeFunction -> {
                if (!definedFunctions.containsKey(calleeFunction.getFunctionSignature())) {
                    // This method is not user defined
                    return;
                }
                BasicFunctionDefNode functionDefNode = definedFunctions.get(calleeFunction.getFunctionSignature());
                CallGraphNode callee = new CallGraphNode(calleeFunction.getFunctionSignature(),
                                                                calleeFunction.getClassName(),
                                                                calleeFunction.getFunctionName(),
                                                                calleeFunction.getPackageName(),
                                                                functionDefNode.getFilePath(),
                                                                functionDefNode.getDeclarationStart(),
                                                                functionDefNode.getDeclarationEnd());

                if (!graph.containsVertex(callee)) {
                    graph.addVertex(callee);
                }
                DefaultWeightedEdge edge = graph.addEdge(caller, callee);
                graph.setEdgeWeight(edge, calleeFunction.getCallLine());
            }));
        });

        logger.info("graph:" + graph + "\n");
        logger.info("View doT graph below via https://edotor.net/ :" + "\n");

        String dot = toDoT(graph);

        try {
            FileUtils.writeStringToFile(new File("graph.txt"), dot, "UTF-8", true);
            logger.info("doT image saved to graph.txt");
        } catch (Exception e) {
            logger.error("write doT error, " + e.getMessage());
        }

        var uri = "<uri>";
        var user = "<username>";
        var password = "<password>";
        CallGraphManager callGraphManager = new CallGraphManager(uri, user, password, Config.defaultConfig());

        callGraphManager.createCallGraph(projectInfo);
        callGraphManager.close();
        System.out.println("Successfully created a callgraph");
    }
    
    /**
     * 转换为doT
     * @param directedGraph
     * @return 
     */
    public static String toDoT(Graph<CallGraphNode, DefaultWeightedEdge> directedGraph) {
        DOTExporter<CallGraphNode, DefaultWeightedEdge> exporter = new DOTExporter<>();

        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.toString()));
            return map;
        });

        exporter.setEdgeAttributeProvider((e) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("weight", DefaultAttribute.createAttribute(directedGraph.getEdgeWeight(e)));
            return map;
        });

        Writer writer = new StringWriter();
        exporter.exportGraph(directedGraph, writer);
        
        return writer.toString();
    }
}
