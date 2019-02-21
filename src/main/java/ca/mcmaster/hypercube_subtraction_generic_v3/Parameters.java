/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3;

import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*; 
import ca.mcmaster.hypercube_subtraction_generic_v3.bcp.BCP_LEVEL_ENUM;
import ca.mcmaster.hypercube_subtraction_generic_v3.heuristics.BRANCHING_HEURISTIC_ENUM;

/**
 *
 * @author tamvadss
 */
public class Parameters {
    
    //hard MIPS with BCP    
    
    public static final String MIP_FILENAME = "2club200v.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\2club200v.mps";
    
    //public static final String MIP_FILENAME = "bnatt500.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\bnatt500.mps";
    
    //public static final String MIP_FILENAME = "opm2-z12-s8.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\opm2-z12-s8.mps";
        
    //public static final String MIP_FILENAME = "sts405.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\sts405.mps";
    
    //public static final String MIP_FILENAME = "supportcase3.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\supportcase3.mps";
    
    //public static final String MIP_FILENAME = "hanoi5.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\hanoi5.mps";
        
    //public static final String MIP_FILENAME = "f2000.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\f2000.mps";
    
    //set partitioning
    //public static final String MIP_FILENAME = "neos-807456.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\neos-807456.mps";
     

    //public static final String MIP_FILENAME = "seymour-disj-10.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\seymour-disj-10.mps";
    
    
    //public static final String MIP_FILENAME = "reblock354.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\reblock354.mps";
    
    //open MIPS with BCP
    //public static final String MIP_FILENAME = "pythago7824.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\pythago7824.mps";
      
    
    //public static final String MIP_FILENAME = "methanosarcina.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\methanosarcina.mps"; 
    
   
    
    //this mip does not have variables in both polarities 
    //public static final String MIP_FILENAME = "v150d30-2hopcds.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\v150d30-2hopcds.mps";
    
    //public static final String MIP_FILENAME = "p6b.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\p6b.mps";
    
    // ignore capacity seymour-disj-10 reblock354  opms7 opm2-z12-s8 opm14 rmine10 sans capacity constraints
    //set partitioning neos807456  t1722 ds other libraries
     
    //public static final String MIP_FILENAME = "supportcase10.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\supportcase10.mps";
    
    
    // IMPORTNAT TO SOLVE A PROBLEM TO COMPLETION BY IGNORING CAPACITY, SUCH AS OPM14  
   
    //ds and ts1722
    
    //do sorrell8 max and pythago all
    
   
    
    
    //public static final String MIP_FILENAME = "rmine10.mps";
    //public static final String MIP_FILENAME = "F:\\temporary files here\\pythago7824.mps";

    //ivu52  
    
    //barrier - sts405, sorrell8, supportcase10 and 22, opm8 
    
    // supportcase 10 ok with mininfeas, and 22 has a capacity constraint try wiht light bcp and without cuts
    // ignore capacity reblock354  opms7 opm2-z12-s8 opm14 rmine10 sans capacity constraints
    //set partitioning neos807456  t1722 ds other libraries
    //rmine10, rail03,  s1234
    //sorrell8 and 7 , wnq , v150d30 and p6b , sey-disj-10 have all vars same sign, compare with JW
    //competition problems
    //public static final String MIP_FILENAME = "F:\\temporary files here\\knapsack.lp";
     
    
    public static final long PERF_VARIABILITY_RANDOM_SEED = 2;
    public static final java.util.Random  PERF_VARIABILITY_RANDOM_GENERATOR = new  java.util.Random  (PERF_VARIABILITY_RANDOM_SEED);
  
    public static final boolean USE_PURE_CPLEX = false;
   
    //collect the best vertex, and all adjacent vertices, and vertices adajacent to adjacent vertices, and so on
    //set to 0 to collect only the best vertex, and to a large numberto collect all
    public static final int NUM_ADJACENT_VERTICES_TO_COLLECT = BILLION;
    //if too many hypercubes for a constraint, discard them all
    public static final int MAX_HYPERCUBES_PER_CONSTRAINT= BILLION;
    
    public static final boolean MERGE_COLLECTED_HYPERCUBES = true;
    public static final boolean ABSORB_COLLECTED_HYPERCUBES = false;
    
    public static final BRANCHING_HEURISTIC_ENUM HEURISTIC_TO_USE = BRANCHING_HEURISTIC_ENUM.STEPPED_WEIGHT;
        
    public static final int LOOKAHEAD_LEVELS =  BILLION;

    //shuffle constraint during creation (for perf variablity) before arranging by desired order
    public static final boolean SHUFFLE_THE_CONSTRAINTS = false;
    //do you want to sort vars in a given constraint ? 
    public static final boolean SORT_THE_CONSTRAINTS =true;
    //check duplicates?
    public static final boolean CHECK_FOR_DUPLICATES = true;
    //use this parameter to do multiple rounds of collection
    public static final int NUMBER_OF_ADDITIONAL_HYPERCUBE_COLLECTION_ROUNDS = ZERO;
    
    public static final BCP_LEVEL_ENUM USE_BCP_LEVEL = BCP_LEVEL_ENUM.ABOVE_AVG_VARS;
    public static final boolean EXCLUDE_CPLEX_LP_INTEGRAL_VARS =true;
    public static final boolean USE_ONLY_MAX_PSEDUDO_COST_VARS = false;
    
    public static final boolean ENABLE_EQUIVALENT_CHECK_BCP = true;
    
}
