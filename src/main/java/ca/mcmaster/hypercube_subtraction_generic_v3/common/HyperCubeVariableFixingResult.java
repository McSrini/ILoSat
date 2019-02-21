/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.common;

import java.util.List;

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
    public List<VariableCoefficientTuple> fixingList=null;
    
    public void printMe (){
        System.out.println("isInfeasibilityDetected "+isInfeasibilityDetected);
        System.out.println("isMismatch "+isMismatch);
        if (fixingList!=null) {
            for (VariableCoefficientTuple tuple: fixingList){
               System.out.println("tuple  "+tuple.varName+ " "+ tuple.coeff);
            }
        }
    }
    
}
