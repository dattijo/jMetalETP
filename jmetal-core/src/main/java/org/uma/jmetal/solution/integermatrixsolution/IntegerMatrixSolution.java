package org.uma.jmetal.solution.integermatrixsolution;

/**
 *
 * @author aadatti
 */

import org.uma.jmetal.solution.Solution;
 
/**
 * Interface representing ETP solutions
 *
 * @author aadatti <aadatti.cs@buk.ng>
 * @param <T>
 */
//public interface IntegerMatrixSolution<T> extends Solution<T> {
public interface IntegerMatrixSolution<T> extends Solution<T> {    
    int getLength() ;
}
