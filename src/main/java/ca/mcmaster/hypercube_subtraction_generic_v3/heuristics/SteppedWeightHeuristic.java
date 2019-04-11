/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.heuristics; 

import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.Driver;
import ca.mcmaster.hypercube_subtraction_generic_v3.bcp.BCP_Propogator;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.HyperCube;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntegerFeasibilityStatus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class SteppedWeightHeuristic extends SteppedHeuristic{
    
    //set this flag to save static priority order of branching varaibles
    public boolean isStaticPriorityOrderWanted= false;
    
    public   List<String> getBranchingVariableSuggestions ( ){
        
        List<String> candidateVars = new ArrayList<String> ();
        
        if ( ONE==infeasibleHypercubeMap.firstKey()){
            //pick any var 
            HyperCube someCube = infeasibleHypercubeMap.firstEntry().getValue().get(ZERO);
            candidateVars.addAll(someCube.zeroFixingsMap.keySet());
            candidateVars.addAll(someCube.oneFixingsMap.keySet());
             
        }else  {
            //if any cubes exists at level 2 , we do BCP 
            boolean noVarsForBCP = (variablesToUseForBCP==null);
            if (!noVarsForBCP)  noVarsForBCP = (variablesToUseForBCP.size()==ZERO);
            if ( TWO==infeasibleHypercubeMap.firstKey() &&  !noVarsForBCP){
                //cater for BCP  
                
                //System.out.println("variablesToUseForBCP "+ variablesToUseForBCP.size()) ;
                
                //okay use full BCP , but improvements can be made so we do not have to recalulate eliminated
                //hypercubes in each child node
                candidateVars= 
                            selectCandidatesWithBest_BCP(this.variablesToUseForBCP, 
                                    infeasibleHypercubeMap);
                 
            }else {
                //simply use stepped weight as defined in my parent class
                candidateVars=super.getBranchingVariableSuggestions( );
                 
            }
        }
        
        return   candidateVars ;
        
    }
    

    
   
    
    private List<String> selectCandidatesWithBest_BCP (Set<String>  vars, 
            TreeMap<Integer, List<HyperCube>> filterResult  ){
        BCP_Propogator propogator = new BCP_Propogator (vars, filterResult);
        List<String>  result = propogator.performBCP(isStaticPriorityOrderWanted);
        
        this.wasInfeasibilityDetectedDuringBCP=propogator.isInfeasibleTriggerFoundDuringBCP;
        if (isStaticPriorityOrderWanted && ! wasInfeasibilityDetectedDuringBCP){
            //save the priority order
            this.variablesInPriorityOrder= propogator.getAllVariablesInPriorityOrder();
        }
        
        return result;
    }
}
