/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.hypercube_subtraction_generic_v3.common;
  
import static ca.mcmaster.hypercube_subtraction_generic_v3.Constants.ZERO;
import ca.mcmaster.hypercube_subtraction_generic_v3.Driver;


/**
 *
 * @author tamvadss
 */
public class VariableCoefficientTuple  implements Comparable{
    
    public String varName ;
    public double coeff;
    
    public VariableCoefficientTuple (String varname, double coefficient)   {
    
        this.varName  =  varname; 
        coeff =coefficient;
    }
       
    //lowest frequency variable first, and if freq is same then lowest magnitude coeff first 
    public int compareTo(Object other) {
        
        int myFreq = Driver.mapOfVariableFrequencyInConstraints.get(this.varName);
        int otherFreq = Driver.mapOfVariableFrequencyInConstraints.get(((VariableCoefficientTuple)other).varName);
        
        int result = Double.compare( myFreq, otherFreq);
        if (ZERO== result) {
            result= Double.compare(Math.abs(this.coeff ), Math.abs(  ((VariableCoefficientTuple)other).coeff ));
             
        }
               
        return result;
    } 
}
