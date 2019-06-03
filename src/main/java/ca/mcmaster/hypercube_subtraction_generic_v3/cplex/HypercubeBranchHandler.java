/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.cplex; 
  
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntegerFeasibilityStatus; 
import ilog.cplex.IloCplex.NodeId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;  
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.Driver;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Driver.DOES_MIP_HAVE_TWO_VARIABLES_IN_EVERY_CONSTRAINT;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.*;
import static ca.mcmaster.hypercube_subtraction_generic_v3.bcp.BCP_LEVEL_ENUM.ABOVE_AVG_VARS;
import static ca.mcmaster.hypercube_subtraction_generic_v3.bcp.BCP_LEVEL_ENUM.ALL_VARS;
import static ca.mcmaster.hypercube_subtraction_generic_v3.bcp.BCP_LEVEL_ENUM.NO_BCP;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.heuristics.*;
import static ca.mcmaster.hypercube_subtraction_generic_v3.heuristics.BRANCHING_HEURISTIC_ENUM.*;
import static java.lang.System.exit;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class HypercubeBranchHandler extends IloCplex.BranchCallback{
    
    private final TreeMap<Integer, List<HyperCube>>  collectedHypercubes;
    
     
    private static Logger logger=Logger.getLogger(HypercubeBranchHandler.class);
    static {
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender appender = new  RollingFileAppender(layout,
                    LOG_FOLDER+HypercubeBranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION);
            appender.setMaxBackupIndex(SIXTY);
            logger.addAppender(appender);
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
    
    public HypercubeBranchHandler (       TreeMap<Integer, List<HyperCube>>   infeasibleHypercubeMap ) {
        this. collectedHypercubes =infeasibleHypercubeMap;
         
    }
    
    
    protected void main() throws IloException {
        if ( getNbranches()> ZERO ){  
             
            boolean isMipRoot = ( getNodeId().toString()).equals( MIP_ROOT_ID);
                       
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            
            if (isMipRoot){
                //root of mip
                
                NodePayload data = new NodePayload (  );
                data.infeasibleHypercubesMap=this.collectedHypercubes;
                setNodeData(data);                
            } 
            
            NodePayload nodeData = (NodePayload) getNodeData();
            
            if (nodeData!=null) {
                overruleCplexBranching( nodeData );
            }else {
                //take default cplex branching
                logger.warn("taking default cplex branching at node for lack of node data"+ getNodeId()) ;
                
            }
            
            
        } //nbranches >0   
    }
    
    private void overruleCplexBranching ( NodePayload nodeData  ) throws IloException{
        
            //the first thing to do in the branch callback is to find the var fixings, and 
            //filter our hypercube collection
            TreeMap<String, Boolean> thisNodesVarFixings = this.getCplexFixedVars(nodeData.parentVarFixings.keySet());
            
            //this.print_VarFixings(thisNodesVarFixings);
            
            // this.printInfeasibleHyperCubeMap(nodeData.infeasibleHypercubesMap );
            
            
             
            
            //for these   fixings, create a local infeasible hypercube map
            TreeMap<Integer, List<HyperCube>>  filterResult  = 
                    getFilteredHypercubes(nodeData.infeasibleHypercubesMap, thisNodesVarFixings);
            
             
                      
            //this.printInfeasibleHyperCubeMap(filterResult.filteredHypercubes);
            
            if (filterResult.size()==ZERO) {
                 
                logger.warn("  no cubes at  "+ getNodeId() + " will take cplex default branching.");
                
            } else {
                
                //we use our heuristic to make the branching decision
                    
                //first restore this nodes var fixings to inlude those of parent, these will be passed to both kids
                for (Entry <String, Boolean> parentFixing: nodeData.parentVarFixings.entrySet()){                
                    thisNodesVarFixings .put(parentFixing.getKey(),parentFixing.getValue() );
                }    

                //this.printVarFixings(thisNodesVarFixings);
                
                //we now try to find our candidate branching vars, according to our heuristic
                List<String> candidateBranchingVars =null;
                
                BaseHeuristic branchingHeuristic =BranchingHeuristicFactory.getBranchingHeuristic();
                //  pass the filtered hypercubes to the branching heuristic
                branchingHeuristic.infeasibleHypercubeMap=filterResult;
                
                //prepare for BCP                
                if (TWO==filterResult.firstKey() &&   STEPPED_WEIGHT.equals( HEURISTIC_TO_USE) && !USE_BCP_LEVEL.equals(NO_BCP)){
                    //prepare for BCP
                    Set<String> bcpCandidateVars = this.getAllVariables_InTwoSizedHypercubes(filterResult);
                    if (EXCLUDE_CPLEX_LP_INTEGRAL_VARS) bcpCandidateVars=  this.getIntegerInfeasibleVariables( bcpCandidateVars);
                
                    if (USE_ONLY_MAX_PSEDUDO_COST_VARS) bcpCandidateVars = getVarsWithLargestPSeudoCosts (bcpCandidateVars);
                    
                    //if ( USE_ONLY_MAX_INFEASIBLE_VARS) bcpCandidateVars = getMostInfeasibleVaribales(bcpCandidateVars) ;
                           
                    branchingHeuristic.variablesToUseForBCP = USE_BCP_LEVEL.equals(ALL_VARS) ?bcpCandidateVars:
                            getHighFreqVariables_InTwoSizedHypercubes (bcpCandidateVars, filterResult);                       
                    
                    //System.out.println("Size of variablesToUseForBCP "+branchingHeuristic.variablesToUseForBCP.size());
                    
                }  
                
                //System.out.println("getting branch suggestion for node " + getNodeId());
                candidateBranchingVars = branchingHeuristic.getBranchingVariableSuggestions( );
                
                
                //System.out.println("candidateBranchingVars "+ candidateBranchingVars.size());
                 
                
                if (candidateBranchingVars!=null && candidateBranchingVars.size()!=ZERO) {
                    
                    //pick one candidate at random
                    int randomPosition = PERF_VARIABILITY_RANDOM_GENERATOR.nextInt(candidateBranchingVars.size());
                    String branchingVarDecision = candidateBranchingVars.get(randomPosition );

                    // vars needed for child node creation 
                    IloNumVar[][] vars = new IloNumVar[TWO][] ;
                    double[ ][] bounds = new double[TWO ][];
                    IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
                    getArraysNeededForCplexBranching(branchingVarDecision, vars , bounds , dirs);

                    //create both kids, pass on infeasible hypercubes from parent      

                    double lpEstimate = getObjValue();

                    NodePayload zeroChildData = new NodePayload (  );
                    zeroChildData.infeasibleHypercubesMap= filterResult;
                    zeroChildData.parentVarFixings= thisNodesVarFixings;
                    NodeId zeroChildID =  makeBranch( vars[ZERO][ZERO],  bounds[ZERO][ZERO],
                                                      dirs[ZERO][ZERO],  lpEstimate  , zeroChildData );

                    NodePayload    oneChildData = zeroChildData;
                    NodeId oneChildID = makeBranch( vars[ONE][ZERO],  bounds[ONE][ZERO],
                                                         dirs[ONE][ZERO],   lpEstimate, oneChildData );
                    
                    
                    /*System.out.println( getNodeId() + " zero child created with " + vars[ZERO][ZERO].getName() + 
                            " bound " + bounds[ZERO][ZERO] +
                            " dir " +  dirs[ZERO][ZERO] +
                            " id " + zeroChildID);
                    System.out.println( getNodeId() + " one  child created with " + vars[ONE][ZERO].getName() + 
                            " bound " + bounds[ONE][ZERO] +
                            " dir " +  dirs[ONE][ZERO] +
                            " id " + oneChildID);*/
                    

                }else {
                    //no candidates , so take cplex default branching
                    logger.warn("Took CPLEX default branch at node "+ getNodeId()+ " for lack of candidates");
                }

            }
    }
    
    
    //get var fixings relevant to this cube
    private TreeMap<String, Boolean> getRelevantVarFixings (HyperCube cube,  TreeMap<String, Boolean> thisNodesVarFixings) {
        
        TreeMap<String, Boolean> result = new TreeMap<String, Boolean> ();
        
        for (String var :cube.zeroFixingsMap.keySet()){
            Boolean fixing= thisNodesVarFixings.get(var) ;
            if (null!=fixing){
                result.put (var, fixing);
            }
        }
        for (String var :cube.oneFixingsMap.keySet()){
            Boolean fixing= thisNodesVarFixings.get(var) ;
            if (null!=fixing){
                result.put (var, fixing);
            }
        }
        
        return result;
    }
  
    /**
     * 
     * @param hyperCubeMap
     * @param thisNodesVarFixings  fixed to true if 1 fixing
     * @return cubes that pass this node's fixings
     * @throws IloException 
     */
    private TreeMap<Integer, List<HyperCube>>    getFilteredHypercubes (TreeMap<Integer, List<HyperCube>> hyperCubeMap ,
            TreeMap<String, Boolean> thisNodesVarFixings) throws IloException{
        
        TreeMap<Integer, List<HyperCube>> resultCubeMap = new TreeMap<Integer, List<HyperCube>>();
        
        
        for (Entry <Integer, List<HyperCube>> entry : hyperCubeMap.entrySet()){
            
            for (  HyperCube cube:entry.getValue()){
                
                //filter on every var fixing
                HyperCube filteredCube = cube;
                //keep track of how much the key changes.  
                int decreaseInSize = ZERO;
                
                for (Entry<String, Boolean>  fixing :  getRelevantVarFixings(cube, thisNodesVarFixings).entrySet()    ){
                    filteredCube = filteredCube.filter( fixing.getKey(),   fixing.getValue()  );
                    //System.out.println("key is " + fixing.getKey() + " and val is "+ fixing.getValue()) ;
                    if (null ==filteredCube) {
                        //this cube does not pass these fixings, i.e. it does not belong in this node
                        //no need to check more fixings for this cube                        
                        break;
                    }
                                       
                    //find the updated key value                    
                    if (!filteredCube.isFilterResult_simplePassThrough) decreaseInSize ++;  
                    
                    
                    if(  entry.getKey() ==decreaseInSize) {
                        
                        //infeasible
                        //do not pass this hypercube
                        //leave it to cplex to detect infeasibility of this node
                        filteredCube=null;
                        break;
                    }
                    
                }
                 
                
                //if filtered cube is not null, collect it
                //note that we do not do merge and absorb here, although we can (should?)
                if (null !=filteredCube){    
                    Integer newKey =   entry.getKey()- decreaseInSize;
                    List<HyperCube> currentCubes = resultCubeMap.get( newKey);
                    if (null==currentCubes) currentCubes = new ArrayList<HyperCube>();
                    currentCubes.add(filteredCube);
                    resultCubeMap.put(newKey, currentCubes );                    
                }       
                
            }//iterate cubes at this level of infeasible hypercube map
             
            
        }//iteration over entire input hypercube map
        
       
        return resultCubeMap;
    }
    
       
    private TreeMap<String, Boolean> getCplexFixedVars ( Set<String> alreadyKnown) throws IloException{
        TreeMap<String, Boolean>  fixedVars = new TreeMap<String, Boolean>  ();
        
        int numVarsInModel =Driver.mapOfAllVariablesInTheModel.values().size();
        IloNumVar[] varArray =  new IloNumVar[numVarsInModel];
        int index = ZERO;
        for (IloNumVar var :  Driver.mapOfAllVariablesInTheModel.values()){
            varArray[index++] = var;
        }
        
        
        double[] ubValues =  getUBs (varArray) ;
        double[] lbValues =  getLBs(varArray);
        
        index = -ONE;
        for (IloNumVar var : varArray  ){
            index ++;
            if (alreadyKnown.contains(var.getName() )) continue ;
            
            Double upper = ubValues[index];
            Double lower = lbValues[index];
            
            if (  ZERO == Long.compare(Math.round(upper),Math.round(lower))){
                //value is false if 0 fixing
                fixedVars.put(var.getName(), ZERO!=   Long.compare( Math.round(lower), Math.round(ZERO))  );
                //System.out.println(" fixed var by cplex "+  var.getName() + " " + lower);
            }
        } 
        return fixedVars;
    }
    
    
   
    
    
    private void getArraysNeededForCplexBranching (String branchingVar,IloNumVar[][] vars ,
                                                   double[ ][] bounds ,IloCplex.BranchDirection[ ][]  dirs ){
        
        IloNumVar branchingCplexVar = Driver.mapOfAllVariablesInTheModel.get(branchingVar );
        
         
        //    System.out.println("branchingCplexVar is "+ branchingCplexVar);
         
        
        //get var with given name, and create up and down branch conditions
        vars[ZERO] = new IloNumVar[ONE];
        vars[ZERO][ZERO]= branchingCplexVar;
        bounds[ZERO]=new double[ONE ];
        bounds[ZERO][ZERO]=ZERO;
        dirs[ZERO]= new IloCplex.BranchDirection[ONE];
        dirs[ZERO][ZERO]=IloCplex.BranchDirection.Down;

        vars[ONE] = new IloNumVar[ONE];
        vars[ONE][ZERO]=branchingCplexVar;
        bounds[ONE]=new double[ONE ];
        bounds[ONE][ZERO]=ONE;
        dirs[ONE]= new IloCplex.BranchDirection[ONE];
        dirs[ONE][ZERO]=IloCplex.BranchDirection.Up;
    }
 

    
    private void print_InfeasibleHyperCubeMap( TreeMap<Integer, List<HyperCube>>   hypercubes){
        logger.info("Printing Infeasible hypercubes " + hypercubes.size());
        for (Entry <Integer, List<HyperCube>> entry : hypercubes.entrySet()){
            logger.info(entry.getKey() );
            for ( HyperCube cube: entry.getValue()){
                logger.info(cube.printMe() );
            }             
        }
    }
    
    private void print_VarFixings (TreeMap<String, Boolean> varFixings) {
        logger.info("Printing Var Fixings " +varFixings.size() );
        for (Entry <String, Boolean> entry : varFixings.entrySet()){
            logger.info(entry.getKey() + " , " + entry.getValue());
        }
    }
    
    private Set<String> getIntegerInfeasibleVariables( Set<String> candidates) throws IloException{
        Set<String>   filteredVars= new  HashSet<String> ();
        
        if (candidates.size()>ZERO ){
            IloNumVar[] allVariables = new  IloNumVar[candidates.size()] ;
            int index =ZERO;
            for  (String var : candidates) {
                //
                allVariables[index++] = Driver.mapOfAllVariablesInTheModel .get(var);
            }
            IntegerFeasibilityStatus [] status =   getFeasibilities(allVariables);

            index =-ONE;
            for (IloNumVar var: allVariables){
                index ++;
                //check if candidate is integer infeasible in the LP relax
                if (status[index].equals( IntegerFeasibilityStatus.Infeasible)) {
                    filteredVars.add(var.getName() );
                }
            }
        }
                
        return filteredVars;
    }
    
    //use when we only have 2 sized hypercubes with only 0 fixings, as in set partitioning
    /*private Set<String>getVariables_InTwoSizedHypercubes_perPair (TreeMap<Integer, List<HyperCube>>  filterResult){
        //our return value
        Set<String> resultSet = new HashSet<String> ();  
        
        Set<String> excluded = new HashSet<String> ();  
       
        for (HyperCube cube : filterResult.get (TWO)){
            //the first var, if not already excluded, gets in
            String firstVar = cube.zeroFixingsMap.firstKey();
            String secondVar = cube.zeroFixingsMap.lastKey();
            if (!excluded.contains(firstVar ) && !excluded.contains(secondVar )){
                resultSet.add(firstVar);
            }
            excluded.addAll( cube.zeroFixingsMap.keySet());
        }   
         
        return resultSet;
    }*/
    
    //return  variables in all0 cubes of smallest size
    /*private Set<String>getAllVariables_InSmallestAllZeroHypercubes (TreeMap<Integer, List<HyperCube>>  filterResult){
        //our return value
        Set<String> resultSet = new HashSet<String> ();  
        
        boolean isAllZeroCubeFound = false;
        
        for (Entry <Integer, List<HyperCube>>  entry :filterResult.entrySet()){
            for (HyperCube cube :  entry.getValue()){
                
                if (cube.oneFixingsMap.isEmpty()) {
                    isAllZeroCubeFound= true;
                    resultSet.addAll(cube.zeroFixingsMap.keySet());
                }
            }
            
            //do not investigate next level if isAllZeroCubeFound at this level
            if (isAllZeroCubeFound) break;
        }
        
        return resultSet;
    }*/
    
    private Set<String>getAllVariables_InTwoSizedHypercubes (TreeMap<Integer, List<HyperCube>>  filterResult){
        //our return value
        Set<String> resultSet = new HashSet<String> ();  
        
       
        for (HyperCube cube : filterResult.get (TWO)){
            
            /*if (Driver.IS_THIS_SET_PARTITIONING) {
                //only consider vars in all 0 cubes
                if (cube.getOneFixingsSize() !=ZERO){
                    continue ;
                }
            }*/
            
            resultSet.addAll(cube.zeroFixingsMap.keySet());
            resultSet.addAll(cube.oneFixingsMap.keySet());
        }   
        
         
        
        //System.out.println("Size of size two vars "+resultSet.size());
        return resultSet;
    }
    
    private Set<String> getHighFreqVariables_InTwoSizedHypercubes (Set<String>  candidates, TreeMap<Integer, List<HyperCube>>  filterResult) {
        //our return value
        Set<String> resultSet = new HashSet<String> (); 
        
        if (DOES_MIP_HAVE_TWO_VARIABLES_IN_EVERY_CONSTRAINT  ) {
            //simply use frequencies counted at the outset
             
            double averageFreq =ZERO;
            int maxFreq =ZERO;
            for (Entry <String, Integer> entry : Driver.mapOfVariableFrequencyInConstraints.entrySet()){
                if (!candidates.contains ( entry.getKey())) continue;
                int value = entry.getValue();
                averageFreq+=value;
                if (maxFreq<value) maxFreq=value;
            }
            averageFreq= averageFreq/ candidates.size();



            for ( String var : candidates ){
                if (Double.compare(  USE_BCP_LEVEL.equals( ABOVE_AVG_VARS) ?averageFreq: maxFreq,
                        Driver.mapOfVariableFrequencyInConstraints.get( var))<=ZERO){
                    resultSet.add(var);
                }
            }
            
        }else {
            resultSet =getHighFreqVariables_InTwoSizedHypercubes_Full(candidates, filterResult);
        }
        
        return resultSet;
    }
    
    private Set<String> getHighFreqVariables_InTwoSizedHypercubes_Full (Set<String>  candidates, TreeMap<Integer, List<HyperCube>>  filterResult) {
        //our return value
        Set<String> resultSet = new HashSet<String> (); 
         
        Map<String, Integer> freqMap  = new HashMap<String, Integer>();
        
        for (HyperCube cube : filterResult.get (TWO)){
            for (String var : cube.zeroFixingsMap.keySet()) {
                if (!candidates.contains(var))continue;
                if (freqMap.containsKey(var)){
                    int freq = freqMap .get(var);
                    freqMap .put(var,ONE+freq) ;
                }else{
                    freqMap .put(var,ONE) ;
                }
            }
            for (String var : cube.oneFixingsMap.keySet()) {
                if (!candidates.contains(var))continue;
                if (freqMap.containsKey(var)){
                    int freq = freqMap .get(var);
                    freqMap .put(var,ONE+freq) ;
                }else{
                    freqMap .put(var,ONE) ;
                }               
            }                 
        } 
        
        double averageFreq =ZERO;
        int maxFreq =ZERO;
        for (int value : freqMap.values()){
            averageFreq+=value;
            if (maxFreq<value) maxFreq=value;
        }
        averageFreq= averageFreq/ freqMap.size();
        
        
        
        for (Entry <String, Integer> entry :freqMap.entrySet() ){
            if (Double.compare(  USE_BCP_LEVEL.equals( ABOVE_AVG_VARS) ?averageFreq: maxFreq,entry.getValue())<=ZERO){
                resultSet.add(entry.getKey());
            }
        }
        
        //System.out.println("resultSet size "+resultSet);
        return resultSet;
    }
   
    
    private Set<String> getVarsWithLargestPSeudoCosts (Collection<String> candidateList) throws IloException{
        Set<String> result = new HashSet<String> ();
        double highestKnownPseudoCost = -ONE;
        for (Entry <String, IloNumVar> entry : Driver.mapOfAllVariablesInTheModel.entrySet()){
            
            if (! candidateList.contains( entry.getKey()) ) continue;
            
            //System.out.println("" + getUpPseudoCost(entry.getValue()) +" "+ getDownPseudoCost(entry.getValue())) ;
            
            double higher = Math.max (getUpPseudoCost(entry.getValue()), getDownPseudoCost(entry.getValue())) ;
            if (ZERO> Double.compare(highestKnownPseudoCost, higher)){
                highestKnownPseudoCost= higher;
                result.clear();
                result.add(entry.getKey());
            }else if (ZERO== Double.compare(highestKnownPseudoCost, higher)){
                 result.add(entry.getKey());
            }
        }
        return result;
    }
    
    
}
