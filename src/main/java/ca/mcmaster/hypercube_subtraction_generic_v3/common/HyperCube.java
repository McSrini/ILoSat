/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.common;
  
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.Driver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class HyperCube {
     
    //we use treemaps to get sorted order of keys
    //sorted order is useful for merge and absorb
    public TreeMap<String, Boolean> zeroFixingsMap = new  TreeMap<String, Boolean>();
    public TreeMap<String, Boolean> oneFixingsMap  = new  TreeMap<String, Boolean>();
        
    //flag used to discard merged and absorbed hypercubes
    public boolean isMarkedAsMerged = false;
    
    //set this flag to true if cube-fliter detected that var is not included in this hyper cube
    public boolean isFilterResult_simplePassThrough = false;
    
    public HyperCube (Collection<String> zeroFixedvars ,Collection<String> oneFixedvars){
        for (String var: zeroFixedvars){
            zeroFixingsMap.put(var, false);  //value is ignored
        } 
        for (String var: oneFixedvars){
            oneFixingsMap.put(var, true); 
        }
        
         
    }
    
      
    
    //if failing filter, return null
    //if matching filter, return new hypercube with filter fixing removed
    //if no match, simply return self
    public HyperCube filter (String var, boolean isOneFixed) {
        HyperCube result = null;
        if (isOneFixed){
            //check if 1 fixing exists
            if (oneFixingsMap.containsKey(var)){
                //return new hypercube
                List<String> newOneFixedVars = new ArrayList<String>();
                newOneFixedVars.addAll(this.oneFixingsMap.keySet());
                newOneFixedVars.remove( var);
                
                
                
                result  = new HyperCube ( this.zeroFixingsMap.keySet(), newOneFixedVars) ;                      
                result.isFilterResult_simplePassThrough = false;
                
               
                
            } else if (zeroFixingsMap.containsKey(var)){
                //mismatch
                //return null
            } else {
                //no fixing of this var in this cube, pass thru
                result = this;
                result.isFilterResult_simplePassThrough = true;
            }
        }else {
            //check if 0 fixing exists
            if (zeroFixingsMap.containsKey(var)){
                //new hypercube
                List<String> newZeroFixedvars = new ArrayList<String>();
                newZeroFixedvars.addAll(this.zeroFixingsMap.keySet());
                newZeroFixedvars.remove(var );
                
                result  = new HyperCube (newZeroFixedvars,this.oneFixingsMap.keySet()) ;
                result.isFilterResult_simplePassThrough = false;                
                
                
                 
            } else if (oneFixingsMap.containsKey(var)){
                //null
            } else {
                //no fixing of this var, pass thru
                result = this;
                result.isFilterResult_simplePassThrough = true;
            }
        }
        
        return result;
    }    
    
    public int getSize(){
        return  this.zeroFixingsMap.size() + this.oneFixingsMap.size();
    }
    public int getZeroFixingsSize(){
        return  this.zeroFixingsMap.size()  ;
    }
    public int getOneFixingsSize(){
        return   this.oneFixingsMap.size();
    }
    

    
    public boolean isDuplicate ( HyperCube other){
        boolean result = ( this.getZeroFixingsSize() ==other.getZeroFixingsSize())&&
                        ( this.getOneFixingsSize()==other.getOneFixingsSize()) ;
        return result && isAncestorOf (   other);
    }
     
    public boolean isAncestorOf ( HyperCube other){
       boolean result = ( this.getZeroFixingsSize() <=other.getZeroFixingsSize())&&
                        ( this.getOneFixingsSize()<=other.getOneFixingsSize()) ;
        
       
       if (result){
           int zeroSize = this.getZeroFixingsSize();
           String[] thisZeroFixings  = this.zeroFixingsMap .keySet().toArray(new String[ZERO]);
           String[] otherZeroFixings = other.zeroFixingsMap .keySet().toArray(new String[ZERO]);
           for (int index=ZERO; index < zeroSize; index++){
               if (! thisZeroFixings[index].equals(otherZeroFixings[index] )){
                   result = false;
                   break;
               }
           }
       }
       
       if (result){
           int oneSize = this.oneFixingsMap.size();
           String[] thisOneFixings = this.oneFixingsMap.keySet().toArray(new String[ZERO]);
           String[] otherOneFixings = other.oneFixingsMap.keySet().toArray(new String[ZERO]);
           for(int index=ZERO; index < oneSize; index++){
               if (! thisOneFixings[index].equals( otherOneFixings[index])){
                   result = false;
                   break;
               }
           }
       }
       
       
       return result;
    }
    
        
    public double getBestPossibleObjectiveValue ()        {
        double objectiveValueAtBestUnconstrainedVertex = ZERO;
        
        for (Map.Entry <String, Double> entry :  Driver.objectiveFunctionMap.entrySet()){
            String thisVar = entry.getKey() ;
            double coeff =entry.getValue();
            
            boolean isIncludedZero = this.zeroFixingsMap .containsKey(thisVar);
            boolean isIncludedOne = this.oneFixingsMap  .containsKey(thisVar );
            
            if ( isIncludedZero || isIncludedOne                ) {
                //already fixed 
                if (isIncludedOne) objectiveValueAtBestUnconstrainedVertex+= coeff;
            }else {
                //choose fixing so that objective becomes lowest possible
                if ( coeff < ZERO){
                     
                    objectiveValueAtBestUnconstrainedVertex+= coeff;
                }  
            }
        }
        
        
        return objectiveValueAtBestUnconstrainedVertex;
    }
    
    
    //merge two siblings, if other is a sibling
    //if other is not a sibling, then return null
    public HyperCube merge (HyperCube other) {
        HyperCube result = null;
        
        //all vars same, except 1 var complimentary
        int myZeroSize = this.zeroFixingsMap .size();
        int myOneSize = this.oneFixingsMap .size();
        int otherZeroSize = other.zeroFixingsMap .size();
        int otherOneSize = other.oneFixingsMap .size();
        
        boolean isSizeMatchOne = ( (myZeroSize == otherZeroSize-ONE) &&(myOneSize==otherOneSize+ONE)) ;
        boolean isSizeMatchTwo =   ( (myZeroSize == otherZeroSize+ONE) &&(myOneSize==otherOneSize-ONE)) ;
        
        boolean isComplimentary = true;
        
        if ( isSizeMatchOne) {
            List < String> extraZeroVar = new ArrayList<String> ();
            for (String var : other.zeroFixingsMap.keySet()) {
                if ( null==this.zeroFixingsMap.get(var)) extraZeroVar.add(var);
                if (extraZeroVar.size()>ONE){
                    //not complimentary
                    isComplimentary= false;
                    break;
                }
            }
            
            List < String> extraOneVar = new ArrayList<String> ();
            if (isComplimentary) {
                for (String var : this.oneFixingsMap.keySet()  ) {
                    if ( null==other.oneFixingsMap.get(var)) extraOneVar.add(var);
                    if (extraOneVar.size()>ONE){
                        //not complimentary
                        isComplimentary= false;
                        break;
                    }
                }
            }
            
            if (isComplimentary && extraOneVar.get(ZERO).equals(extraZeroVar.get(ZERO))) {
                //well and truly complimentary
                result = new HyperCube (this.zeroFixingsMap.keySet(),other.oneFixingsMap.keySet()) ;
            }  
        }else if (isSizeMatchTwo){
            List < String> extraZeroVar = new ArrayList<String> ();
            for (String var : this.zeroFixingsMap.keySet()) {
                if ( null==other.zeroFixingsMap.get(var)) extraZeroVar.add(var);
                if (extraZeroVar.size()>ONE){
                    //not complimentary
                    isComplimentary= false;
                    break;
                }
            }
            
            List < String> extraOneVar = new ArrayList<String> ();
            if (isComplimentary) {
                for (String var : other.oneFixingsMap.keySet()  ) {
                    if ( null==this.oneFixingsMap.get(var)) extraOneVar.add(var);
                    if (extraOneVar.size()>ONE){
                        //not complimentary
                        isComplimentary= false;
                        break;
                    }
                }
            }
            
            if (isComplimentary && extraOneVar.get(ZERO).equals(extraZeroVar.get(ZERO))) {
                //well and truly complimentary
                result = new HyperCube (other.zeroFixingsMap.keySet(),this.oneFixingsMap.keySet()) ;
            }
        }
        
         
        return result;
    }
    
    //if exactly 1 var in cube fixed at 1   , then rest all var in cube to 0
    //if more than 1  var in cube fixed at 1 , infeasible
    //if all vars in cube fixed at 0 -> infeasible
    //if all vars in cube fixed at 0 except 1 -> leftover var fixed to 1     
    //all other cases  ->  no fixing
    //
    /*
    public  HyperCubeVariableFixingResult getResultOfVarFixings_SetPartition (List<VariableCoefficientTuple> inputVarFixings  ){
         
        HyperCubeVariableFixingResult result = new HyperCubeVariableFixingResult ();
        
        //recall that set partition hypercubes are all 0 klktu=p1l
         
        Set<String> cubeVarsFixedAtZero__ByTheseFixings = new HashSet<>();
        Set<String> cubeVarsFixedAtOne__ByTheseFixings = new HashSet<>();
                   
        for (VariableCoefficientTuple inputFixing: inputVarFixings ){
            if (Math.round(inputFixing.coeff)>ZERO){
                //tuple is 1 fixed
                if (this.zeroFixingsMap.containsKey( inputFixing.varName)) {
                    cubeVarsFixedAtOne__ByTheseFixings.add(inputFixing.varName);
                }
            } else {
                //tuple is 0 fixed
                if (this.zeroFixingsMap.containsKey( inputFixing.varName)) {
                    cubeVarsFixedAtZero__ByTheseFixings.add(inputFixing.varName);
                } 
            }
        }
        
        if (cubeVarsFixedAtOne__ByTheseFixings.size()==ONE){
            //all vars in this cube, except this 1 fixed var, must be fixed at 0
            List<String> zeroFixedVarsInCube_AnotherCopy = new ArrayList<>();
            zeroFixedVarsInCube_AnotherCopy.addAll(this.zeroFixingsMap.keySet() );
            zeroFixedVarsInCube_AnotherCopy.removeAll( cubeVarsFixedAtOne__ByTheseFixings );

            result.fixingList = new ArrayList<VariableCoefficientTuple> () ;
            for (String varible: zeroFixedVarsInCube_AnotherCopy){
                result.fixingList.add( new VariableCoefficientTuple( varible, ZERO));
            }
        }else if (cubeVarsFixedAtOne__ByTheseFixings.size()>ONE){
            result.isInfeasibilityDetected=true;
        } else {
            //no 1 fixings in cube
            if (cubeVarsFixedAtZero__ByTheseFixings.size()==this.zeroFixingsMap.size()){
                //all 0 fixed
                result.isInfeasibilityDetected=true;
            }else if (ONE+cubeVarsFixedAtZero__ByTheseFixings.size()==this.zeroFixingsMap.size()){
                //leftover var is 1 fixed
                List<String> zeroFixedVarsInCube_AnotherCopy = new ArrayList<>();
                zeroFixedVarsInCube_AnotherCopy.addAll(this.zeroFixingsMap.keySet() );
                zeroFixedVarsInCube_AnotherCopy.removeAll(cubeVarsFixedAtZero__ByTheseFixings);
                String leftOverVariable = zeroFixedVarsInCube_AnotherCopy.get(ZERO);
                result.fixingList = new ArrayList<VariableCoefficientTuple> () ;
                result.fixingList.add( new VariableCoefficientTuple(leftOverVariable, ONE));
            }
        }
               
        return result;
    }
    
    */
    
    //get var fixing, if any
    //if even a single var mismatches the cubes fixings, then there is no fixing  
    //if more than 1 var is left unmatched, there is no fixing  
    //if exactly 1 var  is left unmatched, there is one fixing (its opposite fixing)
    //if all match, return infeasible node
    //
    //note that if a fixing is resulting in infeasibility, we should branch on this variable
    //
    public  HyperCubeVariableFixingResult getResultOfVarFixings ( Map<String, Boolean> inputVarFixings  ){
       
        HyperCubeVariableFixingResult result = new HyperCubeVariableFixingResult ();
        
        List<String> zeroFixedVarsInCube_Copy = new ArrayList<>();
        zeroFixedVarsInCube_Copy.addAll(this.zeroFixingsMap.keySet() );
        List<String> oneFixedVarsInCube_Copy  = new ArrayList<>();
        oneFixedVarsInCube_Copy.addAll( this.oneFixingsMap.keySet());
        
        Map<String, Boolean>  relevantInputVarFixings = 
                getRelevantInputVarFixings(inputVarFixings, zeroFixedVarsInCube_Copy, oneFixedVarsInCube_Copy) ;
         
        for (Entry <String, Boolean>    inputFixing: relevantInputVarFixings.entrySet()  ){
            
            if ( inputFixing.getValue() ){
                //tuple is 1 fixed
                
                oneFixedVarsInCube_Copy.remove(inputFixing.getKey()) ;
                
                if (zeroFixedVarsInCube_Copy.contains(inputFixing.getKey() )){
                    //mismatch
                    result.isMismatch=true;
                    break;
                }                    
                
            }else {
                //tuple is 0 fixed
                zeroFixedVarsInCube_Copy.remove( inputFixing.getKey());
                if (oneFixedVarsInCube_Copy.contains(inputFixing.getKey() )){
                    //mismatch
                    result.isMismatch=true;
                    break;
                }
            }
        }
        
        if (! result.isMismatch){
            int size = zeroFixedVarsInCube_Copy.size()+oneFixedVarsInCube_Copy.size();
            if (size==ZERO){
                result.isInfeasibilityDetected= true;
            }else if (size==ONE){                
                if (zeroFixedVarsInCube_Copy.size()==ONE){                     
                    result.fixingMap = new TreeMap<String, Boolean> ();
                    result.fixingMap.put( zeroFixedVarsInCube_Copy.get(ZERO), true);
                }else {
                     result.fixingMap = new TreeMap<String, Boolean> ();
                    result.fixingMap.put(oneFixedVarsInCube_Copy.get(ZERO), false);
                }
            }else {
                //no fixing results, do nothing
            }
        }
        
        return result;
    }
    
        
    //toString() 
    public String printMe () {
        String result =   " ZERO  ";
        for (String var:   this.zeroFixingsMap.keySet()){
            result +=var +" ";
        }
        
        result = result + " ONE ";
        for (String var:   this.oneFixingsMap.keySet()){
            result +=var +" ";
        }
         
        result += " "+ this.isMarkedAsMerged;
         
        //System.out.println(result) ;
        return result ;
    }
    
    //get subset of map, without doing sequential search on keys
    private  Map<String, Boolean>       getRelevantInputVarFixings(
            Map<String, Boolean> inputVarFixings, 
            List<String> zeroFixedVarsInCube_Copy, 
            List<String> oneFixedVarsInCube_Copy) {
        
        Map<String, Boolean>  result = new HashMap<String, Boolean> ();
        
        for (String str : zeroFixedVarsInCube_Copy){
            Boolean fixing = inputVarFixings.get(str);
            if (null!=fixing) {
                result.put (str,fixing );
            }
        }
         
        for (String str :  oneFixedVarsInCube_Copy){
            Boolean fixing = inputVarFixings.get(str);
            if (null!=fixing) {
                result.put (str,fixing );
            }
        }
         
        return result;
        
        
    }
}

