/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.common;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class HyperCubeVariableFixingResult {
    
    //with this fix, will node become infeasible? true if all the conditions match
    public boolean isInfeasibilityDetected = false;
    
    //at least 1 variable fixing mismatches, in which case this cube will not contribute to cascaded fixing
    //
    //this logic not applicable to set partitioning cubes
    public boolean isMismatch = false;
    
    //non-null if any fixing actually results
    //set partitioning constraints can result in multiple fixings
    //
    //false indicates 0 fixing
    public TreeMap<String, Boolean> fixingMap=null;
    
    public void printMe (){
        System.out.println("isInfeasibilityDetected "+isInfeasibilityDetected);
        System.out.println("isMismatch "+isMismatch);
        if (fixingMap!=null) {
            for (Entry <String ,Boolean> entry: fixingMap.entrySet()){
               System.out.println("tuple  "+entry.getKey()+ " "+ entry.getValue());
            }
        }
    }
    
}
