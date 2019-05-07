/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.heuristics; 
  
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.HEURISTIC_TO_USE;
import static ca.mcmaster.hypercube_subtraction_generic_v3.heuristics.BRANCHING_HEURISTIC_ENUM.JERRY_WANG;
import java.util.List;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class BranchingHeuristicFactory {
    
    public static BaseHeuristic getBranchingHeuristic (  ){
        BaseHeuristic heuristic = null;
        
        if (HEURISTIC_TO_USE.equals(BRANCHING_HEURISTIC_ENUM.SET_PARTITIONING_HEURISTIC)){
            heuristic = new  SetPartitioning_ZeroCount_Heuristic ();
        }else     if (JERRY_WANG.equals(HEURISTIC_TO_USE) ){
            heuristic = new  JerryWangHeuristic();
        }  else if (HEURISTIC_TO_USE.equals(BRANCHING_HEURISTIC_ENUM.STEPPED_WEIGHT)) {
            heuristic = new SteppedWeightHeuristic();
        }  
        return heuristic;
    }
    
}

 