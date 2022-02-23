package org.uma.jmetal.operator.mutation.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.problem.integermatrixproblem.IntegerMatrixProblem;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.util.checking.Check;
import org.uma.jmetal.util.pseudorandom.BoundedRandomGenerator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;

/**
 *
 * @author aadatti
 */
public class Kinterchange<T> implements MutationOperator<IntegerMatrixSolution<T>> {

//    class localSearchComparator implements Comparator<IntegerMatrixSolution> {
//
//        @Override
//        public int compare(IntegerMatrixSolution a, IntegerMatrixSolution b) {
//            return a.getObjective(0) < b.getObjective(0) ? -1 : a.getObjective(0) == b.getObjective(0) ? 0 : 1;
//        }
//    }

//    private int evaluations;
//    private int improvementRounds;
//    private int numberOfImprovements;
    Comparator comparator;

    private double mutationProbability;
    private RandomGenerator<Double> mutationRandomGenerator;
    private BoundedRandomGenerator<Integer> positionRandomGenerator;
//    private IntegerMatrixProblem problem;

    int[][] conflictMatrix;
    boolean kempeChain;

    public Kinterchange(double mutationProbability, int[][] conMat, IntegerMatrixProblem problem) {
        this(mutationProbability, () -> JMetalRandom.getInstance().nextDouble(), 
                (a, b) -> JMetalRandom.getInstance().nextInt(a, b), conMat, problem, false);
    }
    
    public Kinterchange(double mutationProbability, int[][] conMat, IntegerMatrixProblem problem, boolean kempeChain) {
        this(mutationProbability, () -> JMetalRandom.getInstance().nextDouble(), 
                (a, b) -> JMetalRandom.getInstance().nextInt(a, b), conMat, problem, kempeChain);
    }

    /**
     * Constructor
     * @param mutationProbability
     * @param randomGenerator
     * @param conMat
     * @param problem
     * @param kempeChain
     */
    public Kinterchange(double mutationProbability, RandomGenerator<Double> randomGenerator, int[][] conMat, IntegerMatrixProblem problem, 
            boolean kempeChain) {
        this(mutationProbability, randomGenerator, BoundedRandomGenerator.fromDoubleToInteger(randomGenerator), conMat, problem, kempeChain);
    }

    /**
     * Constructor
     * @param mutationProbability
     * @param mutationRandomGenerator
     * @param positionRandomGenerator
     * @param conMat
     * @param problem
     * @param kempeChain
     */
    public Kinterchange(double mutationProbability, RandomGenerator<Double> mutationRandomGenerator, 
            BoundedRandomGenerator<Integer> positionRandomGenerator, int[][] conMat, IntegerMatrixProblem problem, boolean kempeChain) {
        Check.probabilityIsValid(mutationProbability);
        this.mutationProbability = mutationProbability;
        this.mutationRandomGenerator = mutationRandomGenerator;
        this.positionRandomGenerator = positionRandomGenerator;
        this.conflictMatrix = conMat;
//        this.problem = problem;
//        this.numberOfImprovements = 0;
//        this.improvementRounds = 1000;
//        this.comparator = new localSearchComparator();
        this.kempeChain = kempeChain;
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
//        Check.isNotNull(solution);
//
//        int best;
//        evaluations = 0;
//
//        int rounds = improvementRounds;
//
//        int i = 0;
//        while (i < rounds) {
//            IntegerMatrixSolution mutatedSolution = doMutation((IntegerMatrixSolution) solution.copy());
//
//            problem.evaluate(mutatedSolution);
//            evaluations++;
//
//            best = comparator.compare(mutatedSolution, solution);
//            if (best == -1) {
//                solution = mutatedSolution;
//                numberOfImprovements++;
//            } else if (best == 0) {
//                if (mutationRandomGenerator.getRandomValue() < 0.5) {
//                    solution = mutatedSolution;
//                }
//            }
//            i++;
//        }
//
//        return solution;
    }

    public IntegerMatrixSolution<T> doMutation(IntegerMatrixSolution<T> solution) {
        //PICK TWO RANDOM EXAMS TO SWAP
        int solutionLength = solution.getNumberOfVariables();
        Map<Integer, ArrayList<Integer>> solutionMap = new HashMap<>();

        for (int i = 0; i < solutionLength; i++) {
            solutionMap.put(i, new ArrayList((ArrayList) solution.getVariable(i)));
        }

        if ((solutionLength != 0) && (solutionLength != 1)) {
            if (mutationRandomGenerator.getRandomValue() < mutationProbability) {
                int pos1 = positionRandomGenerator.getRandomValue(0, solutionLength - 1);
                int pos2 = positionRandomGenerator.getRandomValue(0, solutionLength - 1);

                while (pos1 == pos2) {
                    if (pos1 == (solutionLength - 1)) {
                        pos2 = positionRandomGenerator.getRandomValue(0, solutionLength - 2);
                    } else {
                        pos2 = positionRandomGenerator.getRandomValue(pos1, solutionLength - 1);
                    }
                }

                ArrayList exam1 = solutionMap.get(pos1);//System.out.println("exam1: "+exam1);
                ArrayList exam2 = solutionMap.get(pos2);//System.out.println("exam2: "+exam2);

                //COLLECT ALL EXAMS HAVING THE SAME TIMESLOT WITH EACH OF THE TWO RANDOMLY SELECTED EXAMS
                Map<Integer, ArrayList<Integer>> mapTi = new HashMap<>();
                Map<Integer, ArrayList<Integer>> mapTj = new HashMap<>();

                int timeslot1 = getTimeslot(exam1);
                int timeslot2 = getTimeslot(exam2);

                while (timeslot1 == timeslot2) {
                    Random rand = new Random();
                    pos2 = rand.nextInt(solutionMap.get(0).size());

                    exam2 = solutionMap.get(pos2);

                    for (timeslot2 = 0; timeslot2 < exam2.size(); timeslot2++) {
                        if ((int) exam2.get(timeslot2) != 0) {
                            break;
                        }
                    }
                }//System.out.println("re:timeslot2="+timeslot2);

                //simple timeslot swap
                if(kempeChain){                    
                    for (int i = 0; i < solutionMap.size(); i++) {
                        if (solutionMap.get(i).get(timeslot1) != 0) {
                            mapTi.put(i, solutionMap.get(i));
                        } else if (solutionMap.get(i).get(timeslot2) != 0) {
                            mapTj.put(i, solutionMap.get(i));
                        }
                    }//System.out.println("mapTi: "+mapTi+"\nmapTj: "+mapTj);

                    //FROM ABOVE, COLLECT THE ONES HAVING CONFLICTS
                    Map<Integer, ArrayList<Integer>> newMapTi = new HashMap<>();
                    Map<Integer, ArrayList<Integer>> newMapTj = new HashMap<>();

                    ArrayList tiKeys = new ArrayList(mapTi.keySet());//System.out.println("tiKeys:"+tiKeys);
                    ArrayList tjKeys = new ArrayList(mapTj.keySet());//System.out.println("tjKeys:"+tjKeys);

                    for (int i = 0; i < tiKeys.size(); i++) {
                        for (int j = 0; j < tjKeys.size(); j++) {
                            int x = (int) tiKeys.get(i);
                            int y = (int) tjKeys.get(j);

                            if (conflictMatrix[x][y] != 0) {
                                newMapTi.put(x, mapTi.get(x));
                                newMapTj.put(y, mapTj.get(y));
                            }
                        }
                    }//System.out.println("newMapTi:"+newMapTi+"\nnewMapTj:"+newMapTj);

                    //KINTERCHANGE 
                    Set K = new HashSet();

                    K.addAll(newMapTi.keySet());
                    K.addAll(newMapTj.keySet());
                    //System.out.println("K:"+K);

                    Set ti_complement_K = new HashSet<>(newMapTi.keySet());
                    Set tj_intersection_K = new HashSet<>(newMapTj.keySet());
                    ti_complement_K.removeAll(K);
                    tj_intersection_K.retainAll(K);
                    Set newTi = new HashSet<>(ti_complement_K);
                    newTi.addAll(tj_intersection_K);
                    //System.out.println("newTi"+newTi);

                    Set tj_complement_K = new HashSet<>(newMapTj.keySet());
                    Set ti_intersection_K = new HashSet<>(newMapTi.keySet());
                    tj_complement_K.removeAll(K);
                    ti_intersection_K.retainAll(K);
                    Set newTj = new HashSet<>(tj_complement_K);
                    newTj.addAll(ti_intersection_K);
    //                System.out.println("newTj"+newTj);

                    //...AND SWAP TIMESLOTS
                    ArrayList tiArray = new ArrayList(newTi);
                    ArrayList tjArray = new ArrayList(newTj);

                    for (int i = 0; i < tiArray.size(); i++) {
                        int x = (int) tiArray.get(i);
                        int oldRoom = solutionMap.get(x).get(timeslot2);
                        solutionMap.get(x).set(timeslot2, 0);
                        solutionMap.get(x).set(timeslot1, oldRoom);
    //                    System.out.println("roomSwap"+solutionMap.get(x)); 
                    }

                    for (int i = 0; i < tjArray.size(); i++) {
                        int x = (int) tjArray.get(i);
                        int oldRoom = solutionMap.get(x).get(timeslot1);
                        solutionMap.get(x).set(timeslot1, 0);
                        solutionMap.get(x).set(timeslot2, oldRoom);
    //                    System.out.println("roomSwap"+solutionMap.get(x));
                    }

                    //REPLACE BACK INTO SOLUTION
                    for (int i = 0; i < solutionLength; i++) {
                        solution.setVariable(i, (T) solutionMap.get(i));
                    }
                }
                else{
                    IntegerMatrixSolution tmpSolution = (IntegerMatrixSolution) solution.copy();
                    int room1 = getRoom(exam1);
                    exam1.set(timeslot1, -1);
                    exam1.set(timeslot2, room1);
                    tmpSolution.setVariable(pos1, (T)exam1);
                    
                    int room2 = getRoom(exam2);
                    exam2.set(timeslot2, -1);
                    exam2.set(timeslot1, room2);                    
                    tmpSolution.setVariable(pos2, (T)exam2);
                    
                    if(isFeasible(solution)){
                        return tmpSolution;
                    }else {
//                        return solution;
                        return null;
                    }                    
                }                                
            }
        }
        return (IntegerMatrixSolution) solution.copy();
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
    
    public boolean isFeasible(IntegerMatrixSolution solution){
        int solutionLength = solution.getNumberOfVariables();
        for(int i=0;i<solutionLength;i++){
            int slot1 = getTimeslot((ArrayList)solution.getVariable(i));
            for(int j=0;j<solutionLength;j++){
                int slot2 = getTimeslot((ArrayList)solution.getVariable(j));
                if(slot1==slot2&&conflictMatrix[i][j]!=0){
                    return false;
                }
            }
        }
        return true;
    }
}
