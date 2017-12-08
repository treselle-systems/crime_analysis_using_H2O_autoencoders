/**
 * 
 */
package com.treselle.crime.helper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class CSVParser {
	
	private final static Logger LOGGER = Logger.getLogger(CSVParser.class);
	
	/**
	 * This method is to read the CSV file and append it into List.
	 * 
	 * @param filePath
	 * @return crimeList
	 */
	public List<Map<String, Object>> readCSV(String filePath) {
        String line = "";
        List<Map<String, Object>> crimeList = new ArrayList<Map<String, Object>>();
        Map<String, Object> crimeMap = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            while ((line = br.readLine()) != null) {
            	crimeMap = new LinkedHashMap<String, Object>();
                String[] user = line.split(",");
                crimeMap.put("area_id", user[0]);
                crimeMap.put("rpt_dist_no", user[12]);
                crimeMap.put("crm_cd", user[2]);
                crimeMap.put("vict_age", user[18]);
                crimeMap.put("vict_sex", user[17]);
                crimeMap.put("vict_descent", user[16]);
                crimeMap.put("premis_desc", user[11]);
                crimeMap.put("weapon_desc", user[19]);
                crimeList.add(crimeMap);
            }
            br.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
        CSVParser.LOGGER.info("Crime List is :"+crimeList.size());
        return crimeList;
	}

}
