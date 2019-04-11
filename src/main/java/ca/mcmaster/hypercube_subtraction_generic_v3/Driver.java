/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3;

import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import static ca.mcmaster.hypercube_subtraction_generic_v3.CplexParameters.*;
import static ca.mcmaster.hypercube_subtraction_generic_v3.Parameters.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.collection.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.common.*;
import ca.mcmaster.hypercube_subtraction_generic_v3.cplex.BaseCplexSolver;
import ca.mcmaster.hypercube_subtraction_generic_v3.cplex.TraditionalCplexSolver;
import ca.mcmaster.hypercube_subtraction_generic_v3.cplex.staticPriority.StaticCplexSolver;
import ca.mcmaster.hypercube_subtraction_generic_v3.utils.MIPReader;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 * 
 * Driver for CSAT - a CPLEX based sat solver
 * 
 * if any level1 hypercubes exist, branch on the variable
 * 
 * if any level 2 variables exist having both polarities, strong branch on them
 * 
 * use our version of MOMS heuristic for all other branching
 * 
 */
public class Driver {
    
    
    public static Map<String, Double> objectiveFunctionMap;
    public  static List<LowerBoundConstraint> mipConstraintList ;
    public static  TreeMap<String, IloNumVar> mapOfAllVariablesInTheModel = new TreeMap<String, IloNumVar> ();
    public static Map<String, Integer> mapOfVariableFrequencyInConstraints = new HashMap<String ,Integer> ();
         
    //used to limit BCP to level 2
    public   static int ARE_ALL_VARS_SAME_SIGN  ;
    public static boolean DOES_MIP_HAVE_TWO_VARIABLES_IN_EVERY_CONSTRAINT;
    public static boolean IS_THIS_SET_PARTITIONING = false;
    
    private static CollectedInfeasibleHypercubeMap collectedHypercubeMap = new CollectedInfeasibleHypercubeMap();
       
    
    private static Logger logger=Logger.getLogger(Driver.class);
    static {
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  
                RollingFileAppender(layout,LOG_FOLDER+Driver.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(SIXTY);
            logger.addAppender(rfa);
            logger.setAdditivity(false);            
             
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    }
    
    public static void main(String[] args) throws Exception {
           
        logger.info("Start !") ;
        
        try {
            
         
            
            
            IloCplex mip =  new IloCplex();
            mip.importModel(MIP_FILENAME);
            
            logger.info ("preparing allVariablesInModel ... ");
            for (IloNumVar var :MIPReader.getVariables(mip)){
                mapOfAllVariablesInTheModel.put (var.getName(), var );
            }
            System.out.println ("DONE preparing vars. Total is  "+ mapOfAllVariablesInTheModel.size());  
            
            IS_THIS_SET_PARTITIONING = MIPReader.isThisSetpartitioning (mip);
                 
            logger.info ("preparing constraints ... ");
            mipConstraintList= MIPReader.getConstraintsFast(mip);
            logger.info ("finding var frequency in constraints ... ");
            
            //find var frequency in constraints. This is used for sorting the variable order withiin the constraint
            initVariableFrequencyInConstraints();
            
            //sort every constraint expression
            if (SORT_THE_CONSTRAINTS){
                logger.info ("sorting constraints ... ");
                for (LowerBoundConstraint lbc : mipConstraintList){
                    //
                    lbc.sort();
                }
            }
            
            logger.info ("DONE preparing constraints ... ");
            System.out.println ("DONE preparing constraints. Total is  "+ mipConstraintList.size());  
            
            
            
            logger.info ("  preparing objective ... ");
            objectiveFunctionMap = MIPReader.getObjective(mip);
             
             
            
            //we have read the MIP, now start the actual work !
            
            //collect hypercubes
            boolean isMIPInfeasible= runOneRoundOfHypercubeCollection (false) ;
        
            int numberOfAdditionalCollectionRounds = ZERO;            
            for (;numberOfAdditionalCollectionRounds< Parameters.NUMBER_OF_ADDITIONAL_HYPERCUBE_COLLECTION_ROUNDS;numberOfAdditionalCollectionRounds++ ){
                if (isMIPInfeasible) break;
                logger.info (" Starting additional hypercube collection round "+numberOfAdditionalCollectionRounds) ;
                
                isMIPInfeasible= runOneRoundOfHypercubeCollection (true) ;
            }
            
            
            if (isMIPInfeasible) {
                //no need for branching
                System.out.println("Mip is infeasible");
                exit(ZERO);
            } 
            
            if (ABSORB_COLLECTED_HYPERCUBES){
                //this call is needed to remove cubes marked as merged or absorbed
                collectedHypercubeMap.getCollectedCubes();
                //now we invoke absorb
                collectedHypercubeMap .absorb();
            }
            
            collectedHypercubeMap.printCollectedHypercubes(true);
            
            
            //this call is needed to remove cubes marked as merged or absorbed
            collectedHypercubeMap.getCollectedCubes();
            
            //check if all vars in mip are same sign, this is a final variable
            ARE_ALL_VARS_SAME_SIGN = areAllvarsOfSameSign();
            DOES_MIP_HAVE_TWO_VARIABLES_IN_EVERY_CONSTRAINT = collectedHypercubeMap.collectedHypercubes.firstKey()==TWO &&
                                                              collectedHypercubeMap.collectedHypercubes.lastKey()==TWO ;
            
            boolean useStaticSolver = false;
            if (  DOES_MIP_HAVE_TWO_VARIABLES_IN_EVERY_CONSTRAINT && DETECT_STATIC_VARIABLE_PRIORITIES ){
                useStaticSolver =true;
            }
            if (FORCE_STATIC_VARIABLE_PRIORITIES) useStaticSolver =true;  
            
            
            printParameters();
            
            //solve with cplex
            BaseCplexSolver cplexSolver =null;
            if (useStaticSolver && !USE_PURE_CPLEX) {
                //only the traditional solver can solve with pure cplex   
                cplexSolver =new StaticCplexSolver ( collectedHypercubeMap.collectedHypercubes);
                logger.info("using static solver" );
            }else {
                cplexSolver=new TraditionalCplexSolver ( collectedHypercubeMap.collectedHypercubes) ;
                logger.info("using traditional solver" );
            }

                                          
            logger.info ("starting cplex solve ... " );
            System.out.println("starting cplex solve ... ");
            cplexSolver.solve( RAMP_UP_DURATION_HOURS,SOLUTION_DURATION_HOURS);
            cplexSolver.printSolution();
            
            
        } catch (Exception ex){
            System.err.println(ex) ;
            ex.printStackTrace();
        } finally {
            logger.info("Completed !") ;
        }
        
    }
    
    //return -1 if not all same sign
    //return 0 if all 0
    //retrn 1 if all 1
    private static int areAllvarsOfSameSign (){
        boolean areAllSameSign = true;
        
        int retval = -ONE;
        
        int zeroCount = ZERO;
        int oneCount = ZERO;
        
        for ( List<HyperCube> cubeList:collectedHypercubeMap.collectedHypercubes.values()){
            for (HyperCube cube: cubeList){
                if (!cube.zeroFixingsMap.isEmpty()){
                    zeroCount++;
                }
                if (!cube.oneFixingsMap.isEmpty()){
                    oneCount++;
                }
                if (oneCount!=ZERO && zeroCount!=ZERO){
                    areAllSameSign=false;
                    break;
                }
            }
            if (!areAllSameSign) break;
        }
        
        if (areAllSameSign){
            //all same sign
            retval = (oneCount!=ZERO) ? ONE:   ZERO;
        } 
        return retval;
    }
           
    private static void initVariableFrequencyInConstraints(){
        for (LowerBoundConstraint lbc : mipConstraintList){
            for (VariableCoefficientTuple tuple : lbc.constraintExpression){
                if (mapOfVariableFrequencyInConstraints.containsKey(tuple.varName)){
                    int currentFreq = mapOfVariableFrequencyInConstraints.get(tuple.varName);
                    mapOfVariableFrequencyInConstraints.put(tuple.varName, ONE +currentFreq);
                     
                }else {
                    mapOfVariableFrequencyInConstraints.put(tuple.varName, ONE);
                    
                }
            }
        }
        
        //print
        for (int freq=ONE; freq <= Collections.max(mapOfVariableFrequencyInConstraints.values()); freq++){
            System.out.print("for freq "+freq);
            int count = ZERO;
            for (Map.Entry<String, Integer> entry : mapOfVariableFrequencyInConstraints.entrySet()) {
                if (entry.getValue()==freq){
                    //System.out.print(entry.getKey());
                    count++;
                }
            }
            System.out.println(" count is "+count);
        }
    }
    
    private static int getNumHypercubes (TreeMap<Integer, List<HyperCube>>  collectedHypercubeMap){
        int count = ZERO;
        for (List<HyperCube> cubeList: collectedHypercubeMap.values()){
            count+=cubeList.size();
        }
        return count;
    } 
    
    //collect hypercubes and return isInfeasible 
    private static boolean runOneRoundOfHypercubeCollection (boolean doShuffle) {
        boolean isMIPInfeasible= false;
        int numConstraintsCollectedFor=ZERO;
        
        for (LowerBoundConstraint lbc : mipConstraintList) {
            //System.out.println("Collected hypercubes for " + lbc.printMe());
            
            if (MIP_FILENAME .contains( "rmine10.mps")   ){
                if ( lbc.name.contains("cap")){
                    System.out.println("skip hypercube collection for capacity constraint "+lbc.name);
                    continue;
                }
            }
            
            if (MIP_FILENAME .contains( "opm2-z12-s7.mps") || MIP_FILENAME .contains( "opm2-z12-s8") ){
                //ignore capacity constraints for cube collection
                List<String> capacityCosntraints = new ArrayList<String>();
                capacityCosntraints.add("c1");
                capacityCosntraints.add("c2");
                capacityCosntraints.add("c3");
                capacityCosntraints.add("c4");
                capacityCosntraints.add("c5");
                capacityCosntraints.add("c6");
                capacityCosntraints.add("c7");
                capacityCosntraints.add("c8");
                if (capacityCosntraints.contains( lbc.name)) continue;
            }
            if (MIP_FILENAME .contains( "supportcase22")){
                //ignore capacity constraints for cube collection
                List<String> capacityCosntraints = new ArrayList<String>();
                capacityCosntraints.add("c0");
                capacityCosntraints.add("c30138");
                capacityCosntraints.add("c30139");
                capacityCosntraints.add("c82258");
                capacityCosntraints.add("c82259");
                capacityCosntraints.add("c134378");
                capacityCosntraints.add("c134379");
                capacityCosntraints.add("c186498");
                capacityCosntraints.add("c186499");
                capacityCosntraints.add("c238618");
                capacityCosntraints.add("c238619");
                         
                  
                 
                if (capacityCosntraints.contains( lbc.name)) continue;
            }
             
            if (MIP_FILENAME .contains( "reblock354")){
                if ( lbc.name.contains("CAP")){
                    System.out.println("skip hypercube collection for capacity constraint "+lbc.name);
                    continue;
                }
            }
            
            if (MIP_FILENAME .contains( "seymour-disj-10")){
                if ( lbc.name.contains("CAP")){
                    System.out.println("skip hypercube collection for capacity constraint "+lbc.name);
                    continue;
                }
            }
            
            if (doShuffle) lbc.shuffle( );
            
            ConstraintAnchoredCollector collector = new ConstraintAnchoredCollector (lbc);
            collector.collectInfeasibleHypercubes();


            if (collector.collectedHypercubeMap.size()>ZERO  ){
                int numCollected = getNumHypercubes(collector.collectedHypercubeMap);
                //System.out.println(" numCollected " + numCollected);
                if (MAX_HYPERCUBES_PER_CONSTRAINT> numCollected){
                    isMIPInfeasible = collectedHypercubeMap .addCubesAndCheckInfeasibility(collector.collectedHypercubeMap );
                }
            } 
            if (isMIPInfeasible) break;

            //System.out.println("\nCollected cubes");
            //collector.printCollectedHypercubes();
            //System.out.println("\nCumulative map ");
            //collectedHypercubeMap.printCollectedHypercubes(false);

            numConstraintsCollectedFor++;
            if (numConstraintsCollectedFor%(HUNDRED)==ZERO) {
                System.out.println("Collected hypercubes for this many constraints "+   numConstraintsCollectedFor  + " just collected "+lbc.name );
            }
        }
        
        return isMIPInfeasible;
    }
    
    
    private static void printParameters() {
        
        logger.info("PROGRAM VERSION "+ VERSION) ; 
        logger.info("MIP_FILENAME "+ MIP_FILENAME) ; 
        logger.info("MIP_EMPHASIS "+ MIP_EMPHASIS) ; 
        
        logger.info("MAX_THREADS "+ MAX_THREADS) ; 
        logger.info("FILE_STRATEGY "+ FILE_STRATEGY) ; 
   
        logger.info ("DISABLE_HEURISTICS "+ DISABLE_HEURISTICS) ;
        logger.info ("DISABLE_PROBING "+ DISABLE_PROBING) ;
        logger.info ("DISABLE_PRESOLVENODE "+ DISABLE_PRESOLVENODE) ;
        logger.info ("DISABLE_PRESOLVE "+ DISABLE_PRESOLVE) ;
        logger.info ("DISABLE_CUTS "+ DISABLE_CUTS) ;
        
        logger.info ("PERF_VARIABILITY_RANDOM_SEED "+ PERF_VARIABILITY_RANDOM_SEED) ;
    
     
        logger.info ("USE_PURE_CPLEX "+  Parameters.USE_PURE_CPLEX) ;
        logger.info ("NUM_ADJACENT_VERTICES_TO_COLLECT "+ NUM_ADJACENT_VERTICES_TO_COLLECT) ;
        
        logger.info ("MERGE_COLLECTED_HYPERCUBES "+ MERGE_COLLECTED_HYPERCUBES) ;
        logger.info ("ABSORB_COLLECTED_HYPERCUBES "+ ABSORB_COLLECTED_HYPERCUBES) ;
         
        
        logger.info ("RAMP_UP_DURATION_HOURS "+ RAMP_UP_DURATION_HOURS) ;
        logger.info ("SOLUTION_DURATION_HOURS "+ SOLUTION_DURATION_HOURS) ;
        
        logger.info ("SHUFFLE_CONSTRAINT "+ SHUFFLE_THE_CONSTRAINTS) ;
        
        logger.info ("LOOKAHEAD_LEVELS MOMS "+ LOOKAHEAD_LEVELS_MOMS) ;
        logger.info ("MAX_DEPTH_LEVELS_JERRY_WANG "+ Parameters.MAX_DEPTH_LEVELS_JERRY_WANG) ;
        logger.info ("SORT_THE_CONSTRAINT "+ SORT_THE_CONSTRAINTS) ;
        
        logger.info ("NUMBER_OF_ADDITIONAL_HYPERCUBE_COLLECTION_ROUNDS "+ NUMBER_OF_ADDITIONAL_HYPERCUBE_COLLECTION_ROUNDS) ;
        
        logger.info ("CHECK_FOR_DUPLICATES "+ CHECK_FOR_DUPLICATES) ; 
        
        logger.info ("MAX_HYPERCUBES_PER_CONSTRAINT "+  MAX_HYPERCUBES_PER_CONSTRAINT );
        
        logger.info ("CPLEX_RANDOM_SEED "+  CPLEX_RANDOM_SEED );
         
        logger.info ("HEURISTIC_TO_USE "+  HEURISTIC_TO_USE );
        
        logger.info ("USE_BCP_LEVEL "+  USE_BCP_LEVEL );
        logger.info (" allVarsSameSign "+  Driver.ARE_ALL_VARS_SAME_SIGN );
        logger.info ("EXCLUDE_CPLEX_LP_INTEGRAL_VARS "+  EXCLUDE_CPLEX_LP_INTEGRAL_VARS );
                
        logger.info ("USE_ONLY_MAX_PSEDUDO_COST_VARS  "+  USE_ONLY_MAX_PSEDUDO_COST_VARS  );
        
        logger.info ("ENABLE_EQUIVALEN_TTRIGGER_CHECK_FOR_BCP  "+  ENABLE_EQUIVALENT_TRIGGER_CHECK_FOR_BCP  );
        
        logger.info ("IS_THIS_SET_PARTITIONING  "+  IS_THIS_SET_PARTITIONING  );
        
                
        //logger.info ("USE_BARRIER_AND_REDUCED_COSTS  "+  USE_BARRIER_AND_REDUCED_COSTS  );
        
        //logger.info ("USE_ONLY_MAX_INFEASIBLE_VARS  "+  USE_ONLY_MAX_INFEASIBLE_VARS  );
        
        logger.info ("FORCE_STATIC_VARIABLE_PRIORITIES  "+  FORCE_STATIC_VARIABLE_PRIORITIES  );
        logger.info ("DETECT_STATIC_VARIABLE_PRIORITIES  "+  DETECT_STATIC_VARIABLE_PRIORITIES  );
        
        logger.info ("CONSIDER_PARTLY_MATCHED_CUBES_FOR_BCP_VOLUME_REMOVAL  "+  CONSIDER_PARTLY_MATCHED_CUBES_FOR_BCP_VOLUME_REMOVAL  );
        
        logger.info (" ENABLE_TWO_SIDED_BCP_METRIC " + ENABLE_TWO_SIDED_BCP_METRIC);
        logger.info (" ENABLE_BCP_METRIC_NUMBER_OF_VARIABLES_FIXED " + ENABLE_BCP_METRIC_NUMBER_OF_VARIABLES_FIXED);
        
                
    }    
    
}
