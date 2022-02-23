package org.uma.jmetal.example.multiobjective.nsgaii.jmetal5version;

//import java.io.File;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.jmetal5version.NSGAIIBuilder;
import org.uma.jmetal.example.AlgorithmRunner;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.uma.jmetal.operator.crossover.impl.NullCrossover;
import org.uma.jmetal.operator.mutation.impl.Kinterchange;
import org.uma.jmetal.problem.integermatrixproblem.IntegerMatrixProblem;
import org.uma.jmetal.problem.multiobjective.MultiobjectiveETP_3;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;

import net.sourceforge.jFuzzyLogic.FIS;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.component.evaluation.impl.MultithreadedEvaluation;
import org.uma.jmetal.component.termination.Termination;
import org.uma.jmetal.component.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.localsearch.LocalSearchOperator;
import org.uma.jmetal.operator.localsearch.impl.GreatDelugeAlgorithm;
import org.uma.jmetal.operator.mutation.impl.ExamMoveMutation;
import org.uma.jmetal.operator.mutation.impl.NullMutation;
import org.uma.jmetal.operator.mutation.impl.RoomMoveMutation;
import org.uma.jmetal.operator.mutation.impl.RoomShuffleMutation;
import org.uma.jmetal.operator.mutation.impl.RoomSwapMutation;
import org.uma.jmetal.operator.mutation.impl.TimeslotMoveMutation;
import org.uma.jmetal.operator.mutation.impl.TimeslotShuffleMutation;
import org.uma.jmetal.operator.mutation.impl.TimeslotSwapMutation;
//import org.uma.jmetal.problem.multiobjective.MultiobjectiveETP;
import org.uma.jmetal.problem.multiobjective.MultiobjectiveETP_Compact;
//import net.sourceforge.jFuzzyLogic.rule.FuzzyRuleSet;

/**
 * Class for configuring and running the NSGA-II algorithm to solve the
 * bi-objective ETP
 *
 * @author Ahmad A. Datti <aadatti.cs@buk.edu.ng>
 */
public class NSGAIIETPRunner extends AbstractAlgorithmRunner {
    /**
     * @param args Command line arguments.
     * @throws java.io.IOException
     * @throws SecurityException
     */
    public static void main(String[] args) throws JMetalException, IOException {
        
        JMetalRandom.getInstance().setSeed(100L);

        IntegerMatrixProblem problem;
        Algorithm<List<IntegerMatrixSolution<Integer>>> algorithm;
        CrossoverOperator<IntegerMatrixSolution<Integer>> crossover;
        MutationOperator<IntegerMatrixSolution<Integer>> mutation;
        MutationOperator<IntegerMatrixSolution<Integer>> localSearchMutation;
        LocalSearchOperator<IntegerMatrixSolution<Integer>> localSearch;
        SelectionOperator<List<IntegerMatrixSolution<Integer>>, IntegerMatrixSolution<Integer>> selection;
        RankingAndCrowdingDistanceComparator<IntegerMatrixSolution<Integer>> comparator;

//    problem = new MultiobjectiveETP("C:/Users/PhDLab/Documents/NetBeansProjects/examTimetableDataReader/exam_comp_set44.exam",
//            "C:/Users/PhDLab/Documents/NetBeansProjects/jMetal/jmetal-example/examDifficulty.fcl",
//            "C:/Users/PhDLab/Documents/NetBeansProjects/jMetal/jmetal-example/examDifficultyData");
        String mainPath = "C:/Users/PhDLab/Documents/NetBeansProjects/jMetal/jmetal-example";
        String examDifficultyData = "/fuzzyData/exam_comp_set_any.diff";
        String fuzzyLogicFile = "/fuzzyData/examDifficulty.fcl";
//        String examDatasetFile = "/dataset/modifiedITC2007/exam_comp_setALL.exam";
        String examDatasetFile = "/dataset/modifiedITC2007/exam_comp_setGSPPartial.exam";
//      "dataset\originalPurdue\pu-exam-spr12.exam" pu-exam-fal08
//        String examDatasetFile = "/dataset/originalPurdue/pu-exam-fal08Reduced.exam";

        System.out.println("Solving...."+examDatasetFile.substring(24));
        

        problem = new MultiobjectiveETP_Compact(mainPath + examDatasetFile, mainPath + fuzzyLogicFile, mainPath + examDifficultyData);
        
//        problem = new MultiobjectiveETP(mainPath + examDatasetFile, mainPath + fuzzyLogicFile, mainPath + examDifficultyData);

        ArrayList largestExams = problem.getLargestExams();
        int[][] conflictMatrix = problem.getConflictMatrix();
        int[] roomCapacities = problem.getRoomCapacities();
        int[] examEnrollments = problem.getExamEnrollments();
        int numberOfTimeslots = problem.getNumberOfTimeslots();        
        int improvementRounds = 10;
        int examsCount = 2;

        boolean earlierTimeslot = true;
        boolean changeRoom = true;
        boolean kempeChain = true;

        double mutationProbability = 0.9;
        crossover = new NullCrossover();
//        mutation = new TimeslotShuffleMutation(mutationProbability, numberOfTimeslots);
        mutation = new RoomShuffleMutation(mutationProbability);
//        mutation = new NullMutation();
//        mutation = new TimeslotMoveMutation(mutationProbability, numberOfTimeslots);
//        mutation = new TimeslotShuffleMutation(mutationProbability, numberOfTimeslots);
//        localSearchMutation = new NullMutation();

//        int rand = new Random().nextInt(9);  
//        
//        for(int mut = 0; mut < 2; mut++){
//            System.out.println("\n");
        
        switch (-1) {
            //Move 1 or more randomly selected exam(s) to random feasible timeslot(s)
            case 0:
                localSearchMutation = new ExamMoveMutation(mutationProbability, numberOfTimeslots, examsCount);
                System.out.println("Move " + examsCount + " randomly selected exam(s) to random feasible timeslot(s)");
                break;
            //Move 1 randomly selected exam  to random feasible timeslot and room
            case 1:
                localSearchMutation = new ExamMoveMutation(mutationProbability, numberOfTimeslots, changeRoom, roomCapacities, examEnrollments);
                System.out.println("Move 1 randomly selected exam to a random feasible timeslot and room");
                break;
            //Move 1 large exam to random ealier feasible timeslot
            case 2:
                localSearchMutation = new ExamMoveMutation(mutationProbability, numberOfTimeslots, largestExams, earlierTimeslot);
                System.out.println("Move 1 large exam to random ealier feasible timeslot");
                break;

            //Move all exams in a randomly selected timeslot to another feasible timeslot            
            case 3:
                localSearchMutation = new TimeslotMoveMutation(mutationProbability, numberOfTimeslots);
                System.out.println("Move all exams in a randomly selected timeslot to another feasible timeslot");
                break;
            //Swap all exams in 2 randomly selected timeslots
            case 4:
                localSearchMutation = new TimeslotSwapMutation(mutationProbability, numberOfTimeslots);
                System.out.println("Swap all exams in 2 randomly selected timeslots");
                break;
            //Shuffle all timeslots
            case 5:
                localSearchMutation = new TimeslotShuffleMutation(mutationProbability, numberOfTimeslots);
                System.out.println("Shuffle all timeslots");
                break;

            //Move 1 randomly selected exam to a another randomly selected timeslot using the KempeChain Intercange            
            case 6:
                localSearchMutation = new Kinterchange(mutationProbability, conflictMatrix, problem, kempeChain);
                System.out.println("Move 1 randomly selected exam to a another randomly selected timeslot using the KempeChain Intercange");
                break;
            //Swap timeslots of 2 randomly selected exams if feasible
            case 7:
                localSearchMutation = new Kinterchange(mutationProbability, conflictMatrix, problem);
                System.out.println("Swap timeslots of 2 randomly selected exams if feasible");
                break;
            //Move a randomly selected exam to a new feasible room within the same timeslot            
            case 8:
                localSearchMutation = new RoomMoveMutation(mutationProbability, roomCapacities, examEnrollments);
                System.out.println("Move a randomly selected exam to a new feasible room within the same timeslot");
                break;
            //Swap the rooms of 2 randomly selected exam if feasible.
            case 9:
                localSearchMutation = new RoomSwapMutation(mutationProbability, roomCapacities, examEnrollments);
                System.out.println("Swap the rooms of 2 randomly selected exams if feasible");
                break;
            default:
                localSearchMutation = new NullMutation();
                System.out.println("No Local Search Mutation");
        }

        selection = new BinaryTournamentSelection<IntegerMatrixSolution<Integer>>(
                        new RankingAndCrowdingDistanceComparator<IntegerMatrixSolution<Integer>>());

        for(int populationSize=10; populationSize<=10;populationSize+=100){
            System.out.println("\n\nPopulation Size = "+populationSize);
//            int populationSize = 100;   //int populationSize = 100;
            int offspringPopulationSize = populationSize;

            Termination termination = new TerminationByEvaluations(10);
            comparator = new RankingAndCrowdingDistanceComparator<IntegerMatrixSolution<Integer>>();
            localSearch = new GreatDelugeAlgorithm(improvementRounds, localSearchMutation, comparator, problem);
            algorithm
                    = new NSGAII<>(
                            problem, populationSize, offspringPopulationSize, crossover, mutation,
                            termination, localSearch)
                            .setEvaluation(new MultithreadedEvaluation<>(8));

//        SERIAL    
//        algorithm =
//            new NSGAIIBuilder<IntegerMatrixSolution<Integer>>(
//                    problem, crossover, mutation, populationSize)
//                .setSelectionOperator(selection)
//                .setMaxEvaluations(1000) //.setMaxEvaluations(10000)
//                .build();

//            for(int run = 0; run < 1; run++){
//                System.out.println("\n");
//                AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();
//
//                List<IntegerMatrixSolution<Integer>> population = algorithm.getResult();
//                long computingTime = algorithmRunner.getComputingTime();
//
//                new SolutionListOutput(population)
//                        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.tsv"))
//                        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.tsv"))
//                        .print();
//
//                JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
////                JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());
////                JMetalLogger.logger.info("Objectives values have been written to file FUN.tsv");
////                JMetalLogger.logger.info("Variables values have been written to file VAR.tsv");
//                
//            }
            AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();
    
            List<IntegerMatrixSolution<Integer>> population = algorithm.getResult();
            long computingTime = algorithmRunner.getComputingTime();
    
            new SolutionListOutput(population)
                    .setVarFileOutputContext(new DefaultFileOutputContext("VAR.tsv"))
                    .setFunFileOutputContext(new DefaultFileOutputContext("FUN.tsv"))
                    .print();
    
            JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
//            JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());
//            JMetalLogger.logger.info("Objectives values have been written to file FUN.tsv");
//            JMetalLogger.logger.info("Variables values have been written to file VAR.tsv");
        }            
//    }
}}
