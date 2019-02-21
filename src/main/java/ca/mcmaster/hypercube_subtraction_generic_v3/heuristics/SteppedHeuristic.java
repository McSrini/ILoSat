/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.heuristics;
  
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.HyperCube;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tamvadss
 * 
 * this class of heuristics examines hypercubes level by level
 * 
 */
public  abstract class SteppedHeuristic extends BaseHeuristic{
     
    
    public SteppedHeuristic(){
        
    }
    
    public   List<String> getBranchingVariableSuggestions (){
        
        List<String> candidateVars = new ArrayList<String> ();
               
        int levelsExamined = ZERO;
        for (Map.Entry <Integer, List<HyperCube>> entry: infeasibleHypercubeMap.entrySet()){
            
            if (levelsExamined>LOOKAHEAD_LEVELS) break;
                        
            List<HyperCube> cubesAtThisLevel = entry.getValue();
            
            for (HyperCube cube: cubesAtThisLevel){
                
                //cube.printMe();
                
                for (String var : cube.zeroFixingsMap.keySet()){
                    
                    if (ZERO==levelsExamined){
                        //every free var is a candidate 
                    }else {
                        //ignore vars that are not already candidates
                        if (!candidateVars.contains(var)) continue;
                    }
                    
                    Integer currentScore =scoreMap_Regular.get(var);
                    if (null==currentScore){
                        scoreMap_Regular.put (var, ONE) ;
                    }else {
                        scoreMap_Regular.put (var, currentScore+ONE) ;
                    }
                }
                for (String var : cube.oneFixingsMap.keySet()){
                    
                    if (ZERO==levelsExamined){
                        //every free var is a candidate 
                    }else {
                        //ignore vars that are not already candidates
                        if (!candidateVars.contains(var)) continue;
                    }
                    
                    Integer currentScore =scoreMap_Regular.get(var);
                    if (null==currentScore){
                        scoreMap_Regular.put (var, ONE) ;
                    }else {
                        scoreMap_Regular.put (var, currentScore+ONE) ;
                    }
                }
            } 
            
            levelsExamined++;
            int maxFreq = Collections.max(this.scoreMap_Regular.values()) ;
            candidateVars.clear();
            for (Map.Entry<String , Integer> scoreEntry  : scoreMap_Regular.entrySet()){
                if (scoreEntry .getValue()==maxFreq){
                    candidateVars.add( scoreEntry.getKey() );
                }
            }
            
            if (ONE==candidateVars.size()){
                //examine no more levels, we have found our candidate
                break;
            } 
            
            
        }//for all levels of the infeasible hypercube map
        
        
        //return vars with highest freq
        //System.out.println( "\n\ncandidateVars size is = "+candidateVars.size()+"\n\n");
        return candidateVars;
    }
    
    
    
}
