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
public class ExamMoveMutation<T> implements MutationOperator<IntegerMatrixSolution<T>> {
   
    Comparator comparator;

    private double mutationProbability;
    private RandomGenerator<Double> mutationRandomGenerator;
    private BoundedRandomGenerator<Integer> positionRandomGenerator;

    int [] roomCapacities;
    int [] examEnrollments;
    int numberOfTimeslots;
    int examsCount;
    boolean timeslotsExhausted;
    boolean earlierTimeslot;
    boolean changeRoom;
    
    ArrayList<Integer> earlierFreeTimeslots;
    ArrayList<Integer> largestExams;
    

    /**
     * Move 1 or more randomly selected exam(s) to random feasible timeslot(s)
     * Constructor
     */
    public ExamMoveMutation(double mutationProbability, int timeslots, int examsCount) {
        this(mutationProbability, () -> JMetalRandom.getInstance().nextDouble(), 
                (a, b) -> JMetalRandom.getInstance().nextInt(a, b), timeslots, examsCount, false, null, false);
    }
    
    /**
     * Move 1 randomly selected exam  to random feasible timeslot and room
     * Constructor
     */
    public ExamMoveMutation(double mutationProbability, int timeslots, boolean changeRoom, int[] roomCapacities, int[] examEnrollments) {
        this(mutationProbability, () -> JMetalRandom.getInstance().nextDouble(), 
                (a, b) -> JMetalRandom.getInstance().nextInt(a, b), timeslots, 1, changeRoom, null, false);
        this.roomCapacities = roomCapacities;
        this.examEnrollments = examEnrollments;
    }
    
    /**
     * Move 1 large exam to random ealier feasible timeslot
     * Constructor
     */
    public ExamMoveMutation(double mutationProbability, int timeslots, ArrayList largestExams, boolean earlierTimeslot) {
        this(mutationProbability, () -> JMetalRandom.getInstance().nextDouble(), 
                (a, b) -> JMetalRandom.getInstance().nextInt(a, b), timeslots, 1, false, largestExams, earlierTimeslot);
    }

    /**
     * Constructor
     */
    public ExamMoveMutation(double mutationProbability, RandomGenerator<Double> mutationRandomGenerator,
            BoundedRandomGenerator<Integer> positionRandomGenerator, int timeslots, int examsCount, boolean changeRoom,
            ArrayList largestExams, boolean earlierTimeslot) {
        Check.probabilityIsValid(mutationProbability);
        this.mutationProbability = mutationProbability;
        this.mutationRandomGenerator = mutationRandomGenerator;
        this.positionRandomGenerator = positionRandomGenerator;
        this.numberOfTimeslots = timeslots;      
        this.examsCount= examsCount;
        this.timeslotsExhausted=false;
        this.earlierTimeslot=earlierTimeslot;
        this.largestExams = largestExams;
        this.changeRoom = changeRoom;
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
                if(earlierTimeslot){
//                    System.out.println("largestExams = "+largestExams.toString());
                    TimeslotShuffleMutation mutation  = new TimeslotShuffleMutation(1, numberOfTimeslots);
                    solution = mutation.execute(solution);
                }
                
                ArrayList<Integer> randExamIndices = new ArrayList<>();
                int i=0;
                
                while(i<examsCount){
                    int rand;
                    if(earlierTimeslot){
                        rand = positionRandomGenerator.getRandomValue(0, largestExams.size()-1);
                        if(randExamIndices.contains(rand))continue;                    
                        randExamIndices.add(largestExams.get(rand));
                    }
                    else{
                        rand = positionRandomGenerator.getRandomValue(0, solutionLength - 1);
                        if(randExamIndices.contains(rand))continue;                    
                        randExamIndices.add(rand);
                    }                                        
                    i++;                    
                }                
                int timeslotCount;
                ArrayList<Integer> freeTimeslots = getFreeTimeslots(solution, randExamIndices.get(0));
                if(earlierTimeslot){
                    timeslotCount = earlierFreeTimeslots.size();
//                    System.out.println("earlierFreeTimeslots="+timeslotCount);
                }
                else{
                    timeslotCount = freeTimeslots.size();
//                    System.out.println("freeTimeslots="+timeslotCount);
                }

                
                ArrayList<Integer> randTimeslotIndices = new ArrayList<>();
                int k=0;
                while(k<examsCount){
                    if(timeslotCount>0){
                        int rand = positionRandomGenerator.getRandomValue(0, timeslotCount - 1);
                        if(randTimeslotIndices.contains(rand))continue;                    
                        randTimeslotIndices.add(rand);
                        k++;     
                        timeslotCount--;
                    }
                    else{
//                        System.out.println("Timeslots exhausted");
                        timeslotsExhausted=true;
                        return solution;
                    }
                }
                
                for(int j=0;j<examsCount;j++){
                    ArrayList exam = (ArrayList)solution.getVariable(randExamIndices.get(j));
//                    System.out.println("TimeslotChangeMutation on exam: "+exam.toString());
                    int oldRoom = getRoom(exam);                    
                    int oldTimeslot = getTimeslot(exam);                    
                    
                    exam.set(oldTimeslot, -1);
                    if(earlierTimeslot){
                        exam.set(earlierFreeTimeslots.get(randTimeslotIndices.get(j)), oldRoom);
                    }
                    else{
                        exam.set(freeTimeslots.get(randTimeslotIndices.get(j)), oldRoom);
                    }
                    
//                    System.out.println("TimeslotChangeMutation Successful: "+exam.toString());
                    solution.setVariable(randExamIndices.get(j), (T)exam);
                    
                    if(changeRoom){
                        MutationOperator mutOp = new RoomMoveMutation(mutationProbability, roomCapacities, examEnrollments, randExamIndices.get(j));
                        mutOp.execute(solution);
                    }
                }
//                System.out.println("------");                
            }
        }        
        return solution;
    }
    
    ArrayList getFreeTimeslots(IntegerMatrixSolution solution, int examIndex){
        ArrayList usedTimeslots = new ArrayList();
        ArrayList freeTimeslots = new ArrayList();
        earlierFreeTimeslots = new ArrayList();
        int exSlot = getTimeslot((ArrayList)solution.getVariable(examIndex));
        if(exSlot==-1) return freeTimeslots;
//        System.out.println("Finding freeTimeslots before "+exSlot+"...");
        for(int i=0; i<solution.getNumberOfVariables();i++){
            int timeslot = getTimeslot((ArrayList)solution.getVariable(i));
            if(timeslot==-1)break;
            if(!usedTimeslots.contains(timeslot)){
                usedTimeslots.add(timeslot);
            }
        }
//        System.out.println("usedTimeslots = "+usedTimeslots.toString());
        
        for(int i=0;i<numberOfTimeslots;i++){
            if(!usedTimeslots.contains(i)){
                freeTimeslots.add(i);
                if(i<exSlot){
                    earlierFreeTimeslots.add(i);
//                    System.out.println(i);
                }
                
            }
        }
//        System.out.println("freeTimeslots = "+freeTimeslots.toString());
//        System.out.println("earlierFreeTimeslots = "+earlierFreeTimeslots.toString());
        return freeTimeslots;        
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
