/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.heuristics;

import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.ONE;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.HyperCube;
import java.util.ArrayList;
import java.util.Collections;
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
        
        List<String> candidateVars = new ArrayList<String> ();
        
        for (Entry <Integer, List<HyperCube>>  entry :this.infeasibleHypercubeMap.entrySet()){
            
            for (HyperCube hcube : entry.getValue()) {
                
                Set<String> vars = hcube.zeroFixingsMap.keySet();
                int size = vars.size();
                
                for (String var: vars){
                    Integer currentScore = this.scoreMap_Regular.get(var);
                    if (null==currentScore){
                        this.scoreMap_Regular.put(var, size -ONE);
                    }else {
                        this.scoreMap_Regular.put(var, currentScore + size -ONE);
                    }
                }
                
            }
        }
        
        //pick vars with highest score
        final int maxScore = Collections.max( scoreMap_Regular.values());
        for (Entry <String, Integer>  scoreEntry : scoreMap_Regular.entrySet()){
            if (maxScore == scoreEntry.getValue()){
                candidateVars.add(scoreEntry.getKey()) ;
            }
        }
        
        return candidateVars;        
        
    }
    
}
