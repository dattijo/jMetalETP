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
public class RoomShuffleMutation<T> implements MutationOperator<IntegerMatrixSolution<T>> {   
   
    Comparator comparator;

    private double mutationProbability;
    private RandomGenerator<Double> mutationRandomGenerator;        

    public RoomShuffleMutation(double mutationProbability) {
        this(mutationProbability, () -> JMetalRandom.getInstance().nextDouble());
    }

    /**
     * Constructor
     * @param mutationProbability
     * @param mutationRandomGenerator
     * @param students
     */
    public RoomShuffleMutation(double mutationProbability, RandomGenerator<Double> mutationRandomGenerator) {
        Check.probabilityIsValid(mutationProbability);
        this.mutationProbability = mutationProbability;
        this.mutationRandomGenerator = mutationRandomGenerator;                        
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
//        System.out.println("RoomShuffleMuatation");
        int solutionLength = solution.getNumberOfVariables();
        IntegerMatrixSolution<ArrayList<Integer>> tmpSolution = (IntegerMatrixSolution<ArrayList<Integer>>) solution.copy();
//        int numberOfStudents = tmpSolution.getVariable(0).size();

        if ((solutionLength != 0) && (solutionLength != 1)) {
            
            if (mutationRandomGenerator.getRandomValue() < mutationProbability) {
                ArrayList<Integer> tmpRooms = new ArrayList<>();
                tmpRooms.addAll(tmpSolution.getVariable(2));
                Collections.shuffle(tmpRooms);
                tmpSolution.setVariable(2, tmpRooms);
                
//                System.out.println("Exam 0 (Normal): "+solution.getVariable(0));
//                System.out.println("\n\nNEW SOLUTION");
                ////////////////////////////////////////////////////////////////////////////////
//                ArrayList<Integer> studentVector = new ArrayList<>();
//                ArrayList<Integer> regStudentVector = new ArrayList<>();
//                ArrayList<Integer> roomVector = new ArrayList<>();
//                
//                for(int i=0;i<solutionLength;i++){
////                    System.out.println("\tMutating Exam "+i);
//                    studentVector.clear();
//                    regStudentVector.clear();
//                    roomVector.clear();
//                    for(int j=0;j<numberOfStudents;j++){                        
//                        int rm = tmpSolution.getVariable(i).get(j);
//                        if(rm==-1){
//                            studentVector.add(j);
//                        }
//                        else {                            
//                            regStudentVector.add(j);
//                            roomVector.add(rm);
//                        }                        
//                    }
//                    ArrayList<Integer> oldRooms = new ArrayList<>();
//                    oldRooms.addAll(roomVector);
//                    Collections.shuffle(roomVector);
//                    ArrayList<Integer> tmpExam = new ArrayList<>(numberOfStudents);
//                    for(int x=0;x<numberOfStudents;x++)tmpExam.add(-1);
//                    for(int k=0,r=0;k<numberOfStudents;k++){
//                        if(regStudentVector.contains(k)){
////                            System.out.println("\t\t\tStudent "+k+" moved from room "+oldRooms.get(r)+" to "+roomVector.get(r));
//                            tmpExam.set(k, roomVector.get(r));
//                            r++;
//                        }
//                    }
//                    tmpSolution.setVariable(i, tmpExam);
//                }
                ////////////////////////////////////////////////////////////////////////////////
//                System.out.println("Exam 0 (Mutant)= "+tmpSolution.getVariable(0));
            }
        }        
        System.out.println("Mutation Done");
        return (IntegerMatrixSolution<T>)tmpSolution;
    }    
}        


//                
//                
//                ArrayList<ArrayList<Integer>> mySol = new ArrayList(solutionLength);
//                for(int i=0;i<solutionLength;i++)mySol.add(new ArrayList());
//                IntegerMatrixSolution tmpSolution = (IntegerMatrixSolution) solution.copy();                
//                ArrayList exam;                          
////                    System.out.println("usedTimeslots = "+usedTimeslots.toString());
////                    System.out.println("Solution before shuffle="+solution.getVariables());
////                    Collections.shuffle(usedTimeslots);  
//                Collections.shuffle(allTimeslots);
//                for(int slot=0; slot<allTimeslots.size();slot++){    
//                    for(int i=0;i<solutionLength;i++){  
//                        exam = new ArrayList();
//                        exam.addAll((ArrayList)solution.getVariable(i));
//                        if(getTimeslot(exam)==slot){
////                                System.out.println("\nOriginal Exam "+i+":"+solution.getVariable(i));
//                            int room = getRoom(exam);                        
//                            exam.set(slot, -1);                                
////                                exam.set(usedTimeslots.get(slot), room);
//                            exam.set(allTimeslots.get(slot), room);
//                            mySol.set(i,exam);                                
////                                System.out.println("Shuffled Exam "+i+":"+mySol.get(i).toString());
//                        }                                                    
//                    }                        
//                }
//                    System.out.println("\nRESULTS:");                    
//                for(int i=0;i<solutionLength;i++){ 
//                    tmpSolution.setVariable(i, mySol.get(i));
////                        System.out.println("\nsolution  "+i+": "+solution.getVariable(i));
////                        System.out.println("   mySol  "+i+": "+mySol.get(i).toString());
//                }  
//                    System.out.println("Solution after shuffle: "+tmpSolution.getVariables());                                      
//                solution = ;                            
    