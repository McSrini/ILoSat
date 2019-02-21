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
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.ENABLE_EQUIVALENT_CHECK_BCP;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.VariableCoefficientTuple;
import ilog.concert.IloNumVar;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 * 
 * given a trigger, does full BCP on it, and returns the result of the BCP
 * 
 */
public class BCP_Propogator extends AbstractBaseBCP{
    
    public BCP_Propogator (Set<String>  vars,   
                           TreeMap<Integer, List<HyperCube>>  infeasibleHypercubeMap){
        super(vars,    infeasibleHypercubeMap);
    }
    
    
    public List<String> performBCP(){
        
        List<String> candidates = new ArrayList<String>();
        boolean isInfeasible =false;
        
        VariableCoefficientTuple trigger =  getNextTrigger ();     
         
        while (trigger != null){             
            isInfeasible = performBCP (  trigger);
             
            if (isInfeasible){
                candidates.add(trigger.varName);                 
                break;
                // i \\ p\\o.
            }
            trigger =  getNextTrigger ();
        }
         
        
        
        if (! isInfeasible){
            //we must find the best candidates
            
            Set<String> allVarsIn_BCPResultMaps = new HashSet <String> ();
            allVarsIn_BCPResultMaps.addAll( bcpResultMap_OneFix.keySet());
            allVarsIn_BCPResultMaps.addAll( bcpResultMap_ZeroFix.keySet());
            
            double bestKnown_primaryMetric  =ZERO;   
            double bestKnown_secondaryMetric  =ZERO;        
            
            for (String var: allVarsIn_BCPResultMaps){
                double zeroSideVolumeRemoved = ZERO;
                if (bcpResultMap_ZeroFix.containsKey(var)){
                    zeroSideVolumeRemoved = bcpResultMap_ZeroFix.get(var).volumeRemoved_BecauseOfFixings+
                                            bcpResultMap_ZeroFix.get(var).volumeRemoved_BecauseOfMismatch;
                }
                double oneSideVolumeRemoved = ZERO;
                if (bcpResultMap_OneFix.containsKey(var)) {
                    oneSideVolumeRemoved=   bcpResultMap_OneFix.get(var).volumeRemoved_BecauseOfFixings+
                                            bcpResultMap_OneFix.get(var).volumeRemoved_BecauseOfMismatch;
                }
                   
                //note: if entry is missing from map, it is because 
                //it is equivalent to some other entry that is present
                //we do not record who is equivalent to who
                
                //System.out.println("oneSideVolumeRemoved  = "+oneSideVolumeRemoved);
                //System.out.println("zeroSideVolumeRemoved = "+zeroSideVolumeRemoved);
                
                //greedily  prefer the trigger which removes largest volume
                double primaryMetric = Math.max(zeroSideVolumeRemoved , oneSideVolumeRemoved) ;                 
                double secondaryMetric = Math.min(zeroSideVolumeRemoved , oneSideVolumeRemoved) ;                 
                
                if (ZERO > Double.compare(bestKnown_primaryMetric, primaryMetric)){
                    
                    bestKnown_primaryMetric =primaryMetric;
                    bestKnown_secondaryMetric=secondaryMetric;
                    candidates.clear();
                    candidates.add(var);
                    
                }else if (ZERO == Double.compare(bestKnown_primaryMetric,primaryMetric)){
                    //prefer candidate with better secondary metric
                    if (ZERO > Double.compare(bestKnown_secondaryMetric, secondaryMetric)){
                        //System.out.println("ONLY secondaryMetric size "+secondaryMetric);
                        bestKnown_secondaryMetric=secondaryMetric;
                        candidates.clear();
                        candidates.add(var);
                    }else if (ZERO == Double.compare(bestKnown_secondaryMetric,secondaryMetric)){
                        candidates.add(var);     
                    }                                   
                }
                
            }
            
            //System.out.println("bestKnown_primaryMetric  = "+bestKnown_primaryMetric);
            //System.out.println("bestKnown_secondaryMetric = "+bestKnown_secondaryMetric);
            
        } 
                
        //System.out.println("candidates size "+ candidates.size());
        return         candidates;        
    }
        
         
    
    //perform BCP on this trigger  , and return true if infeasibility found
    protected  boolean performBCP (VariableCoefficientTuple inputVariableFixing){
             
        BCP_Result bcpresult= getCascadedVarFixings (  inputVariableFixing,  infeasibleHypercubeMap);

        //record results  
        if (Math.round(inputVariableFixing.coeff) > ZERO)    {
            this.bcpResultMap_OneFix. put (inputVariableFixing.varName , bcpresult);            
        } else{
            bcpResultMap_ZeroFix.put (inputVariableFixing.varName , bcpresult);
        }
        
        //for all the fixings in this BCP result,   equivalent  map entries will have the same or inferior result 
        if (ENABLE_EQUIVALENT_CHECK_BCP && !bcpresult.isInfeasibilityDetected ){
            for (Entry <String, Boolean > entry :bcpresult.varFixingsFound.entrySet()){
                if (inputVariableFixing.varName.equals(entry.getKey()))  continue;

                TreeMap < String, BCP_Result> bcpResultMap_toUse = entry.getValue()? bcpResultMap_OneFix:bcpResultMap_ZeroFix;
                bcpResultMap_toUse.remove(entry.getKey());             
            }
        }
             
        return bcpresult.isInfeasibilityDetected ; 
    }
   
    
    //start from lowest level >=2 , and get all var fixings
    //then get all fixings at next higher level , and so on
    //
    //if any fixings added at higher level, then we must start again from level2 
    //
    protected BCP_Result getCascadedVarFixings (VariableCoefficientTuple inputVariableFixing,
                                            TreeMap<Integer, List<HyperCube>>  infeasibleHypercubeMap) {
        
        BCP_Result finalResult = new BCP_Result();
        
        //make a copy of the input map
        TreeMap<Integer, List<HyperCube>>  infeasibleHypercubeMap_Copy = new TreeMap<Integer, List<HyperCube>>  ();
        for (Map.Entry <Integer, List<HyperCube>> entry : infeasibleHypercubeMap.entrySet()){
            List<HyperCube> copyList =new ArrayList<HyperCube>();
            copyList.addAll(entry.getValue() );
            infeasibleHypercubeMap_Copy.put (entry.getKey(),copyList );
        }
        
        //make a list with given fixed var
        List<VariableCoefficientTuple> inputVariableFixings = new ArrayList<VariableCoefficientTuple> ();
        inputVariableFixings.add(inputVariableFixing );
        
        while (true){
            
            BCP_Result iterationResult = getCascadedVarFixings (  inputVariableFixings,  infeasibleHypercubeMap_Copy);
             
            if (iterationResult.isInfeasibilityDetected){
                finalResult.isInfeasibilityDetected=true; 
                break;
            }else{
                finalResult.merge( iterationResult);
                if (!iterationResult.isReClimbRequired){
                    // we are done, we have worked thru all the levels and found no more fixings
                    break;
                }
            }                
        }
        
        if (! finalResult.isInfeasibilityDetected){
            //we must add the cube volume eliminated because of variable mismatch, although these
            //do not result in any fixings
            addMismatchVolume_EliminatedByFixings ( finalResult) ;
            
            //after spending so much time of finding var fixings and cube eliminations, we must upgrade the 
            //implementtaion so that we do not recalculate these in every child node
            
        }
        
        //logger.debug(" finalResult " + finalResult.printMe() );
        return finalResult;
        
    }
    
    
    //identify var fixings, and any cubes eliminated because of these fixings
    //
    // inputVarFixings = all known var fixings
    //    
    //note : we do not consider cascading effect in this method
    //
    protected BCP_Result getUncascadedVarFixings (List<VariableCoefficientTuple> inputVarFixings, List<HyperCube> remainingCubesAtThisLevel , 
                                               int thisLevel) {
        
        BCP_Result bcpResult = new BCP_Result();
        
        //first, lay the given fixings on given cubes
        //check if this results in any cubes being eliminated, and any vars being fixed
        
        for (HyperCube cube: remainingCubesAtThisLevel){
            HyperCubeVariableFixingResult resultFromCube = cube.getResultOfVarFixings(inputVarFixings);
            if (resultFromCube.isInfeasibilityDetected){
                bcpResult.isInfeasibilityDetected=true;
                break;
            }else if (resultFromCube.isMismatch){
                //no fixings 
            }else if (null !=resultFromCube.fixingList) {
                //check conflict with  fixings found in this round
                //if no conflict then we have found one more fixing                     
                if (!  bcpResult.addFixing(resultFromCube.fixingList.get(ZERO), cube, thisLevel)){
                    bcpResult.isInfeasibilityDetected=true;
                    break;
                }
            } else {
                //no fixing
            }
        }
                
        return bcpResult;
        
    }
    
}