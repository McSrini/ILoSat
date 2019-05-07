/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.utils;
  
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.Driver;
import ca.mcmaster.hypercube_subtraction_generic_v3.Parameters;  
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.HEURISTIC_TO_USE;
import static ca.mcmaster.hypercube_subtraction_generic_v3.bcp.BCP_LEVEL_ENUM.NO_BCP;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.LowerBoundConstraint;
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
import ca.mcmaster.hypercube_subtraction_generic_v3.common.VariableCoefficientTuple;
import ca.mcmaster.hypercube_subtraction_generic_v3.heuristics.BRANCHING_HEURISTIC_ENUM;

/**
 *
 * @author tamvadss
 */
public class MIPReader {
    
    public static boolean isThisSetpartitioning (IloCplex cplex) throws IloException {
        boolean isThisSetpartitioning = true ;
        
        //return false if even a single constraint is not set partitioning
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();        
        final int numConstraints = lpMatrix.getNrows();
        int[][] ind = new int[ numConstraints][];
        double[][] val = new double[ numConstraints][];
        
        double[] lb = new double[numConstraints] ;
        double[] ub = new double[numConstraints] ;
        
        lpMatrix.getRows(ZERO,   numConstraints, lb, ub, ind, val);
                
        for (int index=ZERO; index < numConstraints ; index ++ ){
            
            //System.out.println(index);//k
                        
            boolean isEquality = ub[index]==lb[index];
            
            if (isEquality)  {
                //bound must be 1, and all coeffs must be 1 too
                
                if (ZERO!=Double.compare(ONE, lb[index]) ) {
                    isThisSetpartitioning= false;
                    break;
                }
                if (ZERO!=Double.compare(ONE, ub[index]) ) {
                    isThisSetpartitioning= false;
                    break;
                }
                
                for (  int varIndex = ZERO;varIndex< ind[index].length;   varIndex ++ ){
                    Double coeff = val[index][varIndex];
                    if (ZERO!=Double.compare(ONE, coeff) ) {
                        isThisSetpartitioning= false;
                        break;
                    }
                }
                
            }else {
                isThisSetpartitioning= false;
                break;
            }
        }
        
        return isThisSetpartitioning;
    }
    
    
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
            
            String thisConstraintname = ranges[index].getName();
            //System.out.println("Constarint is : " + thisConstraintname + " lenght is " +ind[index].length);//k
            
            
                       
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
                
                 
                
                if (Driver.IS_THIS_SET_PARTITIONING   && ind[index].length>ONE  
                        && !Parameters.USE_BCP_LEVEL.equals( NO_BCP) && 
                        HEURISTIC_TO_USE.equals( BRANCHING_HEURISTIC_ENUM.STEPPED_WEIGHT)){
                    //add all 2 size constraints, with all pairs in this constraint
                    for (LowerBoundConstraint pairConstraint : getPairConstraints ( lbcUP)){
                        result.add(pairConstraint);
                    }
                }
                
                //System.out.println(lbcUP.printMe());//k
                if (  Driver.IS_THIS_SET_PARTITIONING  && ind[index].length>ONE) {
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
    
    //convert x1=x2+...+xn<=1 into pairs like xi+xj<=1
    public static List <LowerBoundConstraint > getPairConstraints ( LowerBoundConstraint lbc){
        List <LowerBoundConstraint > results= new ArrayList <LowerBoundConstraint >();
        
        //System.out.println("constraint is "  + lbc.printMe());
        
        final int NUMBER_OF_VARS = lbc.constraintExpression.size();
        for (int ii = ZERO; ii< NUMBER_OF_VARS; ii++){
            for (int jj = ZERO; jj< NUMBER_OF_VARS; jj++){
                if (ii>=jj)  continue;
                String thisConstraintName = lbc.name+"_"+ii+"_"+jj;
                List<VariableCoefficientTuple>   this_constraint_Expr = new ArrayList<VariableCoefficientTuple>();
                VariableCoefficientTuple v1= new VariableCoefficientTuple ( lbc.constraintExpression.get(ii).varName, -ONE);
                VariableCoefficientTuple v2= new VariableCoefficientTuple ( lbc.constraintExpression.get(jj).varName, -ONE);
                this_constraint_Expr.add(v1);
                this_constraint_Expr.add(v2);
                
                LowerBoundConstraint pairConstraint = new LowerBoundConstraint( thisConstraintName,this_constraint_Expr, -ONE);
                results.add(pairConstraint );    
                //System.out.println(pairConstraint.printMe());
            }
        }
        
        return results;
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
