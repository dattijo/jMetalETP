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
 *
 * @author aadatti
 */
public class RoomSwapMutation<T> implements MutationOperator<IntegerMatrixSolution<T>> {
    
    Comparator comparator;

    private double mutationProbability;
    private RandomGenerator<Double> mutationRandomGenerator;
    private BoundedRandomGenerator<Integer> positionRandomGenerator;

    Map<Integer, ArrayList<Integer>> availableRooms;
    int[] roomCapacities;
    int[] examEnrollments;
    boolean cannotMutate = false;

    public RoomSwapMutation(double mutationProbability, int[] roomCapacities, int[] examEnrollments) {
        this(mutationProbability,
                () -> JMetalRandom.getInstance().nextDouble(),
                (a, b) -> JMetalRandom.getInstance().nextInt(a, b),
                roomCapacities, examEnrollments);
    }

    /**
     * Constructor
     *
     * @param mutationProbability
     * @param randomGenerator
     * @param availableRooms
     * @param problem
     */
    public RoomSwapMutation(double mutationProbability, RandomGenerator<Double> randomGenerator,
            int[] roomCapacities, int[] examEnrollments) {
        this(mutationProbability, randomGenerator,
                BoundedRandomGenerator.fromDoubleToInteger(randomGenerator),
                roomCapacities, examEnrollments);
    }

    /**
     * Constructor
     *
     * @param mutationProbability
     * @param mutationRandomGenerator
     * @param positionRandomGenerator
     * @param availableRooms
     * @param problem
     */
    public RoomSwapMutation(double mutationProbability, RandomGenerator<Double> mutationRandomGenerator,
            BoundedRandomGenerator<Integer> positionRandomGenerator, int[] roomCapacities,
            int[] examEnrollments) {
        Check.probabilityIsValid(mutationProbability);
        this.mutationProbability = mutationProbability;
        this.mutationRandomGenerator = mutationRandomGenerator;
        this.positionRandomGenerator = positionRandomGenerator;
        this.roomCapacities = roomCapacities;
        this.examEnrollments = examEnrollments;                
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
        return doMutation(solution);
    }

    public IntegerMatrixSolution<T> doMutation(IntegerMatrixSolution<T> solution) {
        //PICK A RANDOM EXAM AND A RANDOM ROOM        
        int solutionLength = solution.getNumberOfVariables();

        if ((solutionLength != 0) && (solutionLength != 1)) {
            if (mutationRandomGenerator.getRandomValue() < mutationProbability) {
                int randExamIndex1 = positionRandomGenerator.getRandomValue(0, solutionLength - 1);
                int randExamIndex2 = positionRandomGenerator.getRandomValue(0, solutionLength - 1);

                while (randExamIndex1 == randExamIndex2) {
                    if (randExamIndex1 == (solutionLength - 1)) {
                        randExamIndex2 = positionRandomGenerator.getRandomValue(0, solutionLength - 2);
                    } else {
                        randExamIndex2 = positionRandomGenerator.getRandomValue(randExamIndex1, solutionLength - 1);
                    }
                }
                int room1 = getRoom((ArrayList) solution.getVariable(randExamIndex1));
                int room2 = getRoom((ArrayList) solution.getVariable(randExamIndex2));
                int timeslot1 = getTimeslot((ArrayList) solution.getVariable(randExamIndex1));
                int timeslot2 = getTimeslot((ArrayList) solution.getVariable(randExamIndex2));
//                System.out.println("Selected Exams:"+randExamIndex1+" and "+randExamIndex2);
//                System.out.println("Attempting to swap rooms "+room1+" and "+ room2);
                boolean feasible = false;
                if (examEnrollments[randExamIndex1]
                        <= roomCapacities[room2 - 1]
                        && examEnrollments[randExamIndex2]
                        <= roomCapacities[room1 - 1]) {

                    for (int i = 0; i < solution.getNumberOfVariables(); i++) {
                        int tmpTimeslot = getTimeslot((ArrayList) solution.getVariable(i));
                        int tmpRoom = getRoom((ArrayList) solution.getVariable(i));
                        if (tmpTimeslot == timeslot1 && tmpRoom == room2) {
//                            feasible = false;
                            break;
                        }
                        if (tmpTimeslot == timeslot2 && tmpRoom == room1) {
//                            feasible = false;
                            break;
                        }
                    }
                    feasible = true;
                    System.out.println("Feasible move found");
                } 
                if (feasible) {
                    System.out.println("Selected Exams:");
                    System.out.println("\tExam 1 = " + randExamIndex1 + ". Timeslot = " + timeslot1 + ". Room = " + room1);
                    System.out.println("\tExam 2 = " + randExamIndex2 + ". Timeslot = " + timeslot2 + ". Room = " + room2);
                    ArrayList<Integer> exam1 = (ArrayList) solution.getVariable(randExamIndex1);
                    ArrayList<Integer> exam2 = (ArrayList) solution.getVariable(randExamIndex2);
                    exam1.set(timeslot1, room2);
                    exam2.set(timeslot2, room1);
                    solution.setVariable(randExamIndex1, (T) exam1);
                    solution.setVariable(randExamIndex2, (T) exam2);
                    System.out.println("New exam1 = " + solution.getVariable(randExamIndex1));
                    System.out.println("New exam2 = " + solution.getVariable(randExamIndex2));
                    cannotMutate = false;
                } else {
                    cannotMutate = true;
                }
                System.out.println("feasible = "+feasible);
            }
        }        
        return solution;
    }

//    ArrayList<Integer> getFreeRooms(IntegerMatrixSolution solution, int examIndex){
//       ArrayList<Integer> freeRooms = new ArrayList();
//       
//       for(int i=0; i<solution.getNumberOfVariables();i++){
//           
//           if(!(getTimeslot((ArrayList)solution.getVariable(examIndex))==
//                   getTimeslot((ArrayList)solution.getVariable(i)))){
//               int room  = getRoom((ArrayList)solution.getVariable(i));
//               if(freeRooms.contains(room)){
//                    continue;
//               }
//               freeRooms.add(room);
//           }
//       }
////        System.out.println("Free Rooms:"+freeRooms.toString());
//       return freeRooms;
//    }    
    public int getTimeslot(ArrayList<Integer> exam) {
        for (int i = 0; i < exam.size(); i++) {
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
