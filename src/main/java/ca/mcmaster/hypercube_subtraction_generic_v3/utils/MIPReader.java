/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.utils;
  
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.*;  
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.HEURISTIC_TO_USE;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.LowerBoundConstraint;
import static ca.mcmaster.hypercube_subtraction_generic_v3.heuristics.BRANCHING_HEURISTIC_ENUM.SET_PARTITIONING;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloLinearNumExprIterator;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloObjectiveSense;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class MIPReader {
    
    //get all constraints as lower bounds
    //Improved method that does not use iterators
    public static List<LowerBoundConstraint> getConstraintsFast(IloCplex cplex) throws IloException{
        
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        
        final int numConstraints = lpMatrix.getNrows();
        final int numVariables = lpMatrix.getNcols();
        
        List<LowerBoundConstraint> result = new ArrayList<LowerBoundConstraint>( );
        
        
        int[][] ind = new int[ numConstraints][];
        double[][] val = new double[ numConstraints][];
        
        double[] lb = new double[numConstraints] ;
        double[] ub = new double[numConstraints] ;
        
        lpMatrix.getRows(ZERO,   numConstraints, lb, ub, ind, val);
        
        IloRange[] ranges = lpMatrix.getRanges() ;
        
        //build up each constraint 
        for (int index=ZERO; index < numConstraints ; index ++ ){
            
            //System.out.println(index);//k
            String thisConstraintname = ranges[index].getName();
                       
            boolean isUpperBound = Math.abs(ub[index])< BILLION ;
            boolean isLowerBound = Math.abs(lb[index])<BILLION ;
            boolean isEquality = ub[index]==lb[index];
            
            if (isEquality)  {
                LowerBoundConstraint lbcUP =new LowerBoundConstraint(thisConstraintname+NAME_FOR_EQUALITY_CONSTRAINT_LOWER_BOUND_PORTION);
                LowerBoundConstraint lbcDOWN =new LowerBoundConstraint(thisConstraintname+NAME_FOR_EQUALITY_CONSTRAINT_UPPER_BOUND_PORTION);
                 
                lbcUP .lowerBound= lb[index];
                lbcDOWN.lowerBound=-ub[index]; //ub portion
                
                for (  int varIndex = ZERO;varIndex< ind[index].length;   varIndex ++ ){
                    String var = lpMatrix.getNumVar(ind[index][varIndex]).getName() ;
                    Double coeff = val[index][varIndex];
                    lbcUP.add(var,  coeff) ;
                    lbcDOWN.add(var, -coeff);
                }
                
                
                result.add(lbcUP) ;
                //System.out.println(lbcUP.printMe());//k
                if (  SET_PARTITIONING.equals( HEURISTIC_TO_USE) && ind[index].length>ONE) {
                    System.out.println("Skipping UB portion equality constraint for SET_PARTITIONING_PROBLEM ");                    
                } else {
                    result.add(lbcDOWN) ;
                }
                //System.out.println(lbcDOWN.printMe());//k
                 
                
            }else {
                LowerBoundConstraint lbc =new LowerBoundConstraint(thisConstraintname);
                lbc.lowerBound=  (isUpperBound && ! isLowerBound )? -ub[index] : lb[index];
                for (  int varIndex = ZERO;varIndex< ind[index].length;   varIndex ++ ){
                    String var = lpMatrix.getNumVar(ind[index][varIndex]).getName() ;
                    Double coeff = val[index][varIndex];
                    lbc.add(var, (isUpperBound && ! isLowerBound )? -coeff: coeff) ;
                }
                result.add(lbc) ;
                //System.out.println(lbc.printMe());//k
            }
            
        }
 
        return result;
        
    }
    
   
    public static List<IloNumVar> getVariables (IloCplex cplex) throws IloException{
        List<IloNumVar> result = new ArrayList<IloNumVar>();
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        IloNumVar[] variables  =lpMatrix.getNumVars();
        for (IloNumVar var :variables){
            result.add(var ) ;
        }
        return result;
    }
    
    //minimization objective
    public static Map<String, Double> getObjective (IloCplex cplex) throws IloException {
        
        Map<String, Double>  objectiveMap = new HashMap<String, Double>();
        
        IloObjective  obj = cplex.getObjective();
        boolean isMaximization = obj.getSense().equals(IloObjectiveSense.Maximize);
        
        IloLinearNumExpr expr = (IloLinearNumExpr) obj.getExpr();
                 
        IloLinearNumExprIterator iter = expr.linearIterator();
        while (iter.hasNext()) {
           IloNumVar var = iter.nextNumVar();
           double val = iter.getValue();
           
           //convert  maximization to minimization 
            
           objectiveMap.put(var.getName(), !isMaximization ? val : -val );
        }
        
        return  objectiveMap ;
        
         
    }
}
