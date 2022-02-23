package org.uma.jmetal.operator.mutation.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.util.checking.Check;
import org.uma.jmetal.util.pseudorandom.BoundedRandomGenerator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 * Mutation operator that moves a random exam to a random feasible room within the same time slot
 * @author aadatti
 * @param <T> Solution type 
 */
public class RoomMoveMutation<T> implements MutationOperator<IntegerMatrixSolution<T>>{       
        
    Comparator comparator;
     
    private double mutationProbability;
    private RandomGenerator<Double> mutationRandomGenerator;
    private BoundedRandomGenerator<Integer> positionRandomGenerator;  
    
    Map<Integer, ArrayList<Integer>> availableRooms;
    int[] roomCapacities;
    int[] examEnrollments;
    int randExamIndex = -1;
    
    public RoomMoveMutation(double mutationProbability, int[] roomCapacities, int[] examEnrollments) {        
        this(mutationProbability,
                () -> JMetalRandom.getInstance().nextDouble(), 
                (a, b) -> JMetalRandom.getInstance().nextInt(a, b), 
                roomCapacities, examEnrollments);
    }
    
    public RoomMoveMutation(double mutationProbability, int[] roomCapacities, int[] examEnrollments, int examIndex) {        
        this(mutationProbability,
                () -> JMetalRandom.getInstance().nextDouble(), 
                (a, b) -> JMetalRandom.getInstance().nextInt(a, b), 
                roomCapacities, examEnrollments);
        this.randExamIndex = examIndex;
    }

    /** Constructor
     * @param mutationProbability
     * @param randomGenerator
     * @param availableRooms
     * @param problem */
    public RoomMoveMutation(double mutationProbability, RandomGenerator<Double> randomGenerator, 
            int[] roomCapacities, int[] examEnrollments) 
    {
        this(mutationProbability, randomGenerator, 
                BoundedRandomGenerator.fromDoubleToInteger(randomGenerator), 
                roomCapacities, examEnrollments);
    }

  /** Constructor
     * @param mutationProbability
     * @param mutationRandomGenerator
     * @param positionRandomGenerator
     * @param availableRooms
     * @param problem */
    public RoomMoveMutation(double mutationProbability, RandomGenerator<Double> mutationRandomGenerator, 
            BoundedRandomGenerator<Integer> positionRandomGenerator, int[] roomCapacities, 
            int[] examEnrollments) 
    {
        Check.probabilityIsValid(mutationProbability);
        this.mutationProbability = mutationProbability;
        this.mutationRandomGenerator = mutationRandomGenerator;
        this.positionRandomGenerator = positionRandomGenerator;             
        this.roomCapacities = roomCapacities;
        this.examEnrollments = examEnrollments;           
    }

    @Override
    public double getMutationProbability() 
    {
        return mutationProbability;
    }

    public void setMutationProbability(double mutationProbability) 
    {
        this.mutationProbability = mutationProbability;
    }
    
    @Override
    public IntegerMatrixSolution<T> execute(IntegerMatrixSolution<T> solution){
        Check.isNotNull(solution);        
        return doMutation((IntegerMatrixSolution)solution);
    }
    
    public IntegerMatrixSolution<T> doMutation(IntegerMatrixSolution<T> solution) {
        //PICK A RANDOM EXAM AND A RANDOM ROOM        
        int solutionLength = solution.getNumberOfVariables();        
                
        if ((solutionLength != 0) && (solutionLength != 1)) {
            if (mutationRandomGenerator.getRandomValue() < mutationProbability) {
                if(randExamIndex == -1){
                    randExamIndex = positionRandomGenerator.getRandomValue(0, solutionLength - 1);                  
//                  System.out.println("Changing Room for Exam "+randExamIndex+": "+solution.getVariable(randExamIndex)+"");                     
                }
                else{
                    ArrayList<Integer> availableRooms = getFreeRooms(solution,randExamIndex);
                    ArrayList roomsTried =  new ArrayList();
                    int randRoomIndex = positionRandomGenerator.getRandomValue(0, availableRooms.size()-1);
                    int room = availableRooms.get(randRoomIndex);
    //                System.out.println("Room "+room+" selected out of "+availableRooms.size()+" rooms.");
                    int attempts=0;
                    boolean success=true;
                    while (examEnrollments[randExamIndex] > roomCapacities[randRoomIndex]){     
                        if(attempts>availableRooms.size()){                        
                            success=false;
                            break;
                        }
                        if(!roomsTried.contains(this)){
                            roomsTried.add(room);
                            attempts++;
                        }                    
                        randRoomIndex = positionRandomGenerator.getRandomValue(0, availableRooms.size()-1);         
                    }                                                                                                            
                    if(success){
                        ArrayList<Integer> exam = (ArrayList)solution.getVariable(randExamIndex);                                

                        exam.set(getTimeslot(exam), availableRooms.get(randRoomIndex));
    //                    System.out.println("Room changed succesfully: "+solution.getVariable(randExamIndex));
                        //REPLACE BACK INTO SOLUTION
                        solution.setVariable(randExamIndex, (T)exam);
                    }
                }               
            }
        }        
        return solution;
    }

    ArrayList<Integer> getFreeRooms(IntegerMatrixSolution solution, int examIndex){
       ArrayList<Integer> freeRooms = new ArrayList();       
       
       for(int i=0; i<solution.getNumberOfVariables();i++){
           
           if(!(getTimeslot((ArrayList)solution.getVariable(examIndex))==
                   getTimeslot((ArrayList)solution.getVariable(i)))){
               
               int room  = getRoom((ArrayList)solution.getVariable(i));
               if(freeRooms.contains(room)){
                    continue;
               }
               freeRooms.add(room);
           }
       }
//        System.out.println("Free Rooms:"+freeRooms.toString());
       return freeRooms;
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
}