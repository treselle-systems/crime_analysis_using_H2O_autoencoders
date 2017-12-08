package com.treselle.util;

import org.springframework.stereotype.Component;

@Component
public class UtilHelper {

	private static boolean useDecimalOutput = false;
	
	public static String myDoubleToString(double d) {
	    if (Double.isNaN(d)) {
	      return "NA";
	    }
	    return useDecimalOutput? Double.toString(d) : Double.toString(d);
	  }
}
