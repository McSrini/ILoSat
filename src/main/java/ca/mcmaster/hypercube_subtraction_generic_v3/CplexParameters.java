/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3;

import ilog.cplex.IloCplex;

/**
 *
 * @author tamvadss
 */
public class CplexParameters {
    
        
    public static final int MIP_EMPHASIS=   0; 
    public static final int MAX_THREADS= 32;  
    public static final int FILE_STRATEGY= 3;  
    
    //leave at -1 to take default
    public static final int CPLEX_RANDOM_SEED=  -1 ;
    
    //leave at 0 unless debugging
    public static final int NODE_LOG_INTERVAL = 0; 
    
    
    
    public static final boolean DISABLE_HEURISTICS= true; 
    public static final boolean DISABLE_PROBING= false; 
    public static final boolean DISABLE_PRESOLVENODE = false ;
    public static final boolean DISABLE_PRESOLVE = false;
    public static final boolean DISABLE_CUTS = false;
    
    //since we overrule cplex anyway during ramp up
    public static final int  FAST_CPLEX_BRANCH_STRATEGY = IloCplex.VariableSelect.MinInfeas;
    
    
    
    //use if default cplex is very slow to branch some MIPs
    //public static final boolean USE_BARRIER_AND_REDUCED_COSTS = false;
    
    public static final int RAMP_UP_DURATION_HOURS= 1;  
    public static final int SOLUTION_DURATION_HOURS= 20*24 ;  //20 DAYS MAXIMUM
    
}
