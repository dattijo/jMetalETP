package org.uma.jmetal.operator.localsearch.impl;

//import java.util.Comparator;
import java.util.Comparator;
import org.uma.jmetal.operator.localsearch.LocalSearchOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.comparator.impl.OverallConstraintViolationComparator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 *
 * @author aadatti
 */
public class GreatDelugeAlgorithm<S extends Solution<?>> implements LocalSearchOperator<S>{
    private Problem<S> problem;
    private int improvementRounds;
    private Comparator<S> constraintComparator;
    private Comparator<S> comparator;
    
    private MutationOperator<S> mutationOperator;
    private int evaluations;
    private int numberOfImprovements;
    
    private RandomGenerator<Double> randomGenerator;
    
    public GreatDelugeAlgorithm(int improvementRounds, MutationOperator<S> mutationOperator,
            Comparator<S> comparator, Problem<S> problem){
        this(improvementRounds, mutationOperator, comparator, problem, 
                ()->JMetalRandom.getInstance().nextDouble());
    }
    
    public GreatDelugeAlgorithm(int improvementRounds, MutationOperator<S> mutationOperator,
            Comparator<S> comparator, Problem<S> problem, RandomGenerator<Double> randomGenerator){
        this.problem = problem;
        this.mutationOperator = mutationOperator;
        this.improvementRounds = improvementRounds;
        this.comparator = comparator;
        constraintComparator = new OverallConstraintViolationComparator();
        
        this.randomGenerator = randomGenerator;
        numberOfImprovements = 0;
    }
    
    @Override
    public S execute(S solution){
        int best;
        evaluations = 0;        
        int rounds = improvementRounds;
        
        //COST FUNCTION F(S)
        problem.evaluate(solution);
        //DESIRED VALUE
        double desiredValue=10049.0;
        //BOUNDARY LEVEL B = F(S)
        double boundaryLevel = solution.getObjective(0);
        int resetThreshold = 10;
        
        
        //DECAY RATE = delB
        double decayRate;
        if(rounds!=0){
            decayRate = (boundaryLevel - desiredValue)/rounds;
        }
        else{
            return solution;            
        }
        double bestCost = boundaryLevel;
        int i = 0;
        int noChange=0;
//        System.out.println("Improving solution with objective "+solution.getObjective(0));
//        System.out.println("Rounds = "+rounds+"\nDesired value = "+desiredValue+"\nBoundary Level = "+boundaryLevel);
//        System.out.println("Reset Threshold = "+resetThreshold+"\nDecay Rate = "+decayRate+"\nbestCost = "+bestCost);
        while(i< rounds){            
            //NEIGHBOURHOOD N(S)
//            System.out.println("Local Search Round "+i);
            S mutatedSolution = mutationOperator.execute((S)solution.copy());
            
            problem.evaluate(mutatedSolution);
            double mutatedSolutionCost = mutatedSolution.getObjective(0);
            evaluations++;
            //IF(S*)<=F(S) OR F(S*)<=B
            best= comparator.compare(mutatedSolution, solution);
            if(best==-1||mutatedSolutionCost<=bestCost){
                solution = mutatedSolution;
                numberOfImprovements++;
                noChange=0;
                if(best==-1){
//                    System.out.println("Improved solution: "+mutatedSolutionCost+" < "+solution.getObjective(0)+" Old solution");
                }else{
//                    System.out.println("Improved solution: "+mutatedSolutionCost+" <= "+bestCost+" bestCost");
                }                
            }else{
                noChange++;
//                System.out.println("No improvement");
            }
            
            //B = B- delB
            if(mutatedSolutionCost<bestCost){
                bestCost=mutatedSolutionCost;                
                boundaryLevel -= decayRate;
//                System.out.println("Updating bestCost and boundaryLevel after improvement");
//                System.out.println("New bestCost = "+bestCost+"\t New boundaryLevel = "+boundaryLevel);
            }
            
            if(noChange>=resetThreshold){
                boundaryLevel = solution.getObjective(0);
//                System.out.println(noChange+" iterations without improvement. Resetting boundary level");
//                System.out.println("boundaryLevel = "+boundaryLevel);
            }            
            i++;
//            System.out.println("\n\nNext Iteration");
        }
        return (S) solution.copy();
    }
    
    @Override
    public int getNumberOfEvaluations(){
        return evaluations;
    }
    
    @Override
    public int getNumberOfImprovements(){
        return numberOfImprovements;
    }
}
