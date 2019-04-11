/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.cplex;
 
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.Driver;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.bcp.BCP_Propogator; 
import ca.mcmaster.hypercube_subtraction_generic_v3.common.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.heuristics.BRANCHING_HEURISTIC_ENUM;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static ilog.cplex.IloCplex.Algorithm.None;
import static ilog.cplex.IloCplex.IncumbentId;
import static ilog.cplex.IloCplex.Status.Infeasible;
import static ilog.cplex.IloCplex.Status.Optimal;
import static ilog.cplex.IloCplex.VariableSelect.PseudoReduced;
import java.io.File;
import static java.lang.System.exit;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class TraditionalCplexSolver extends BaseCplexSolver{
            
    private IloCplex.BranchCallback branchingCallback = null;
     

     
    public TraditionalCplexSolver (       TreeMap<Integer, 
            List<HyperCube>>   infeasibleHypercubeMap ) throws IloException {
        cplex = new IloCplex() ;
        cplex.importModel(MIP_FILENAME);
        
        if (CPLEX_RANDOM_SEED>=ZERO) cplex.setParam(IloCplex.Param.RandomSeed,  CPLEX_RANDOM_SEED);
        
        cplex.setParam(IloCplex.Param.Emphasis.MIP,  MIP_EMPHASIS);
        cplex.setParam( IloCplex.Param.Threads, MAX_THREADS);
        cplex.setParam(IloCplex.Param.MIP.Strategy.File,  FILE_STRATEGY);   
        cplex.setParam(IloCplex.Param.MIP.Interval, NODE_LOG_INTERVAL);
         
        if (DISABLE_HEURISTICS) cplex.setParam( IloCplex.Param.MIP.Strategy.HeuristicFreq , -ONE);
        if (DISABLE_PROBING) cplex.setParam(IloCplex.Param.MIP.Strategy.Probe, -ONE);
        
        if (DISABLE_PRESOLVENODE) cplex.setParam(IloCplex.Param.MIP.Strategy.PresolveNode, -ONE);
       
        if (DISABLE_PRESOLVE) cplex.setParam(IloCplex.Param.Preprocessing.Presolve, false);
        
        if (DISABLE_CUTS) cplex.setParam(IloCplex.Param.MIP.Limits.CutPasses, -ONE);
        
        if (USE_PURE_CPLEX){
            branchingCallback=new EmptyBranchHandler();
             
        }else   {
            branchingCallback=new HypercubeBranchHandler( infeasibleHypercubeMap );
            cplex.setParam(IloCplex.IntParam.VarSel,  FAST_CPLEX_BRANCH_STRATEGY);
            
        }     
        
    }
    
    
    public void solve (int rampUpDurationHours, int solveDurationHours  ) throws IloException{
        for (int hours=ZERO; hours < rampUpDurationHours; hours ++){
            
            if (isHaltFilePresent()) break;
            
            cplex.clearCallbacks();
            cplex.use( this.branchingCallback);
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
