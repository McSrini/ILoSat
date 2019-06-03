/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.cplex;
   
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.*;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.time.Duration;
import java.time.Instant;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public   class StaticticsCallback extends IloCplex.NodeCallback{ 

    //for statistics   
    public long numberOFLeafs = ZERO;
    public long numberOFNodesProcessed = ZERO;
    public double bestKnownSOlution ; 
    public double bestKnownBound ;
    
    protected void main() throws IloException {
         
        numberOFLeafs =getNremainingNodes64();
        numberOFNodesProcessed=getNnodes64();
        bestKnownSOlution = getIncumbentObjValue();
        bestKnownBound=getBestObjValue();     
        abort();
    }
   
}
