/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.cplex;
  
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.ZERO;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchCallback;

/**
 *
 * @author tamvadss
 */
public class EmptyBranchHandler extends BranchCallback{
 
    protected void main() throws IloException {
        if ( getNbranches()> ZERO ){  
             
        }
    }
    
}
