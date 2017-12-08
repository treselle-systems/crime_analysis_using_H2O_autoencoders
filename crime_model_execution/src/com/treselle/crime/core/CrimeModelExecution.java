/**
 * 
 */
package com.treselle.crime.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treselle.crime.helper.CSVParser;
import com.treselle.crime.helper.URLExecution;

/**
 * 
 * This project is implemented to parse a CSV file and convert it into JSON string.
 * The JSON string will be passed into an API call as POST request. 
 * Here the input is passed in runtime where the location of our input file is passed.
 *
 */
public class CrimeModelExecution {
	
	private static final Logger log             = Logger.getLogger(CrimeModelExecution.class);
	
	public static void main(String args[]) {
		try {
			String filePath = args[0];
			CrimeModelExecution.log.info("File Path is :"+filePath);
			
			CSVParser parser = new CSVParser();
			List<Map<String, Object>> crimeList = parser.readCSV(filePath);
			
			ObjectMapper objectMapper = new ObjectMapper();
			String jsonBody = objectMapper.writeValueAsString(crimeList);
			
			Map<String, Object> requestProperty = new HashMap<String, Object>();
			requestProperty.put("Accept","application/json");
			requestProperty.put("content-type","application/json");
			
			URLExecution executeUrl = new URLExecution();
			executeUrl.executeURL("POST", "http://localhost:8081/crime/crime_check", jsonBody, requestProperty);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
