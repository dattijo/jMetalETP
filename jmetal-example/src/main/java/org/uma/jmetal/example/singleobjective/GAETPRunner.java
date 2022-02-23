package org.uma.jmetal.example.singleobjective;

import java.util.ArrayList;
import java.util.List;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.example.AlgorithmRunner;
import org.uma.jmetal.algorithm.singleobjective.geneticalgorithm.GeneticAlgorithmBuilder;

import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.NullCrossover;
import org.uma.jmetal.operator.localsearch.LocalSearchOperator;
import org.uma.jmetal.operator.localsearch.impl.GreatDelugeAlgorithm;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.Kinterchange;
import org.uma.jmetal.operator.mutation.impl.NullMutation;
import org.uma.jmetal.operator.mutation.impl.RoomMoveMutation;
import org.uma.jmetal.operator.mutation.impl.RoomSwapMutation;
import org.uma.jmetal.operator.mutation.impl.ExamMoveMutation;
import org.uma.jmetal.operator.mutation.impl.TimeslotMoveMutation;
import org.uma.jmetal.operator.mutation.impl.TimeslotShuffleMutation;
import org.uma.jmetal.operator.mutation.impl.TimeslotSwapMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;

import org.uma.jmetal.problem.integermatrixproblem.IntegerMatrixProblem;
import org.uma.jmetal.problem.singleobjective.ETP;

import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;

import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

/**
 * @author aadatti
 */
public class GAETPRunner 
{                         
    public static void main(String[] args) throws Exception 
    {
        IntegerMatrixProblem problem;
        Algorithm<IntegerMatrixSolution<Integer>> algorithm;
        CrossoverOperator<IntegerMatrixSolution<Integer>> crossover;
        MutationOperator<IntegerMatrixSolution<Integer>> mutation;        
        MutationOperator<IntegerMatrixSolution<Integer>> localSearchMutation;
        LocalSearchOperator<IntegerMatrixSolution<Integer>> localSearch;
        SelectionOperator<List<IntegerMatrixSolution<Integer>>, IntegerMatrixSolution<Integer>> selection;  
        RankingAndCrowdingDistanceComparator<IntegerMatrixSolution<Integer>> comparator;
        
        //"C:\Users\PhDLab\Documents\NetBeansProjects\jMetal\jmetal-example\dataset\modifiedITC2007\exam_comp_set00.exam"
        problem = new ETP("C:/Users/PhDLab/Documents/NetBeansProjects/examTimetableDataReader/exam_comp_set00.exam");
    
        int [][] conflictMatrix =  problem.getConflictMatrix();        
        int [] roomCapacities = problem.getRoomCapacities();
        int [] examEnrollments = problem.getExamEnrollments();        
        int numberOfTimeslots = problem.getNumberOfTimeslots();
        
        ArrayList largestExams = problem.getLargestExams();
        
        double mutationProbability = 0.9;   
        int improvementRounds = 0;
        int examsCount=2;
        
        boolean earlierTimeslot=true; 
        boolean changeRoom = true;
        boolean kempeChain=true;                     
        
        //mutation = new TimeslotShuffleMutation(mutationProbability, numberOfTimeslots);                
        
        mutation = new NullMutation();
        localSearchMutation = new NullMutation();
        int rand = (int)(9*Math.random());
        switch(6){
            //Move 1 or more randomly selected exam(s) to random feasible timeslot(s)
            case 0: localSearchMutation = new ExamMoveMutation(mutationProbability, numberOfTimeslots, examsCount);
                System.out.println("Move "+examsCount+" randomly selected exam(s) to random feasible timeslot(s)");
                break;
            //Move 1 randomly selected exam  to random feasible timeslot and room
            case 1: localSearchMutation = new ExamMoveMutation(mutationProbability, numberOfTimeslots, changeRoom, roomCapacities, examEnrollments);
                System.out.println("Move 1 randomly selected exam  to random feasible timeslot and room");
                break;            
            //Move 1 large exam to random ealier feasible timeslot
            case 2: localSearchMutation = new ExamMoveMutation(mutationProbability, numberOfTimeslots, largestExams, earlierTimeslot);
                System.out.println("Move 1 large exam to random ealier feasible timeslot");
                break;
            
            //Move all exams in a randomly selected timeslot to another feasible timeslot            
            case 3: localSearchMutation = new TimeslotMoveMutation(mutationProbability, numberOfTimeslots);
                System.out.println("Move all exams in a randomly selected timeslot to another feasible timeslot");
                break;
            //Swap all exams in 2 randomly selected timeslots
            case 4: localSearchMutation = new TimeslotSwapMutation(mutationProbability, numberOfTimeslots);
                System.out.println("Swap all exams in 2 randomly selected timeslots");
                break;
            //Shuffle all timeslots
            case 5: localSearchMutation = new TimeslotShuffleMutation(mutationProbability, numberOfTimeslots);
                System.out.println("Shuffle all timeslots");
                break;
            
            //Move 1 randomly selected exam to a another randomly selected timeslot using the KempeChain Intercange            
            case 6: localSearchMutation = new Kinterchange(mutationProbability, conflictMatrix, problem, kempeChain);
                System.out.println("Move 1 randomly selected exam to a another randomly selected timeslot using the KempeChain Intercange");
                break;
            //Swap timeslots of 2 randomly selected exams if feasible
            case 7: localSearchMutation = new Kinterchange(mutationProbability, conflictMatrix, problem);
                System.out.println("Swap timeslots of 2 randomly selected exams if feasible");
                break;
            
            //Move a randomly selected exam to a new feasible room within the same timeslot            
            case 8: localSearchMutation = new RoomMoveMutation(mutationProbability, roomCapacities, examEnrollments); 
                System.out.println("Move a randomly selected exam to a new feasible room within the same timeslot");
                break;
            //Swap the rooms of 2 randomly selected exam if feasible.
            case 9: localSearchMutation = new RoomSwapMutation(mutationProbability, roomCapacities, examEnrollments);
                System.out.println("Swap the rooms of 2 randomly selected exam if feasible");
                break;
        }
        
        crossover = new NullCrossover(); 
        comparator = new RankingAndCrowdingDistanceComparator<IntegerMatrixSolution<Integer>>();
        localSearch = new GreatDelugeAlgorithm(improvementRounds, localSearchMutation, comparator, problem);        
        selection = new BinaryTournamentSelection<IntegerMatrixSolution<Integer>>(comparator);                        
        algorithm = new GeneticAlgorithmBuilder<>(problem, crossover, mutation, localSearch)
            .setPopulationSize(100)      //.setPopulationSize(100)
            .setMaxEvaluations(200)      //.setMaxEvaluations(250000) 
            .setSelectionOperator(selection)
            .setVariant(GeneticAlgorithmBuilder.GeneticAlgorithmVariant.GENERATIONAL)
            .build() ; 

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();
    
        IntegerMatrixSolution<Integer> solution = algorithm.getResult();        
        
        List<IntegerMatrixSolution<Integer>> population = new ArrayList<>(1);

        population.add(solution) ;
        
        long computingTime = algorithmRunner.getComputingTime();

        new SolutionListOutput(population)
            .setVarFileOutputContext(new DefaultFileOutputContext("VAR.tsv"))
//            .setVarFileOutputContext(new DefaultFileOutputContext("itc2007VAR.sln"))
            .setFunFileOutputContext(new DefaultFileOutputContext("FUN.tsv"))
            .print();                

        JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
        JMetalLogger.logger.info("Objectives values have been written to file FUN.tsv");
        JMetalLogger.logger.info("Variables values have been written to file VAR.tsv");
    }
}