/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.cplex;
 
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static ilog.cplex.IloCplex.IncumbentId;
import java.io.File;
import static java.lang.System.exit;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public  abstract class BaseCplexSolver {
    
    protected static Logger logger = Logger.getLogger(BaseCplexSolver.class);;
    protected IloCplex cplex ;
    
        
    static {
        logger.setLevel(LOGGING_LEVEL );
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  RollingFileAppender(layout,LOG_FOLDER+BaseCplexSolver.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(SIXTY);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
    
    public BaseCplexSolver (){
        
    }
    
    public abstract void solve (int rampUpDurationHours, int solveDurationHours  ) throws IloException;
     
    public void printSolution () throws IloException{
        if (cplex.getStatus().equals(IloCplex.Status.Optimal)|| cplex.getStatus().equals(IloCplex.Status.Feasible)){
            //print vector and value of incumbent
            printIncumbent() ;
        } else {
            logger.info ("status is " + cplex.getStatus());
            if (!cplex.getStatus().equals(IloCplex.Status.Infeasible) ) logger.info ( " and bound is "+ cplex.getBestObjValue()) ;
        }
    }
    
     
    
    public void printIncumbent() throws IloException {
        logger.info ("best known solution is " + cplex.getObjValue()) ;
        logger.info ("best known bound is " + cplex.getBestObjValue()) ;
        logger.info ("status is " + cplex.getStatus()) ;
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        for (IloNumVar var :  lpMatrix.getNumVars()) {            
             logger.info ("var is " + var.getName() + " and is soln value is " + cplex.getValue(var, IncumbentId )) ;
        }
    }
    
    public static boolean isHaltFilePresent (){
        File file = new File("haltfile.txt");         
        return file.exists();
    }
    
    
}
