/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.cplex.staticPriority;

import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.ONE;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.SIXTY;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.ZERO;
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.CPLEX_RANDOM_SEED;
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.DISABLE_CUTS;
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.DISABLE_HEURISTICS;
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.DISABLE_PRESOLVE;
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.DISABLE_PRESOLVENODE;
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.DISABLE_PROBING;
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.FAST_CPLEX_BRANCH_STRATEGY;
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.FILE_STRATEGY;
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.MAX_THREADS; 
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.MIP_EMPHASIS;
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.NODE_LOG_INTERVAL;
import ca.mcmaster.hypercube_subtraction_generic_v3.Driver;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.MIP_FILENAME;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.USE_PURE_CPLEX;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.HyperCube;
import ca.mcmaster.hypercube_subtraction_generic_v3.cplex.BaseCplexSolver;
import static ca.mcmaster.hypercube_subtraction_generic_v3.cplex.BaseCplexSolver.isHaltFilePresent;
import ca.mcmaster.hypercube_subtraction_generic_v3.cplex.EmptyBranchHandler;
import ca.mcmaster.hypercube_subtraction_generic_v3.cplex.HypercubeBranchHandler;
import ca.mcmaster.hypercube_subtraction_generic_v3.cplex.StaticticsCallback;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static ilog.cplex.IloCplex.Status.Infeasible;
import static ilog.cplex.IloCplex.Status.Optimal;
import static java.lang.System.exit;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 * 
 * at the first node where there is no infeasible trigger, set a static priority list
 * At that stage, witch to pure CPLEX with priority lists
 * 
 * Only use with SAT problems, not implemented for set-partitioning
 * 
 * 
 */
public class StaticCplexSolver extends BaseCplexSolver{
    
    //should be moved into the base class
    private IloCplex.BranchCallback branchingCallback = null;
    
    //constructor should also be moved into the base class
    public StaticCplexSolver (       TreeMap<Integer, 
            List<HyperCube>>   infeasibleHypercubeMap ) throws IloException {
        
        cplex = new IloCplex() ;
        cplex.importModel(MIP_FILENAME);
        
        if (CPLEX_RANDOM_SEED>=ZERO) cplex.setParam(IloCplex.Param.RandomSeed,  CPLEX_RANDOM_SEED);
        
        cplex.setParam(IloCplex.Param.Emphasis.MIP,   MIP_EMPHASIS );
        cplex.setParam( IloCplex.Param.Threads, MAX_THREADS);
        cplex.setParam(IloCplex.Param.MIP.Strategy.File,  FILE_STRATEGY);   
        cplex.setParam(IloCplex.Param.MIP.Interval, NODE_LOG_INTERVAL);
         
        if (DISABLE_HEURISTICS) cplex.setParam( IloCplex.Param.MIP.Strategy.HeuristicFreq , -ONE);
        if (DISABLE_PROBING) cplex.setParam(IloCplex.Param.MIP.Strategy.Probe, -ONE);
        
        if (DISABLE_PRESOLVENODE) cplex.setParam(IloCplex.Param.MIP.Strategy.PresolveNode, -ONE);
       
        if (DISABLE_PRESOLVE) cplex.setParam(IloCplex.Param.Preprocessing.Presolve, false);
        
        if (DISABLE_CUTS) cplex.setParam(IloCplex.Param.MIP.Limits.CutPasses, -ONE);
        
        if (null!=infeasibleHypercubeMap){
            //we will run BCP t figure out priority list
            branchingCallback=new StaticBranchHandler( infeasibleHypercubeMap );
            cplex.setParam(IloCplex.IntParam.VarSel,  FAST_CPLEX_BRANCH_STRATEGY);
        }    
            
        
        
    }
    
    //use this solve method with  frequency based DLIS
    public void solve (int rampUpDurationHours, int solveDurationHours, Map<IloNumVar, Integer> freqMap ) throws IloException {
     
        for (Entry<IloNumVar, Integer> priorityEntry   :freqMap.entrySet()) {
            cplex.setPriority(  priorityEntry.getKey() , priorityEntry.getValue());
        }
        
        rampUpAndSolve (  rampUpDurationHours,   solveDurationHours) ;
        
    }
 
    //use this solve to   do BCP, then set priority list
    public void solve(int rampUpDurationHours, int solveDurationHours) throws IloException {
        
        logger.info (  "  Static callback started  ");
        System.out.println (  "  Static callback started  ");
        //first solve using StaticBranchHandler
        cplex.clearCallbacks();
        cplex.use( this.branchingCallback);         
        cplex.setParam( IloCplex.Param.Threads, ONE);
        cplex.solve();
        logger.info (  "  Static callback completed  ");
        System.out.println (  "  Static callback completed  ");
        
        //set priority order
        StaticBranchHandler staticHandler = (StaticBranchHandler)this.branchingCallback;
        for (Entry<String, Integer> priorityEntry   :staticHandler.variablesInPriorityOrder.entrySet()) {
            cplex.setPriority( Driver.mapOfAllVariablesInTheModel.get(priorityEntry.getKey()), priorityEntry.getValue());
        }
        
        rampUpAndSolve (  rampUpDurationHours,   solveDurationHours) ;
        
    }
    
    private void rampUpAndSolve (int rampUpDurationHours, int solveDurationHours) throws IloException {
        
        //now solve using empty branch callback and pure cplex
        //assign variable priority order for the first "ramp up" hours, then clear it
        //this is the same as the traditional solver, except that the hypercube handler is replaced by a static variable priority list
        for (int hours=ZERO; hours < rampUpDurationHours; hours ++){
            
            if (isHaltFilePresent()) break;
            
            cplex.clearCallbacks();
            cplex.use(  new EmptyBranchHandler());
            cplex.setParam( IloCplex.Param.TimeLimit,  SIXTY*SIXTY);
            //switch to the correct number of threads
            cplex.setParam( IloCplex.Param.Threads, MAX_THREADS);
            cplex.solve();

            //print stats
            StaticticsCallback stats = new StaticticsCallback();
            cplex.use (stats) ; 
            //switch to 1 thread and solve
            cplex.setParam( IloCplex.Param.Threads, ONE);
            cplex.solve();
            logger.info (  " , " + hours + " , " + stats.bestKnownBound + " , "+ stats.bestKnownSOlution + " , "+
                            stats.numberOFLeafs +  " , "+ stats.numberOFNodesProcessed );
            
            //stop iterations if completely solved
            if (cplex.getStatus().equals(Optimal)||cplex.getStatus().equals(Infeasible)) break;
        }
                
        for (IloNumVar priorityEntry   :  Driver.mapOfAllVariablesInTheModel.values() ) {
            cplex.delPriority( priorityEntry );
        }
        
        //now do the vanilla solve
        for (int hours=ZERO; hours < solveDurationHours; hours ++){
            
            if (isHaltFilePresent()) break;
             
            cplex.clearCallbacks();
            cplex.setParam(IloCplex.IntParam.VarSel,   IloCplex.VariableSelect.DefaultVarSel);
             
            cplex.use(new EmptyBranchHandler());
            cplex.setParam( IloCplex.Param.TimeLimit,   SIXTY*SIXTY);
            //switch to the correct number of threads
            cplex.setParam( IloCplex.Param.Threads, MAX_THREADS);
            cplex.solve();

            //print stats
            StaticticsCallback stats = new StaticticsCallback();
            cplex.use (stats) ;
            //switch to 1 thread and solve
            cplex.setParam( IloCplex.Param.Threads, ONE);
            cplex.solve();
            logger.info (   " , " + (rampUpDurationHours+hours )+ " , " + stats.bestKnownBound + " , "+ stats.bestKnownSOlution + " , "+
                            stats.numberOFLeafs +  " , "+ stats.numberOFNodesProcessed );
            
            //stop iterations if completely solved
            if (cplex.getStatus().equals(Optimal)||cplex.getStatus().equals(Infeasible)) break;
            
        }
        
         
    }
    
}
