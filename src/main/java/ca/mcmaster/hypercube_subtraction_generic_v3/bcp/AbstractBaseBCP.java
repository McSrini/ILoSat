/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.bcp;

import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.DOUBLE_ONE;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.ONE;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.TWO;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.ZERO;
import ca.mcmaster.hypercube_subtraction_generic_v3.Driver;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.HEURISTIC_TO_USE; 
import ca.mcmaster.hypercube_subtraction_generic_v3.common.HyperCube;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.VariableCoefficientTuple;
import static ca.mcmaster.hypercube_subtraction_generic_v3.heuristics.BRANCHING_HEURISTIC_ENUM.SET_PARTITIONING;
import static ca.mcmaster.hypercube_subtraction_generic_v3.heuristics.BRANCHING_HEURISTIC_ENUM.STEPPED_WEIGHT;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public abstract class AbstractBaseBCP {
    
    protected TreeMap < String, BCP_Result> bcpResultMap_ZeroFix = new TreeMap < String, BCP_Result>();
    protected TreeMap < String, BCP_Result> bcpResultMap_OneFix  = new TreeMap < String, BCP_Result>();
    protected   TreeMap<Integer, List<HyperCube>>  infeasibleHypercubeMap =null;
    
    public AbstractBaseBCP (Set<String>  vars,   
                           TreeMap<Integer, List<HyperCube>>  infeasibleHypercubeMap){
        //init maps
        for (String  var: vars) {
            bcpResultMap_ZeroFix.put (var, null);
            bcpResultMap_OneFix.put (var , null);
        }
        this. infeasibleHypercubeMap= infeasibleHypercubeMap;
    }
    
    public abstract List<String> performBCP();
    
    protected abstract BCP_Result getCascadedVarFixings (VariableCoefficientTuple inputVariableFixing,
                                            TreeMap<Integer, List<HyperCube>>  infeasibleHypercubeMap) ;
        
    
    
    //given an initial set of var fixings, get more fixings at all levels 
    //stop climbing up if more fixings found at a higher level (i.e. we will start again from level 2)
    //
    //infeasibleHypercubeMap_Copy has all the non-eliminated cubes
    protected BCP_Result getCascadedVarFixings (List<VariableCoefficientTuple> inputVariableFixings,
                                            TreeMap<Integer, List<HyperCube>>  infeasibleHypercubeMap_Copy) {
        
        BCP_Result cumulativeResult =new BCP_Result();
        
        for (Map.Entry <Integer, List<HyperCube>> entry : infeasibleHypercubeMap_Copy.entrySet()){
            
            if (Driver.ARE_ALL_VARS_SAME_SIGN>=ZERO && entry.getKey()>TWO &&  STEPPED_WEIGHT.equals( HEURISTIC_TO_USE)){
                //we only do bcp at level 2 
                break;
            }
            
            int numFixingsBefore= inputVariableFixings.size();
            BCP_Result iterationBcpResult  = getCascadedVarFixings (  inputVariableFixings, entry.getValue(), entry.getKey()) ;
            int numFixingsAfter= inputVariableFixings.size();
            
            if (iterationBcpResult.isInfeasibilityDetected){
                //if infeasible trigger,  note infeasibility and exit                 
                cumulativeResult.isInfeasibilityDetected=true;                 
                break;
            }else  {
                
                cumulativeResult.merge( iterationBcpResult);
                
                if (numFixingsAfter>numFixingsBefore && entry.getKey()>TWO){
                    cumulativeResult.isReClimbRequired=true;
                    break;
                }
            }//end if else
            
        }//end for
       
        //logger.debug(" reclimb break bcpresult" + cumulativeResult.printMe() );
        return cumulativeResult;
    }
    
    
    
    
    //tuos5uy8w5u098wun08u7h65u387h087y98fgwiu5uwujy95u0uyw5u09y90u6iu7uep6u9u78u8786u87u69u7986u78u6087u8u88hujyj6u89u978u687u38u7968u   //get cascaded var fixings, till cascading stops 
    //note that this cascading is only at a given level
    //
    //warning, both input lists get mangled
    //
    //returns: all fixings and eliminated hypercubes at this level, for the initial inputVariableFixings and remainingCubesAtThisLevel
    //
    protected BCP_Result getCascadedVarFixings (List<VariableCoefficientTuple> inputVariableFixings, List<HyperCube> remainingCubesAtThisLevel, 
                                             int thisLevel) {
             
        BCP_Result cumulativeResult = new BCP_Result ();
        
        BCP_Result iterationResult = getUncascadedVarFixings( inputVariableFixings, remainingCubesAtThisLevel, thisLevel);
        while (! iterationResult.isInfeasibilityDetected && iterationResult.varFixingsFound.size()!=ZERO){            
            
            //check if inputVariableFixings include any conflicting tuple
            if (augmentVariableFixings(inputVariableFixings,iterationResult.varFixingsFound )){
                cumulativeResult.isInfeasibilityDetected=true;
                break;
            }else {
                remainingCubesAtThisLevel.removeAll( iterationResult.cubesEliminatedByFixing.get( thisLevel));

                //record results of this iteration
                cumulativeResult .merge ( iterationResult) ;

                //another iteration
                iterationResult = getUncascadedVarFixings( inputVariableFixings, remainingCubesAtThisLevel, thisLevel);            
                
            } //end if else           
        }//end while
         
        //logger.debug(" Cascaded bcpresult" + cumulativeResult.printMe() );
        if (iterationResult.isInfeasibilityDetected) cumulativeResult.isInfeasibilityDetected=true;
         
        return cumulativeResult;
         
    }
    
     
    //only the uncascaded var fixings at a given level are implemented differntly fo each kind of BCP propogator
    protected abstract BCP_Result getUncascadedVarFixings (List<VariableCoefficientTuple> inputVarFixings, List<HyperCube> remainingCubesAtThisLevel , 
                                               int thisLevel) ;
    
    //perform BCP for a fixing is different in only that equivalency does not exist for set partitioning
    protected abstract boolean performBCP (VariableCoefficientTuple inputVariableFixing);
    
    
    //augments current var fixings with new fixings found, and return true if conflict detected
    protected boolean augmentVariableFixings(List<VariableCoefficientTuple> existingVariableFixings, 
                                           Map<String, Boolean>    newFixings){
        boolean isConflictFound = false;
        
        List<VariableCoefficientTuple> augmentedVariableFixings = new ArrayList<VariableCoefficientTuple>();
        
        for (Map.Entry<String,Boolean> newFix : newFixings.entrySet()){
            for (VariableCoefficientTuple existing: existingVariableFixings){            
                if (existing.varName.equals(newFix.getKey())){
                    boolean clashConditionOne =  (Math.round(existing.coeff)>ZERO) && !newFix.getValue();
                    boolean clashConditionTwo =   newFix.getValue() && !(Math.round(existing.coeff)>ZERO) ;
                    if (clashConditionOne||clashConditionTwo){
                        isConflictFound = true;
                        break;
                    }
                }
            }
            if (isConflictFound) {
                break;
            }else {
                //fix to be added to existing fixings
                augmentedVariableFixings.add (new VariableCoefficientTuple(newFix.getKey(), newFix.getValue() ? ONE : ZERO) );
            }
        }
        
        existingVariableFixings.addAll(augmentedVariableFixings );
        return isConflictFound;
    }
    
    
    protected  VariableCoefficientTuple getNextTrigger (){
        VariableCoefficientTuple result = null;
                
        //System.out.println("triggers left " + (bcpResultMap_OneFix.size()+bcpResultMap_ZeroFix.size()));
        
        if ((Driver.ARE_ALL_VARS_SAME_SIGN>ZERO) || HEURISTIC_TO_USE.equals( SET_PARTITIONING)){
            //all 1 hypercubes, use 1 triggers   
            //check if any pending triggers in the 1 fix map
            for (Map.Entry < String, BCP_Result> entry : bcpResultMap_OneFix.entrySet()){
                if (null==entry.getValue()){
                    result = new VariableCoefficientTuple(entry.getKey(), ONE) ;
                    break;
                }
            }
            if (result==null){
                for (Map.Entry < String, BCP_Result> entry : bcpResultMap_ZeroFix.entrySet()){
                    if (null==entry.getValue()){
                        result = new VariableCoefficientTuple(entry.getKey(), ZERO) ;
                        break;
                    }
                }
            }
        }else {
            for (Map.Entry < String, BCP_Result> entry : bcpResultMap_ZeroFix.entrySet()){
                if (null==entry.getValue()){
                    result = new VariableCoefficientTuple(entry.getKey(), ZERO) ;
                    break;
                }
            }

            if (result==null){
                //check if any pending triggers in the 1 fix map
                for (Map.Entry < String, BCP_Result> entry : bcpResultMap_OneFix.entrySet()){
                    if (null==entry.getValue()){
                        result = new VariableCoefficientTuple(entry.getKey(), ONE) ;
                        break;
                    }
                }        
            }
        }
        
        return result;
    }
    
    
    protected void addMismatchVolume_EliminatedByFixings (  BCP_Result finalResult) {
        for (Map.Entry < Integer,List<HyperCube>> entry : this.infeasibleHypercubeMap.entrySet()){
            //find cubes not eliminated because of fixings
            List<HyperCube> remainingCubesAtThisLevel = new ArrayList<HyperCube>();
            remainingCubesAtThisLevel.addAll(entry.getValue());

            if (null!=finalResult.cubesEliminatedByFixing.get(entry.getKey())){
                remainingCubesAtThisLevel.removeAll(finalResult.cubesEliminatedByFixing.get(entry.getKey()));
            }            
            
            // of these reamining cubes, check which are eliminated because of the var fixings due to mismatch 
            //Note that  these are cubes that did not result in fixings
            for (HyperCube reaminingCube : remainingCubesAtThisLevel){
                if (isEliminatedByMismatch (reaminingCube, finalResult.varFixingsFound) ){
                    double cubeSize = reaminingCube.zeroFixingsMap.size()+ reaminingCube.oneFixingsMap.size();
                    finalResult.volumeRemoved_BecauseOfMismatch+= DOUBLE_ONE/Math.pow(TWO,cubeSize);
                }
            }
            
        }
    }
    
    protected boolean isEliminatedByMismatch (HyperCube reaminingCube, Map<String, Boolean> varFixingsFound  ){
        boolean result = false;
        
        for (Map.Entry <String, Boolean> entry : varFixingsFound.entrySet()){
            String fixedVar = entry.getKey();
            if (entry.getValue()){
                //1 fixning
                if (reaminingCube.zeroFixingsMap.keySet().contains( fixedVar)){
                    //mismatch
                    result = true ;
                    break;                    
                }
            }else {
                //0 fixing
                if (reaminingCube.oneFixingsMap.containsKey(fixedVar )){
                    //mismatch
                    result = true ;
                    break; 
                }
            }
        }
        
        
        return result;
    }
    
}
