package org.uma.jmetal.problem.integermatrixproblem.impl;

import java.util.ArrayList;
import org.uma.jmetal.problem.AbstractGenericProblem;
import org.uma.jmetal.problem.integermatrixproblem.IntegerMatrixProblem;
import org.uma.jmetal.solution.integermatrixsolution.IntegerMatrixSolution;
import org.uma.jmetal.solution.integermatrixsolution.impl.DefaultIntegerMatrixSolution;

/**
 *
 * @author PhDLab
 */
@SuppressWarnings("serial")
public abstract class AbstractIntegerMatrixProblem 
        extends AbstractGenericProblem<IntegerMatrixSolution<ArrayList<Integer>>> implements 
        IntegerMatrixProblem<IntegerMatrixSolution<ArrayList<Integer>>> {
        
    
    public abstract ArrayList<Integer> getListOfExamsPerVariable();
    
    @Override
    public int getExamsFromVariable(int index)
    {
        return getListOfExamsPerVariable().get(index);
    }
    
    @Override
    public IntegerMatrixSolution<ArrayList<Integer>> createSolution() {
        return new DefaultIntegerMatrixSolution(getListOfExamsPerVariable(), getNumberOfObjectives()) ;
    }
}