package org.uma.jmetal.lab.experiment.studies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
//import org.uma.jmetal.algorithm.multiobjective.nsgaii.jmetal5version.NSGAII;
import org.uma.jmetal.component.termination.Termination;
import org.uma.jmetal.component.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.component.GenerateBoxplotsWithR;
import org.uma.jmetal.lab.experiment.component.GenerateFriedmanTestTables;
import org.uma.jmetal.lab.experiment.component.GenerateLatexTablesWithStatistics;
import org.uma.jmetal.lab.experiment.component.GenerateWilcoxonTestTablesWithR;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.localsearch.LocalSearchOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.problem.multiobjective.MultiobjectiveETP_1;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.GenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.qualityindicator.impl.hypervolume.PISAHypervolume;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;

/**
 *
 * @author aadatti
 */
public class ETPStudy {
    private static final int INDEPENDENT_RUNS = 50;
    static int populationSize = 100;
    static int offspringPopulationSize = populationSize;
    static CrossoverOperator<IntegerMatrixSolution<Integer>> crossover;
    static MutationOperator<IntegerMatrixSolution<Integer>> mutation;
    static LocalSearchOperator<IntegerMatrixSolution<Integer>> localSearch;
    
    
    public static void main(String[] args) throws IOException{
        String mainPath = "C:/Users/PhDLab/Documents/NetBeansProjects/jMetal/jmetal-example";
        String examDatasetFile = "/dataset/modifiedITC2007/exam_comp_setGSPPartial.exam";
        String examDifficultyData = "/fuzzyData/exam_comp_set_any.diff";
        String fuzzyLogicFile = "/fuzzyData/examDifficulty.fcl";
        
        String experimentBaseDirectory = "";
        
        
        
        List<ExperimentProblem<IntegerMatrixSolution<Integer>>> problemList = new ArrayList<>();
        problemList.add(new ExperimentProblem<>(new MultiobjectiveETP_1(mainPath + examDatasetFile, mainPath + fuzzyLogicFile, mainPath + examDifficultyData)));
        
        List<ExperimentAlgorithm<IntegerMatrixSolution<Integer>, List<IntegerMatrixSolution<Integer>>>> algorithmList = configureAlgorithmList(problemList);
        
        Experiment<IntegerMatrixSolution<Integer>, List<IntegerMatrixSolution<Integer>>> experiment = 
                new ExperimentBuilder<IntegerMatrixSolution<Integer>, List<IntegerMatrixSolution<Integer>>>("ETPStudy")
            .setAlgorithmList(algorithmList)
            .setProblemList(problemList)
            .setExperimentBaseDirectory(experimentBaseDirectory)
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setReferenceFrontDirectory("")
            .setIndicatorList(
                Arrays.asList(
                    new Epsilon<IntegerMatrixSolution<Integer>>(),
                    new Spread<IntegerMatrixSolution<Integer>>(),
                    new GenerationalDistance<IntegerMatrixSolution<Integer>>(),
                    new PISAHypervolume<IntegerMatrixSolution<Integer>>(),
                    new InvertedGenerationalDistance<IntegerMatrixSolution<Integer>>(),
                    new InvertedGenerationalDistancePlus<IntegerMatrixSolution<Integer>>()))
            .setIndependentRuns(INDEPENDENT_RUNS)
            .setNumberOfCores(4)
            .build();
            
        new ExecuteAlgorithms<>(experiment).run();
        new ComputeQualityIndicators<>(experiment).run();
        new GenerateLatexTablesWithStatistics(experiment).run();
        new GenerateWilcoxonTestTablesWithR<>(experiment).run();
        new GenerateFriedmanTestTables<>(experiment).run();
        new GenerateBoxplotsWithR<>(experiment).setRows(2).setColumns(2).run();
    }

    static List<ExperimentAlgorithm<IntegerMatrixSolution<Integer>, List<IntegerMatrixSolution<Integer>>>> configureAlgorithmList(
        List<ExperimentProblem<IntegerMatrixSolution<Integer>>> problemList) {
        List<ExperimentAlgorithm<IntegerMatrixSolution<Integer>, List<IntegerMatrixSolution<Integer>>>> algorithms = new ArrayList<>();
        
        Termination termination = new TerminationByEvaluations(100);
        
        for(int run = 0; run< INDEPENDENT_RUNS; run++){
            for(int i=0; i < problemList.size(); i++){
                Algorithm<List<IntegerMatrixSolution>> algorithm = 
                        new NSGAII<>(problemList.get(i).getProblem(),
                        populationSize,offspringPopulationSize,crossover,mutation,termination,localSearch)
                        .setMaxEvaluations(10)
                        .build();
                algorithms.add(new ExperimentAlgorithm<>(algorithm,"NSGAII-ETP",problemList.get(i),run));
            }
        }
        return algorithms;
    }    
}
