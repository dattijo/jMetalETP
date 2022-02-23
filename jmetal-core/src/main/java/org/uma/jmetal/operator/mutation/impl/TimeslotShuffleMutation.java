package org.uma.jmetal.operator.mutation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.util.checking.Check;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 *
 * @author aadatti
 * @param <T>
 */
public class TimeslotShuffleMutation<T> implements MutationOperator<IntegerMatrixSolution<T>> {   
   
    Comparator comparator;

    private double mutationProbability;
    private RandomGenerator<Double> mutationRandomGenerator;    

    int numberOfTimeslots;        
    ArrayList<Integer> freeTimeslots;
    ArrayList<Integer> usedTimeslots;

    public TimeslotShuffleMutation(double mutationProbability, int timeslots) {
        this(mutationProbability, () -> JMetalRandom.getInstance().nextDouble(), timeslots);
    }

    /**
     * Constructor
     * @param mutationProbability
     * @param mutationRandomGenerator
     * @param timeslots
     */
    public TimeslotShuffleMutation(double mutationProbability, RandomGenerator<Double> mutationRandomGenerator, int timeslots) {
        Check.probabilityIsValid(mutationProbability);
        this.mutationProbability = mutationProbability;
        this.mutationRandomGenerator = mutationRandomGenerator;        
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
                ArrayList<Integer> allTimeslots = new ArrayList();
                for(int i=0;i<numberOfTimeslots;i++){
                    allTimeslots.add(i);
                }
                ArrayList<ArrayList<Integer>> mySol = new ArrayList(solutionLength);
                for(int i=0;i<solutionLength;i++)mySol.add(new ArrayList());
                IntegerMatrixSolution tmpSolution = (IntegerMatrixSolution) solution.copy();                
                ArrayList exam;                          
//                    System.out.println("usedTimeslots = "+usedTimeslots.toString());
//                    System.out.println("Solution before shuffle="+solution.getVariables());
//                    Collections.shuffle(usedTimeslots);  
                Collections.shuffle(allTimeslots);
                for(int slot=0; slot<allTimeslots.size();slot++){    
                    for(int i=0;i<solutionLength;i++){  
                        exam = new ArrayList();
                        exam.addAll((ArrayList)solution.getVariable(i));
                        if(getTimeslot(exam)==slot){
//                                System.out.println("\nOriginal Exam "+i+":"+solution.getVariable(i));
                            int room = getRoom(exam);                        
                            exam.set(slot, -1);                                
//                                exam.set(usedTimeslots.get(slot), room);
                            exam.set(allTimeslots.get(slot), room);
                            mySol.set(i,exam);                                
//                                System.out.println("Shuffled Exam "+i+":"+mySol.get(i).toString());
                        }                                                    
                    }                        
                }
//                    System.out.println("\nRESULTS:");                    
                for(int i=0;i<solutionLength;i++){ 
                    tmpSolution.setVariable(i, mySol.get(i));
//                        System.out.println("\nsolution  "+i+": "+solution.getVariable(i));
//                        System.out.println("   mySol  "+i+": "+mySol.get(i).toString());
                }  
//                    System.out.println("Solution after shuffle: "+tmpSolution.getVariables());                                      
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
