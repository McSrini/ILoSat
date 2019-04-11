/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.bcp;

import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.DOUBLE_ONE;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.FOUR;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.ONE;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.THREE;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.TWO;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.ZERO;
import ca.mcmaster.hypercube_subtraction_generic_v3.Driver;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Driver.IS_THIS_SET_PARTITIONING;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.HEURISTIC_TO_USE; 
import ca.mcmaster.hypercube_subtraction_generic_v3.common.HyperCube;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.VariableCoefficientTuple; 
import static ca.mcmaster.hypercube_subtraction_generic_v3.heuristics.BRANCHING_HEURISTIC_ENUM.STEPPED_WEIGHT;
import ilog.concert.IloNumVar;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.CONSIDER_PARTLY_MATCHED_CUBES_FOR_BCP_VOLUME_REMOVAL;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.ENABLE_EQUIVALENT_TRIGGER_CHECK_FOR_BCP;

/**
 *
 * @author tamvadss
 */
public abstract class AbstractBaseBCP {
    
    protected TreeMap < String, BCP_Result> bcpResultMap_ZeroFix = new TreeMap < String, BCP_Result>();
    protected TreeMap < String, BCP_Result> bcpResultMap_OneFix  = new TreeMap < String, BCP_Result>();
    protected   TreeMap<Integer, List<HyperCube>>  infeasibleHypercubeMap =null;
    
    public boolean isInfeasibleTriggerFoundDuringBCP = false;
    
    public AbstractBaseBCP (Set<String>  vars,   
                           TreeMap<Integer, List<HyperCube>>  infeasibleHypercubeMap){
        //init maps
        for (String  var: vars) {
            bcpResultMap_ZeroFix.put (var, null);
            bcpResultMap_OneFix.put (var , null);
        }
        this. infeasibleHypercubeMap= infeasibleHypercubeMap;
    }
   
     
    
    public abstract List<String> performBCP(boolean saveVariablePriorityOrder);
    
    
    //perform BCP on this trigger  , and return true if infeasibility found
    //
    //this method moved into base class, so that set partitioning can also use trigger equivalency
    //
    protected  boolean performBCP (VariableCoefficientTuple inputVariableFixing){
             
        BCP_Result bcpresult= getCascadedVarFixings (  inputVariableFixing,  infeasibleHypercubeMap);

        //record results  
        if (Math.round(inputVariableFixing.coeff) > ZERO)    {
            this.bcpResultMap_OneFix. put (inputVariableFixing.varName , bcpresult);            
        } else{
            bcpResultMap_ZeroFix.put (inputVariableFixing.varName , bcpresult);
        }
        
        //for all the fixings in this BCP result,   equivalent  map entries will have the same or inferior result 
        if (ENABLE_EQUIVALENT_TRIGGER_CHECK_FOR_BCP && !bcpresult.isInfeasibilityDetected ){
            
            int removedCount = ZERO;
                    
            for (Map.Entry <String, Boolean > entry :bcpresult.varFixingsFound.entrySet()){
                if (inputVariableFixing.varName.equals(entry.getKey()))  continue;

                TreeMap < String, BCP_Result> bcpResultMap_toUse = entry.getValue()? bcpResultMap_OneFix:bcpResultMap_ZeroFix;
                bcpResultMap_toUse.remove(entry.getKey());      
                
                removedCount++;
            }
            
            //System.out.println("removedCount "+removedCount + " for trigger "+ inputVariableFixing.varName + " val "  +inputVariableFixing.coeff) ;
            
        }
             
        return bcpresult.isInfeasibilityDetected ; 
    }
   
    
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
    
    protected int getNumTriggersRemaining() {
        
        return bcpResultMap_OneFix.size() + bcpResultMap_ZeroFix.size();
    }
    
    protected  VariableCoefficientTuple getNextTrigger (){
        VariableCoefficientTuple result = null;
                
        //System.out.println("triggers left " + (bcpResultMap_OneFix.size()+bcpResultMap_ZeroFix.size()));
        
        //for set partitioning, since we consider vars in size2 hypercubes, 0 and 1 triggers both cause BCP       
        
        if ((Driver.ARE_ALL_VARS_SAME_SIGN>ZERO)    || Driver.IS_THIS_SET_PARTITIONING     ){
            //if all 1 hypercubes, or set partitioning,  use 1 triggers   
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
            
            int thisLevel = entry.getKey();
            
             
            
            //find cubes not eliminated because of fixings
            List<HyperCube> remainingCubesAtThisLevel = new ArrayList<HyperCube>();
            remainingCubesAtThisLevel.addAll(entry.getValue());

            if (null!=finalResult.cubesEliminatedByFixing.get(thisLevel)){
                remainingCubesAtThisLevel.removeAll(finalResult.cubesEliminatedByFixing.get(thisLevel));
            }            
            
            // of these reamining cubes, check which are eliminated because of the var fixings due to mismatch 
            //Note that  these are cubes that did not result in fixings
            for (HyperCube reaminingCube : remainingCubesAtThisLevel){
                if (isEliminatedByMismatch (reaminingCube, finalResult.varFixingsFound) ){
                     
                    double cubeSize = reaminingCube.zeroFixingsMap.size()+ reaminingCube.oneFixingsMap.size();
                    finalResult.volumeRemoved_BecauseOfMismatch+= DOUBLE_ONE/Math.pow(TWO,cubeSize);

                                        
                } else  if (THREE==thisLevel && CONSIDER_PARTLY_MATCHED_CUBES_FOR_BCP_VOLUME_REMOVAL ) {
                    
                    // for SAT problems, we consider 3 size cubes with 1 var fixed
                    if (isOneVariableMatched (  reaminingCube, finalResult.varFixingsFound)) {
                        //removed vol is half of 2^-3
                        finalResult.volumeRemoved_BecauseOfMismatch+= DOUBLE_ONE/Math.pow(TWO, FOUR);
                    }
                }
            }
            
        }
    }
    
    protected boolean isOneVariableMatched (HyperCube reaminingCube, Map<String, Boolean> varFixingsFound  ){
        boolean result = false;
        
        for (Map.Entry <String, Boolean> entry : varFixingsFound.entrySet()){
            String fixedVar = entry.getKey();
            if (entry.getValue()){
                //1 fixning
                if (reaminingCube. oneFixingsMap.keySet().contains( fixedVar)){
                    //match
                    result = true ;
                    break;                    
                }
            }else {
                //0 fixing
                if (reaminingCube.zeroFixingsMap.containsKey(fixedVar )){
                    //match
                    result = true ;
                    break; 
                }
            }
        }
        
         
        return result;
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
