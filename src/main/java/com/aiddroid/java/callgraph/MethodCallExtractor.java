package com.aiddroid.java.callgraph;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 方法调用提取类
 */
public class MethodCallExtractor {

    private static Logger logger = LoggerFactory.getLogger(MethodCallExtractor.class);
    
    private Settings settings;

    /**
     * 构造方法
     * @param settings 
     */
    public MethodCallExtractor(Settings settings) {
        this.settings = settings;
    }
    
    /**
     * 获取默认情况下，方法调用关系
     * @return 
     */
    public ProjectInfo getMethodCallRelationByDefault() {
        logger.info("从resources目录下配置文件定义的扫描目录开始扫描代码，分析方法调用关系...");
        List<String> srcPaths = settings.getSrcDirs();
        List<String> libPaths = settings.getLibDirs();
        List<Pattern> skipPatterns = settings.getSkipPatterns();
        return getMethodCallRelation(srcPaths, skipPatterns);
    }

    // 获取调用关系
    public ProjectInfo getMethodCallRelation(List<String> srcPaths, List<Pattern> skipPatterns) {
        // 从src和lib目录下解析出符号
        JavaSymbolSolver symbolSolver = SymbolSolverFactory.getJavaSymbolSolver(srcPaths);
        // TODO This is the latest way of configuring the parser. Check whether it breaks anything or not. If it does not break anything change it to the newer version.
        // StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
        StaticJavaParser.getConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        StaticJavaParser.getConfiguration().setPreprocessUnicodeEscapes(true);

        // 获取src目录中的全部java文件，并进行解析
        Map<CallGraphNode, List<CalleeFunction>> callerCallees = new HashMap<>();
        HashMap<String, BasicFunctionDefNode> definedFunctions = new HashMap<>();

        List<String> javaFiles = Utils.getFilesBySuffixInPaths("java", srcPaths);
        int javaFileNum = javaFiles.size();
        for (int i = 0; i < javaFiles.size(); i++) {
            String javaFile = javaFiles.get(i);
            logger.debug("{}/{} processing: {}", i, javaFileNum, javaFile);
            // 解析.java文件中的调用关系
            extract(javaFile, callerCallees, definedFunctions, skipPatterns);
        }
        return new ProjectInfo(callerCallees, definedFunctions);
    }


    /**
     * 分析单个.java文件中的调用关系
     * @param javaFile
     * @param callerCallees 
     */
    private void extract(String javaFile, Map<CallGraphNode, List<CalleeFunction>> callerCallees, HashMap<String, BasicFunctionDefNode> definedFunctions, List<Pattern> skipPatterns) {
        logger.info("Start parsing " + javaFile);

        CompilationUnit cu = null;
        try {
            cu = StaticJavaParser.parse(new FileInputStream(javaFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Get the method declaration and traverse it
        List<MethodDeclaration> all = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration methodDeclaration : all) {
            List<CalleeFunction> curCallees = new ArrayList<>();
            CallGraphNode callGraphNode = null;

            // Iterate over the contents of each method declaration to find other methods that are called internally
            methodDeclaration.accept(new MethodCallVisitor(skipPatterns), curCallees);
            String functionSignature;
            String functionName;
            String className;
            String packageName;
            int declarationStart = methodDeclaration.getBegin().get().line;
            int declarationEnd = methodDeclaration.getEnd().get().line;

            try {
                functionSignature = methodDeclaration.resolve().getQualifiedSignature();
                functionName = methodDeclaration.resolve().getName();
                className = methodDeclaration.resolve().getClassName();
                packageName = methodDeclaration.resolve().getPackageName();
                callGraphNode = new CallGraphNode(functionSignature, className, functionName, packageName, javaFile, declarationStart, declarationEnd);

                definedFunctions.put(functionSignature, new BasicFunctionDefNode(functionSignature, javaFile, declarationStart, declarationEnd));
            } catch (Exception e) {
                functionSignature = methodDeclaration.getSignature().asString();
                logger.error("Use {} instead of  qualified signature, cause: {}", functionSignature, e.getMessage());
            }
            assert functionSignature != null;
            
            // If there is no key in the map, add the key
            if (!callerCallees.containsKey(functionSignature) && !Utils.shouldSkip(functionSignature, skipPatterns)) {
                callerCallees.put(callGraphNode, new ArrayList<>());
            }
            
            if (!Utils.shouldSkip(functionSignature, skipPatterns)) {
                callerCallees.get(callGraphNode).addAll(curCallees);
            }
            
//            logger.info("caller:" + callGraphNode);
//            logger.info("callerCallees:" + callerCallees);
        }

        logger.info("End parsing " + javaFile);
    }
    
    
    // 遍历源码文件时，只关注方法调用的Visitor， 然后提取存放到第二个参数collector中
    private static class MethodCallVisitor extends VoidVisitorAdapter<List<CalleeFunction>> {
        
        private List<Pattern> skipPatterns = new ArrayList<Pattern>();

        public MethodCallVisitor(List<Pattern> skipPatterns) {
            if (skipPatterns != null) {
                this.skipPatterns = skipPatterns;
            }
        }

        public void visit(MethodCallExpr n, List<CalleeFunction> collector) {
            // 提取方法调用
            ResolvedMethodDeclaration resolvedMethodDeclaration = null;
            try {
                resolvedMethodDeclaration = n.resolve();
                // 仅关注提供src目录的工程代码
                String signature = resolvedMethodDeclaration.getQualifiedSignature();
                String functionName = resolvedMethodDeclaration.getName();
                String className = resolvedMethodDeclaration.getClassName();
                String packageName = resolvedMethodDeclaration.getPackageName();
                int callLine = n.getBegin().get().line;

                CalleeFunction calleeFunction = new CalleeFunction(signature, className, functionName, packageName, callLine);

                if (!Utils.shouldSkip(signature, skipPatterns)) {
                    if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) {
                        collector.add(calleeFunction);
                    }

                    // 获取方法调用
                    logger.info("resolvedMethodDeclaration:" + n.resolve().getQualifiedSignature());
                }
            } catch (Exception e) {
                logger.error("Line {}, {} cannot resolve some symbol, because {}",
                        n.getRange().get().begin.line,
                        n.getNameAsString() + n.getArguments().toString().replace("[", "(").replace("]", ")"),
                        e.getMessage());
            }
            // 调试用
            printSymbolType(resolvedMethodDeclaration, n);
            // Don't forget to call super, it may find more method calls inside the arguments of this method call, for example.
            super.visit(n, collector);
        }

        // 打印符号类型
        private void printSymbolType(ResolvedMethodDeclaration resolvedMethodDeclaration, MethodCallExpr n) {
            if (resolvedMethodDeclaration != null) {
                if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) {
                    logger.trace("depend on src: {}", resolvedMethodDeclaration.getQualifiedSignature());
                } else if (resolvedMethodDeclaration instanceof ReflectionMethodDeclaration) {
                    logger.trace("depend on jdk: {}", resolvedMethodDeclaration.getQualifiedSignature());
                } else if (resolvedMethodDeclaration instanceof JavassistMethodDeclaration) {
                    logger.trace("depend on jar: {}", resolvedMethodDeclaration.getQualifiedSignature());
                } else if (resolvedMethodDeclaration instanceof JavaParserEnumDeclaration.ValuesMethod) {
                    logger.error("depend on mem: {}", resolvedMethodDeclaration.getQualifiedSignature());
                } else {
                    logger.error("depend on ???: {}", resolvedMethodDeclaration.getQualifiedSignature());
                }
            }
        }

    }

}
