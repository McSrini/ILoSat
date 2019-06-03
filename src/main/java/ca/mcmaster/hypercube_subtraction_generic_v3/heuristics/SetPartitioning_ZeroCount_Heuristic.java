/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.heuristics;

import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.BILLION;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.DOUBLE_ZERO;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.ONE;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.ZERO;
import ca.mcmaster.hypercube_subtraction_generic_v3.Driver;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.HyperCube;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author tamvadss
 * 
 * expects all 0 cubes

 * 
 */
public class SetPartitioning_ZeroCount_Heuristic extends BaseHeuristic{
    
    public List<String> getBranchingVariableSuggestions() {
        
        //smallest cubes
        Set<String> varsInSmallestCubes = new HashSet<String> ();
        List<HyperCube> smallestCubes =this.infeasibleHypercubeMap.firstEntry().getValue();
        for (HyperCube smallCube : smallestCubes)     {
            varsInSmallestCubes.addAll(smallCube.zeroFixingsMap.keySet());
        }
         
        //highest frequency var among smallest cubes
        for (Entry <Integer, List<HyperCube>>  entry :this.infeasibleHypercubeMap.entrySet()){
            
            for (HyperCube hcube : entry.getValue()) {
                
                Set<String> vars = hcube.zeroFixingsMap.keySet();
                 
                for (String var: vars){     
                    
                    if (!  varsInSmallestCubes.contains(var)) continue;
                    
                    Integer currentScore = this.scoreMap_Regular.get(var);
                    if (null==currentScore){  
                        this.scoreMap_Regular.put(var,  ONE);
                    }else {
                        this.scoreMap_Regular.put(var, currentScore+ONE );
                    }
                }
                
            }
        }
        
        //pick vars with highest score
        List<String> candidateVars = new ArrayList<String> ();
        final int maxScore = Collections.max( scoreMap_Regular.values());
        for (Entry <String, Integer>  scoreEntry : scoreMap_Regular.entrySet()){
            if (maxScore == scoreEntry.getValue()){
                candidateVars.add(scoreEntry.getKey()) ;
            }
        }
                
        return candidateVars;          
        
        
    }
    
}
