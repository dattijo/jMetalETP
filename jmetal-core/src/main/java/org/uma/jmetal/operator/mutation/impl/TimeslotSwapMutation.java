package org.uma.jmetal.operator.mutation.impl;

import java.util.ArrayList;
import java.util.Comparator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.util.checking.Check;
import org.uma.jmetal.util.pseudorandom.BoundedRandomGenerator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 *
 * @author aadatti
 */
public class TimeslotSwapMutation<T> implements MutationOperator<IntegerMatrixSolution<T>> {   
   
    Comparator comparator;

    private double mutationProbability;
    private RandomGenerator<Double> mutationRandomGenerator;
    private BoundedRandomGenerator<Integer> positionRandomGenerator;

    int numberOfTimeslots;    
    ArrayList<Integer> freeTimeslots;
    ArrayList<Integer> usedTimeslots;

    public TimeslotSwapMutation(double mutationProbability, int timeslots) {
        this(mutationProbability, () -> JMetalRandom.getInstance().nextDouble(), 
                (a, b) -> JMetalRandom.getInstance().nextInt(a, b), timeslots);
    }

    /**
     * Constructor
     * @param mutationProbability
     * @param mutationRandomGenerator
     * @param positionRandomGenerator
     * @param timeslots
     * @param swap
     */
    public TimeslotSwapMutation(double mutationProbability, RandomGenerator<Double> mutationRandomGenerator,
            BoundedRandomGenerator<Integer> positionRandomGenerator, int timeslots) {
        Check.probabilityIsValid(mutationProbability);
        this.mutationProbability = mutationProbability;
        this.mutationRandomGenerator = mutationRandomGenerator;
        this.positionRandomGenerator = positionRandomGenerator;
        this.numberOfTimeslots = timeslots;         
    }

    @Override
    public double getMutationProbability() {
        return mutationProbability;
    }

    public void setMutationProbability(double mutationProbability) {
        this.mutationProbability = mutationProbability;
    }

    @Override
    public IntegerMatrixSolution<T> execute(IntegerMatrixSolution<T> solution) {
        Check.isNotNull(solution);        
        return doMutation((IntegerMatrixSolution) solution);
    }

    public IntegerMatrixSolution<T> doMutation(IntegerMatrixSolution<T> solution) {       
        
        int solutionLength = solution.getNumberOfVariables();

        if ((solutionLength != 0) && (solutionLength != 1)) {
            
            if (mutationRandomGenerator.getRandomValue() < mutationProbability) {   
//                System.out.println("\n\nNEW SOLUTION");
                getFreeTimeslots(solution); 
//                                                    
                int randTimeslotIndex1 = usedTimeslots.get(positionRandomGenerator.getRandomValue(0, usedTimeslots.size() - 1));                              
                int randTimeslotIndex2 = usedTimeslots.get(positionRandomGenerator.getRandomValue(0, usedTimeslots.size() - 1));

                while (randTimeslotIndex1 == randTimeslotIndex2) {
                    if (randTimeslotIndex1 == (solutionLength - 1)) {
                        randTimeslotIndex2 = usedTimeslots.get(positionRandomGenerator.getRandomValue(0, usedTimeslots.size() - 2));
                    } else {
                        randTimeslotIndex2 = usedTimeslots.get(positionRandomGenerator.getRandomValue(0, usedTimeslots.size() - 1));
                    }
                }   
                
                IntegerMatrixSolution tmpSolution = (IntegerMatrixSolution) solution.copy();                
                ArrayList exam;
                for(int i=0;i<solution.getNumberOfVariables();i++){
                    exam = (ArrayList)solution.getVariable(i);
                    if(getTimeslot(exam)==randTimeslotIndex1){
//                            System.out.println("Moving exam "+i+" from timelsot "+randTimeslotIndex1+" to "+randTimeslotIndex2);
                        int room = getRoom(exam);
                        exam.set(randTimeslotIndex1, -1);
                        exam.set(randTimeslotIndex2, room);
                        tmpSolution.setVariable(i, exam);
                    }
                    else if(getTimeslot(exam)==randTimeslotIndex2){
//                            System.out.println("Moving exam "+i+" from timelsot "+randTimeslotIndex2+" to "+randTimeslotIndex1);
                        int room = getRoom(exam);
                        exam.set(randTimeslotIndex2, -1);
                        exam.set(randTimeslotIndex1, room);
                        tmpSolution.setVariable(i, exam);
                    }                                        
                }                          
                solution = tmpSolution;
            }
        }        
        return solution;
    }
    
    public int getTimeslot(ArrayList<Integer> exam){
        for (int i = 0; i < exam.size(); i++){
            if (exam.get(i) != -1) {
                return i;
            }
        }
        return -1;
    }

    public int getRoom(ArrayList<Integer> exam) {
        for (int i = 0; i < exam.size(); i++) {
            if (exam.get(i) != -1) {
                return exam.get(i);
            }
        }
        return -1;
    }
    
    void getFreeTimeslots(IntegerMatrixSolution solution){
        usedTimeslots = new ArrayList();
        freeTimeslots = new ArrayList();
        
        for(int i=0; i<solution.getNumberOfVariables();i++){
            int timeslot = getTimeslot((ArrayList)solution.getVariable(i));
            if(!usedTimeslots.contains(timeslot)){
                usedTimeslots.add(timeslot);
            }
        }
        
        for(int i=0;i<numberOfTimeslots;i++){
            if(!usedTimeslots.contains(i)){
                freeTimeslots.add(i);
            }
        }         
    } 
}
