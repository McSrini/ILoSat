/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.heuristics;
  
import ca.mcmaster.hypercube_subtraction_generic_v3.common.HyperCube;
import ilog.concert.IloNumVar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public abstract class BaseHeuristic {
    
    public  TreeMap<Integer, List<HyperCube>> infeasibleHypercubeMap ;
    
    public Set<String>  variablesToUseForBCP ;
     
    protected TreeMap<String, Integer> scoreMap_Regular = new TreeMap<String, Integer> ();
    //another map for the complemented variable
    protected TreeMap<String, Integer> scoreMap_Complimented = new TreeMap<String, Integer> ();
    
    public BaseHeuristic(  ) {
        
    } 
    public  abstract List<String> getBranchingVariableSuggestions ();
    
    
}
