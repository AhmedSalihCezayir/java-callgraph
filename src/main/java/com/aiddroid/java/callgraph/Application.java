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

        System.out.println("\n\n*******************************************************\n\n");
        System.out.println(methodCallRelation);
        System.out.println("\n\n*******************************************************\n\n");
        System.out.println(definedFunctions);
        System.out.println("\n\n*******************************************************\n\n");

        // 声明有向图
//        Graph<String, DefaultEdge> directedGraph =
//            new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

        DirectedWeightedPseudograph<CallGraphNode, DefaultWeightedEdge> graph =
                new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);

        methodCallRelation.forEach((caller, calleeList) -> {
            if (!graph.containsVertex(caller)) {
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
        
//        // 构建有向图
//        for (Map.Entry<String, List<String>> entry : methodCallRelation.entrySet()) {
//            String caller = entry.getKey();
//            // 添加节点和边
//            directedGraph.addVertex(caller);
//            for (String callee : entry.getValue()) {
//                directedGraph.addVertex(callee);
//                directedGraph.addEdge(caller, callee);
//            }
//        }
//
        logger.info("graph:" + graph + "\n");
        logger.info("View doT graph below via https://edotor.net/ :" + "\n");

        String doT = toDoT(graph);
        logger.info(doT);

        try {
            FileUtils.writeStringToFile(new File("graph.txt"), doT, "UTF-8", true);
            logger.info("doT image saved to graph.txt");
        } catch (Exception e) {
            logger.error("write doT error, " + e.getMessage());
        }
    }
    
    /**
     * 转换为doT
     * @param directedGraph
     * @return 
     */
    public static String toDoT(Graph<CallGraphNode, DefaultWeightedEdge> directedGraph) {
        DOTExporter<CallGraphNode, DefaultWeightedEdge> exporter = new DOTExporter<>();
        
        // 为节点添加label
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.toString()));
            return map;
        });
        Writer writer = new StringWriter();
        exporter.exportGraph(directedGraph, writer);
        
        return writer.toString();
    }
}
