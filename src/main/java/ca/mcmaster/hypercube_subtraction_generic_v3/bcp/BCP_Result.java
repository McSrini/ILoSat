/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.bcp;

import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 * 
 * result of BCP for a trigger
 * 
 * contains resulting variable fixings,   volume of space eliminated, and the actual cubes eliminated
 * 
 * 
 * 
 */
public class BCP_Result {
     
    //fixings found, value false indicates 0 fixing
    public Map<String, Boolean> varFixingsFound = new HashMap<String, Boolean> ();
    
    public Map<Integer, List<HyperCube>  > cubesEliminatedByFixing = new TreeMap<Integer,List<HyperCube>>();
    public double volumeRemoved_BecauseOfFixings = ZERO;
    public double volumeRemoved_BecauseOfMismatch = ZERO;
    
    public boolean isInfeasibilityDetected = false;   
    
    //this flag is used to climb up levels, starting again from level 2, if fixings found at higher levels
    public boolean isReClimbRequired = false;
    
    public boolean addFixingS (List<VariableCoefficientTuple>  varFixingFound_List, HyperCube cubeInWhichFixingsWereFound, 
                              int thisLevel){
        boolean isAdded = true;
        for (VariableCoefficientTuple tuple:  varFixingFound_List){
            isAdded= isAdded && addFixing (   tuple,   cubeInWhichFixingsWereFound,  thisLevel);
            if (!isAdded) break;
        }
        return isAdded ;
    }
    
    //add fixing and return true if added, false if unfeasible
    public boolean addFixing (VariableCoefficientTuple  varFixingFound, HyperCube cubeInWhichFixingWasFound, 
                              int thisLevel){
        
        boolean isConflict = false;
        Boolean existingValue =this.varFixingsFound.get(varFixingFound.varName );
        if (existingValue!=null){
            boolean isConflict_one = ! existingValue && Math.round( varFixingFound.coeff) >ZERO;
            boolean isConflict_two =   existingValue && ! (Math.round( varFixingFound.coeff) >ZERO);
                    
            isConflict =isConflict_one || isConflict_two;
        }
        
        if (!isConflict){
            varFixingsFound.put(varFixingFound.varName, Math.round(varFixingFound.coeff)>ZERO);

            List<HyperCube> existing = cubesEliminatedByFixing.get(thisLevel);
            //we need a flag to check if cube elimintion already noted, because multiple fixings per cube will exist for set partitioning
            boolean wasCubeEliminationAlreadyNoted = true;
            if (existing!=null){
                if (!existing.contains(cubeInWhichFixingWasFound )) {
                    existing.add(cubeInWhichFixingWasFound) ;
                    wasCubeEliminationAlreadyNoted=false;
                }
            }else {
                List<HyperCube> newlist = new ArrayList<HyperCube> ();
                newlist.add(cubeInWhichFixingWasFound );
                cubesEliminatedByFixing.put (thisLevel,newlist ) ;
                wasCubeEliminationAlreadyNoted=false;
            }

            if (!wasCubeEliminationAlreadyNoted){
                double cubeSize = cubeInWhichFixingWasFound.zeroFixingsMap.size()+ cubeInWhichFixingWasFound.oneFixingsMap.size();
                this.volumeRemoved_BecauseOfFixings+= DOUBLE_ONE/Math.pow(TWO,cubeSize);
            }
            
        }
                
        return !isConflict;
 
    }
    
    
    public void merge (BCP_Result other){
        
        //check conflict not needed, because we already have checked conflict between 
        //existing fixings and any new fixings found before calling this method
        
        for ( Map.Entry<String, Boolean> otherFixing : other.varFixingsFound.entrySet()){
            this.varFixingsFound.put (otherFixing.getKey(),otherFixing.getValue());
        }
               
        for (Map.Entry <Integer, List<HyperCube>> entry   : other. cubesEliminatedByFixing.entrySet()){
            
            int thisLevel = entry.getKey();
            List<HyperCube> otherCubesAtThisLevel = entry.getValue();
            
            List<HyperCube> existing = this.cubesEliminatedByFixing.get(thisLevel); 
            if (existing!=null){
                existing.addAll( otherCubesAtThisLevel) ;
            }else {
                List<HyperCube> newlist = new ArrayList<HyperCube> ();
                newlist.addAll(otherCubesAtThisLevel );
                this.cubesEliminatedByFixing.put (thisLevel,newlist ) ;
            }
            
            for (HyperCube otherCube: otherCubesAtThisLevel){
                //update volume and best Obj
                double cubeSize = otherCube.zeroFixingsMap.size()+ otherCube.oneFixingsMap.size();
                this.volumeRemoved_BecauseOfFixings+= DOUBLE_ONE/Math.pow(TWO,cubeSize);
 
            }
        }
                
    }//method merge
     
}
