/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.collection;
    
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.HyperCube;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class CollectedInfeasibleHypercubeMap {
    
    //key is # of var fixings
    public   TreeMap<Integer, List<HyperCube>>  collectedHypercubes  = new TreeMap<Integer, List<HyperCube>>();
    
    
    
    public CollectedInfeasibleHypercubeMap (){
        
    }
    
    //add cubes of given size, after deleting duplicates
    //if merge is ON, merge will also be done
    public boolean addCubesAndCheckInfeasibility  (TreeMap<Integer, List<HyperCube>>  collectedHypercubesForAConstraint ){
        boolean isMIPInfeasible = false;
        for ( int size =  collectedHypercubesForAConstraint.lastKey(); size >=ZERO; size --){
            if (!collectedHypercubesForAConstraint.containsKey(size)) continue;
            List<HyperCube> newCubes =  collectedHypercubesForAConstraint.get(size);
            if (CHECK_FOR_DUPLICATES) newCubes=removeDuplicates(newCubes, size);
            isMIPInfeasible=addCubesAndCheckInfeasibility(newCubes,size) ;
        }
        return isMIPInfeasible;
    }
    
    public void absorb( ){
        for ( int size =  this.collectedHypercubes.lastKey(); size >=ZERO; size --){
            if (!collectedHypercubes.containsKey(size)) continue;
            List<HyperCube> cubes =  collectedHypercubes.get(size);
            //check if absorbed into any cube of a smaller size
            for (HyperCube cube: cubes ){
                if (absorb(cube)){
                    cube.isMarkedAsMerged=true;
                    
                    
                }
            }
        }
    }
    
    //return cubes not marked merged or absorbed
    public TreeMap<Integer, List<HyperCube>> getCollectedCubes (){
        for (int key = ZERO; key <=collectedHypercubes .lastKey(); key++){
            if (collectedHypercubes.containsKey(key)){
                
                List<HyperCube> cubes = collectedHypercubes.get(key);
                List<HyperCube> cleanedCubes = new ArrayList<HyperCube> ();
                for (HyperCube cube: cubes){
                    if(!cube.isMarkedAsMerged)                    {
                        cleanedCubes.add(cube);
                    }
                }
                if (cleanedCubes.size()>ZERO){
                    collectedHypercubes.put(key,cleanedCubes);
                } else collectedHypercubes.remove(key);
            }
        }
        return collectedHypercubes;
    }
    
       
    public void printCollectedHypercubes (boolean cleanFirst){
        if (cleanFirst)getCollectedCubes();
        
        //TreeMap<Integer, List<HyperCube>>  collectedHypercubeMap
        for (Map.Entry <Integer, List<HyperCube>> entry :collectedHypercubes.entrySet() ){
            System.out.println("Size "+ entry.getKey() + " : ");
            for ( HyperCube cube: entry.getValue() ){
                System.out.println(cube.printMe());
            }
        }
    }
    
    //is this cube absorbed into any higher level cube ?
    private boolean absorb( HyperCube candidateCube ) {
        boolean isAbsorbed= false;
        for (Entry <Integer, List<HyperCube>> entry : this.collectedHypercubes.entrySet()){
            if (candidateCube.getSize() <=entry.getKey()) continue;
            List<HyperCube> cubesAtThisDepth = entry.getValue();
            for (HyperCube cubeAtThisDepth : cubesAtThisDepth ){
                if (cubeAtThisDepth.isAncestorOf (candidateCube)){
                    candidateCube.isMarkedAsMerged=true;
                    isAbsorbed = true;
                    //System.out.println("print absorbed cubes") ;
                    //candidateCube.printMe();
                    //cubeAtThisDepth.printMe();
                    break;
                }
            }
            if (isAbsorbed) break;
        }
        return isAbsorbed;
    }
    
    private boolean addCubesAndCheckInfeasibility (List<HyperCube> newCubesWithoutDuplicates , int size){
        // if size 0 is added, mip is infeasible
        boolean isMIPInfeasible = (size==ZERO);
        
        List<HyperCube> existingCubes = this.collectedHypercubes.get(size);
        if (null==existingCubes) existingCubes=new ArrayList<HyperCube> ();
        
        if (MERGE_COLLECTED_HYPERCUBES && size > ZERO){
            //new cube merges with any existing cube?
            List<HyperCube> mergedCubes = merge (existingCubes, newCubesWithoutDuplicates)  ;
            if (mergedCubes.size()>ZERO) {
                isMIPInfeasible= addCubesAndCheckInfeasibility(mergedCubes, size-ONE);
            } 
        }
        
        if (!isMIPInfeasible){
            existingCubes.addAll(newCubesWithoutDuplicates );
            collectedHypercubes. put (size, existingCubes);
        }
        return isMIPInfeasible;
        
    }
    
    private  List<HyperCube>   merge (List<HyperCube>   existingCubes, List<HyperCube>   newCubesWithoutDuplicates){
        List<HyperCube>  mergedCubeList = new ArrayList<HyperCube>  ();
        
        for (HyperCube newCube: newCubesWithoutDuplicates){
            //check if can merge with any existing cube, can possibly merge with many     
            for(HyperCube existingCube : existingCubes){
                HyperCube mergedCube= existingCube.merge(newCube);
                if (null!=mergedCube) {
                    newCube.isMarkedAsMerged=true;
                    existingCube.isMarkedAsMerged=true;
                    mergedCubeList.add(mergedCube );
                    
                    //System.out.println("merge succesful" );
                    // newCube.printMe();
                    //  existingCube.printMe();
                    //mergedCube.printMe();
                    // System.out.println();
                    
                }
            }
        }
        
        return mergedCubeList;
    }
    
    private List<HyperCube> removeDuplicates (List<HyperCube> newCubes, int size) {
        
        
        
        List<HyperCube> result = new ArrayList<HyperCube>();
        List<HyperCube> existingCubes = this.collectedHypercubes.get(size);
        if (existingCubes==null){
            result=newCubes;
        }else {
            for (HyperCube newCube: newCubes){
                boolean isDuplicate = false;
                for(HyperCube existingCube : existingCubes){
                    if (existingCube.isDuplicate(newCube )){
                        isDuplicate=true;
                        //System.out.println("duplicate cube found! " +duplicateCount++);
                        break;
                    }
                }
                if (!isDuplicate) result.add(newCube);
            }            
        }
        return result;
    }
    
   
}
