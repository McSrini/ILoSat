/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.collection; 
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class ConstraintAnchoredCollector {
    
    //key is how many vars are differnt from the most infeasible vertex
    private TreeMap<Integer, List<HyperCube>> pendingJobMap = new TreeMap<Integer, List<HyperCube>>();
    
    //this is the constraint for which we will collect infeasible hypercubes
    private LowerBoundConstraint targetConstraint;
    
    //key is # of var fixings
    public  TreeMap<Integer, List<HyperCube>>  collectedHypercubeMap  = new TreeMap<Integer, List<HyperCube>>();
     
    public   ConstraintAnchoredCollector (LowerBoundConstraint targetConstraint) {
        this. targetConstraint=  targetConstraint;
        
        //in this project, we only collect infeasible hypercubes at the MIP root
        HyperCube mipRoot = new  HyperCube(new ArrayList <String>(), new ArrayList <String>()) ; 
        List<HyperCube>         startingJobs = new ArrayList<HyperCube>  ();
        startingJobs.add(mipRoot);
        pendingJobMap.put( ZERO,startingJobs );
    }
     
    /*
    
    We can do more rounds of collection after we aggregate infeasible hypercubes from every constraint.
    For example we can look at the largest collected infeasible hypercubes, and start collecting infeasible hypercubes for their sibling nodes.
    This is a todo for later.
    
    */
    
    public void collectInfeasibleHypercubes(){
        int  numCollected = ZERO;
        while (   true){
            int deviation = this.getPendingJobsLowestKey();
            if (deviation < ZERO){
                //no jobs left
                break;
            }
            if (NUM_ADJACENT_VERTICES_TO_COLLECT < deviation){
                break;
            }
            HyperCube job= getNextJob ();
            HyperCube collectedCube = collectOneInfeasibleHypercube (job, deviation);
            if (null!=collectedCube) numCollected++;
            
            if (MAX_HYPERCUBES_PER_CONSTRAINT < numCollected) break;
        }  
        //System.out.println(this.targetConstraint.name +" number hypercubes collected "+ numCollected);
    }
    
    public void printCollectedHypercubes (){
        //TreeMap<Integer, List<HyperCube>>  collectedHypercubeMap
        for (Entry <Integer, List<HyperCube>> entry :collectedHypercubeMap.entrySet() ){
            System.out.println("Size "+ entry.getKey() + " : ");
            for ( HyperCube cube: entry.getValue() ){
                cube.printMe();
            }
        }
    }
    
    //collect 1 infeasible hypercube, and insert any new jobs back into the pending map
    //
    //while generating new jobs, try   to fix the highest frequency variables
    //
    private HyperCube collectOneInfeasibleHypercube (HyperCube job , int deviation) {
        
        HyperCube infeasibleCube  = null;
        
        LowerBoundConstraint reducedConstraint = this.targetConstraint.getReducedConstraint(
                job.zeroFixingsMap.keySet(), job.oneFixingsMap.keySet() ) ; 
        
        double infeasibility = reducedConstraint.getLargestPossibleInfeasibility();
        
        //if infeasibility>=0 , not possible to collect any infeasible hypercubes
        if (infeasibility<ZERO){
            
            double reaminingInfeasibility= infeasibility;
            int index = -ONE;
            
            for (VariableCoefficientTuple tuple : reducedConstraint.constraintExpression){
                
                index++;
                
                //can we flip this var without making constraint feasible?
                if (Math.abs(tuple.coeff) < Math.abs(reaminingInfeasibility)) { 
                    reaminingInfeasibility+=Math.abs(tuple.coeff);
                }else {
                    //from here on , all vars will be fixed , and this will give us our infeasible hypercube
                    infeasibleCube= constructInfeasibleHyperCube (index, reducedConstraint, job) ;
                    this.addCollectedinfeasibleHypercube(infeasibleCube);
                    addNewPendingJobs(index,reducedConstraint, deviation, job);
                    break;
                }
            }
          
        }else {
            //System.out.println("Job is feasible "+job);
        }
        
        return infeasibleCube;
    }
    
    //add the pending jobs that result when this infeasible hypercube is extracted out
    //
    //try to keep the high frequency variables fixed as far as possible
    private void addNewPendingJobs(int index, LowerBoundConstraint reducedConstraint, 
                                   int parentsDeviation, HyperCube parentJob){
        
        List<HyperCube> newPendingJobs = new ArrayList<HyperCube>();
        
        //fix the variable at cursor towards feasibilty, and all others towards INfeasibility
        //then increment cursor by 1 and repeat
        for ( int cursor = index;cursor< reducedConstraint.constraintExpression.size(); cursor++){
            
            List<String> zeroFixed = new ArrayList<String>();
            List<String> oneFixed = new ArrayList<String>();
            
            VariableCoefficientTuple tuple = reducedConstraint.constraintExpression.get(cursor);
            if (tuple.coeff > ZERO){
                oneFixed.add(tuple.varName );
            }else {
                zeroFixed.add(tuple.varName);
            }
        
            for (int  position = cursor+ONE;  
                 position < reducedConstraint.constraintExpression.size();  
                 position++){
                tuple = reducedConstraint.constraintExpression.get(position);
                if (tuple.coeff < ZERO){
                    oneFixed.add(tuple.varName );
                }else {
                    zeroFixed.add(tuple.varName);
                }
            }
            
            //create the job
            zeroFixed.addAll( parentJob.zeroFixingsMap.keySet());
            oneFixed.addAll(parentJob.oneFixingsMap.keySet());
            newPendingJobs.add (new HyperCube (zeroFixed,oneFixed));
        }
        
        //compared to parent job , all these new jobs have 1 more variable that deviates from 
        //its value at the most infeasible vertex
        List<HyperCube> buffer = pendingJobMap.containsKey(parentsDeviation+ONE)?
                                  pendingJobMap.get( parentsDeviation+ONE): new ArrayList<HyperCube>();
        buffer.addAll(newPendingJobs );
        this.pendingJobMap.put( parentsDeviation+ONE , buffer);        
        
    }
    
    private HyperCube constructInfeasibleHyperCube (int index, LowerBoundConstraint reducedConstraint, 
            HyperCube node) {
        List<String> zeroFixed = new ArrayList<String>();
        List<String> oneFixed = new ArrayList<String>();
        
        zeroFixed.addAll( node.zeroFixingsMap.keySet());
        oneFixed.addAll(node.oneFixingsMap.keySet() );
        
        for (int position= index ; position <reducedConstraint.constraintExpression.size(); position++ ){
            VariableCoefficientTuple tuple = reducedConstraint.constraintExpression.get(position);
            if (tuple.coeff < ZERO){
                oneFixed.add(tuple.varName );
            }else {
                zeroFixed.add(tuple.varName);
            }
        }
        
        //always sort the variables for convenience during absorb
         
        Collections.sort(oneFixed);
        Collections.sort(zeroFixed);
        
        return new HyperCube (zeroFixed,oneFixed);
    }
    
    private void addCollectedinfeasibleHypercube (HyperCube infeasibleHypercube){
        int size = infeasibleHypercube.getSize();
        List<HyperCube> current;
        if (this.collectedHypercubeMap.containsKey(size))      {
            current = this.collectedHypercubeMap.get(size);           
        } else {
            current = new ArrayList<HyperCube> ();
        }
        current.add(infeasibleHypercube);
        this.collectedHypercubeMap.put(size,current );
    }
    
    //return lowest key or -1
    private int getPendingJobsLowestKey (){
        return   pendingJobMap.isEmpty() ? -ONE : pendingJobMap.firstKey();
    }
    
    private HyperCube getNextJob (){
        Entry<Integer, List<HyperCube>> entry = pendingJobMap.firstEntry();
        List<HyperCube> cubes = entry.getValue() ;
        HyperCube result = cubes.remove(ZERO);
        if (cubes.size()>ZERO) {
            pendingJobMap.put (entry.getKey(),cubes );
        } else {
            pendingJobMap.remove (entry.getKey());
        }
        return result;
    }
     
    
}
