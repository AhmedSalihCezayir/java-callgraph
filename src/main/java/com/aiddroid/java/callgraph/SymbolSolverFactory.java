package com.aiddroid.java.callgraph;

import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 符号解析工厂类
 * @author allen
 */
public class SymbolSolverFactory {

    private static Logger logger = LoggerFactory.getLogger(SymbolSolverFactory.class);

    /**
     * 获取符号推理器，以便获取某个类的具体来源
     * @param srcPaths
     * @param libPaths
     * @return 
     */
    public static JavaSymbolSolver getJavaSymbolSolver(List<String> srcPaths, List<String> libPaths) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();

        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver(); // jdk推理
        reflectionTypeSolver.setParent(reflectionTypeSolver);
        combinedTypeSolver.add(reflectionTypeSolver);

        ProjectRoot projectRoot = new SymbolSolverCollectionStrategy().collect(new File(srcPaths.get(0)).toPath());
        projectRoot.getSourceRoots().forEach(root -> combinedTypeSolver.add(new JavaParserTypeSolver(root.getRoot())));

        List<JarTypeSolver> jarTypeSolvers = makeJarTypeSolvers(libPaths);//jar包推理
        jarTypeSolvers.stream().forEach(t -> combinedTypeSolver.add(t));

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);

        return symbolSolver;
    }

    /**
     * 获取jar包的符号推理器
     * @param libPaths
     * @return 
     */
    private static List<JarTypeSolver> makeJarTypeSolvers(List<String> libPaths) {
        List<String> jarPaths = Utils.getFilesBySuffixInPaths("jar", libPaths);
        List<JarTypeSolver> jarTypeSolvers = new ArrayList<>(jarPaths.size());
        try {
            for (String jarPath : jarPaths) {
                jarTypeSolvers.add(new JarTypeSolver(jarPath));
            }
        } catch (IOException e) {
            logger.error("找不到：" + e.getMessage());
            e.printStackTrace();
        }
        return jarTypeSolvers;
    }

    /**
     * 获取工程源代码src的符号推理器
     * @param srcPaths
     * @return 
     */
    private static List<JavaParserTypeSolver> makeJavaParserTypeSolvers(List<String> srcPaths) {
        List<JavaParserTypeSolver> javaParserTypeSolvers = srcPaths.stream()
                .map(t -> new JavaParserTypeSolver(new File(t))).collect(Collectors.toList());
        return javaParserTypeSolvers;
    }

    /**
     * 获取符号推理器
     * @param srcPath
     * @param libPath
     * @return 
     */
    public JavaSymbolSolver getJavaSymbolSolver(String srcPath, String libPath) {
        return getJavaSymbolSolver(Utils.makeListFromOneElement(srcPath), Utils.makeListFromOneElement(libPath));
    }
}
