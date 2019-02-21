/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.heuristics; 

import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.HyperCube;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class JerryWangHeuristic extends BaseHeuristic{

    public   List<String> getBranchingVariableSuggestions (){
        
        List<String> candidateVars = new ArrayList<String> ();
        
        int LOWEST_LEVEL=ZERO;
        LOWEST_LEVEL+=infeasibleHypercubeMap.firstKey();
         
        int LEVEL_WEIGHT=ONE;
        
        int previousLevel = -ONE;
         
        for (Map.Entry <Integer, List<HyperCube>> entry: infeasibleHypercubeMap.descendingMap().entrySet()){
            
            int thisLevel = entry.getKey();
            if (thisLevel> TEN+LOWEST_LEVEL){
                continue;
            }
                        
            LEVEL_WEIGHT*= previousLevel<ZERO ? ONE : Math.pow(TWO,-thisLevel+previousLevel) ;
            //System.out.println("thisLevel " + thisLevel + " LEVEL_WEIGHT is = "+LEVEL_WEIGHT) ;
            
            List<HyperCube> cubesAtThisLevel = entry.getValue();
            for (HyperCube cube: cubesAtThisLevel){
                for (String var : cube.zeroFixingsMap.keySet()){
                     
                    
                    Integer currentScore =scoreMap_Regular.get(var);
                    if (null==currentScore){
                        scoreMap_Regular.put (var, LEVEL_WEIGHT) ;
                    }else {
                        scoreMap_Regular.put (var, currentScore+LEVEL_WEIGHT) ;
                    }
                }
                for (String var : cube.oneFixingsMap.keySet()){
                     
                    
                    Integer currentScore =scoreMap_Regular.get(var);
                    if (null==currentScore){
                        scoreMap_Regular.put (var, LEVEL_WEIGHT) ;
                    }else {
                        scoreMap_Regular.put (var, currentScore+LEVEL_WEIGHT) ;
                    }
                }
            } //end inner for    
             
            previousLevel = thisLevel;
                    
        }//end outer for
        
        int maxFreq = Collections.max(scoreMap_Regular.values());
        //return vars with highest freq
        for (Map.Entry<String, Integer> scoreEntry : scoreMap_Regular.entrySet()){
            if (scoreEntry.getValue()==maxFreq){
                candidateVars.add(scoreEntry.getKey() );
            }
        }
                
        //System.out.println( "\n\ncandidateVars size is = "+candidateVars.size()+"\n\n");
        return candidateVars;
    }
    
    
    
}
