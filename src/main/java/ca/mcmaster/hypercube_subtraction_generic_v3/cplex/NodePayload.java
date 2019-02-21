/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.cplex;
  
import ca.mcmaster.hypercube_subtraction_generic_v3.common.HyperCube;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class NodePayload implements IloCplex.MIPCallback.NodeData  {
    
    //infeasible hypercubes inherited from parent
    //key is # of variables in the hypercubes
    public TreeMap<Integer, List<HyperCube>>   infeasibleHypercubesMap =null;
    
    //var fixings inherited from parent, false indicates 0 fixing
    public TreeMap<String, Boolean> parentVarFixings =new TreeMap<String, Boolean> ();
     
 
    public void delete() {
          
        //we expect java to garbage collect, we do not explicitly free anything
        
        //also note, both kids have the same reference to the parent's hypercubes, so 
        //cant delete the reference anyway until both kids are inactive
    }
    
}
