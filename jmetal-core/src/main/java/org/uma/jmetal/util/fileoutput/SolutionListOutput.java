package org.uma.jmetal.util.fileoutput;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** @author Antonio J. Nebro <antonio@lcc.uma.es> */
public class SolutionListOutput {
  private FileOutputContext varFileContext;
  private FileOutputContext varFileContextITC2007;
  private FileOutputContext funFileContext;
  private String varFileName = "VAR";
  private String funFileName = "FUN";
   private String varFileNameITC2007 = "itc2007VAR";
  private List<? extends Solution<?>> solutionList;
  private List<Boolean> isObjectiveToBeMinimized;

  public SolutionListOutput(List<? extends Solution<?>> solutionList) {
    varFileContext = new DefaultFileOutputContext(varFileName);
    varFileContextITC2007 = new DefaultFileOutputContext(varFileNameITC2007);
    funFileContext = new DefaultFileOutputContext(funFileName);
    this.solutionList = solutionList;
    isObjectiveToBeMinimized = null;
  }    

  public SolutionListOutput setVarFileOutputContext(FileOutputContext fileContext) {
    varFileContext = fileContext;
    return this;
  }

  public SolutionListOutput setFunFileOutputContext(FileOutputContext fileContext) {
    funFileContext = fileContext;
    return this;
  }

  public SolutionListOutput setObjectiveMinimizingObjectiveList(
      List<Boolean> isObjectiveToBeMinimized) {
    this.isObjectiveToBeMinimized = isObjectiveToBeMinimized;

    return this;
  }

  public void print() {
    if (isObjectiveToBeMinimized == null) {
      printObjectivesToFile(funFileContext, solutionList);
    } else {
      printObjectivesToFile(funFileContext, solutionList, isObjectiveToBeMinimized);
    }
    printVariablesToFile(varFileContext, solutionList);
    printVariablesToFileITC2007Format(varFileContextITC2007, solutionList);
  }

  public void printVariablesToFile(
      FileOutputContext context, List<? extends Solution<?>> solutionList) {
    BufferedWriter bufferedWriter = context.getFileWriter();

    try {
      if (solutionList.size() > 0) {
        int numberOfVariables = solutionList.get(0).getNumberOfVariables();
        for (int i = 0; i < solutionList.size(); i++) {
          for (int j = 0; j < numberOfVariables - 1; j++) {
            bufferedWriter.write("" + solutionList.get(i).getVariable(j) + context.getSeparator());              
          }
          bufferedWriter.write(                  
              "" + solutionList.get(i).getVariable(numberOfVariables - 1));            

          bufferedWriter.newLine();
        }
      }

      bufferedWriter.close();
    } catch (IOException e) {
      throw new JMetalException("Error writing data ", e);
    }
  }
  
  //------------------>aadatti<-------------------------------------------------------------------------
  public void printVariablesToFileITC2007Format(
      FileOutputContext context, List<? extends Solution<?>> solutionList) {    
    BufferedWriter bufferedWriter = context.getFileWriter();

    try {
      if (solutionList.size() > 0) {
        int numberOfVariables = solutionList.get(0).getNumberOfVariables();
        int t,r;
        for (int i = 0; i < solutionList.size(); i++) {
          for (int j = 0; j < numberOfVariables - 1; j++) {
              t = getTimeslot((ArrayList)solutionList.get(i).getVariable(j));
              r = getRoom((ArrayList)solutionList.get(i).getVariable(j))-1;            
            bufferedWriter.write("" + t + ", "+r+"\r\n"); 
          }
              t = getTimeslot((ArrayList)solutionList.get(i).getVariable(numberOfVariables - 1));
              r = getRoom((ArrayList)solutionList.get(i).getVariable(numberOfVariables - 1))-1;            
              bufferedWriter.write("" + t + ", "+r);           
          
          bufferedWriter.newLine();
        }
      }

      bufferedWriter.close();
    } catch (IOException e) {
      throw new JMetalException("Error writing data ", e);
    }
  }
  //------------------>aadatti<-------------------------------------------------------------------------

  public void printObjectivesToFile(
      FileOutputContext context, List<? extends Solution<?>> solutionList) {
    BufferedWriter bufferedWriter = context.getFileWriter();

    try {
      if (solutionList.size() > 0) {
        int numberOfObjectives = solutionList.get(0).getNumberOfObjectives();
        for (int i = 0; i < solutionList.size(); i++) {
          for (int j = 0; j < numberOfObjectives - 1; j++) {
            bufferedWriter.write(solutionList.get(i).getObjective(j) + context.getSeparator());
          }
          bufferedWriter.write("" + solutionList.get(i).getObjective(numberOfObjectives - 1));

          bufferedWriter.newLine();
        }
      }

      bufferedWriter.close();
    } catch (IOException e) {
      throw new JMetalException("Error printing objectives to file: ", e);
    }
  }

  public void printObjectivesToFile(
      FileOutputContext context,
      List<? extends Solution<?>> solutionList,
      List<Boolean> minimizeObjective) {
    BufferedWriter bufferedWriter = context.getFileWriter();

    try {
      if (solutionList.size() > 0) {
        int numberOfObjectives = solutionList.get(0).getNumberOfObjectives();
        if (numberOfObjectives != minimizeObjective.size()) {
          throw new JMetalException(
              "The size of list minimizeObjective is not correct: " + minimizeObjective.size());
        }
        for (int i = 0; i < solutionList.size(); i++) {
          for (int j = 0; j < numberOfObjectives - 1; j++) {
            if (minimizeObjective.get(j)) {
              bufferedWriter.write(solutionList.get(i).getObjective(j) + context.getSeparator());
            } else {
              bufferedWriter.write(
                  -1.0 * solutionList.get(i).getObjective(j) + context.getSeparator());
            }
          }
          bufferedWriter.write(
              "" + -1.0 * solutionList.get(i).getObjective(numberOfObjectives - 1));

          bufferedWriter.newLine();
        }
      }

      bufferedWriter.close();
    } catch (IOException e) {
      throw new JMetalException("Error printing objecives to file: ", e);
    }
  }

  /*
   * Wrappers for printing with default configuration
   */
  public void printObjectivesToFile(String fileName) throws IOException {
    printObjectivesToFile(new DefaultFileOutputContext(fileName), solutionList);
  }

  public void printObjectivesToFile(String fileName, List<Boolean> minimizeObjective)
      throws IOException {
    printObjectivesToFile(new DefaultFileOutputContext(fileName), solutionList, minimizeObjective);
  }

  public void printVariablesToFile(String fileName) throws IOException {
    printVariablesToFile(new DefaultFileOutputContext(fileName), solutionList);
  }


//---------------->aadatti<---------------------------
    public int getTimeslot(ArrayList<Integer> exam) {
        for (int i = 0; i < exam.size(); i++) {
            if (exam.get(i) != 0) {
                return i;
            }
        }
        return -1;
    }

    public int getRoom(ArrayList<Integer> exam) {
        for (int i = 0; i < exam.size(); i++) {
            if (exam.get(i) != 0) {
                return exam.get(i);
            }
        }
        return -1;
    }
//---------------->aadatti<---------------------------    
 }