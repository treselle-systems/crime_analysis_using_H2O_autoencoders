/**
 * 
 */
package com.treselle.controller;

import hex.ModelCategory;
import hex.genmodel.GenModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.exception.PredictException;
import hex.genmodel.easy.prediction.BinomialModelPrediction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import au.com.bytecode.opencsv.CSVReader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treselle.util.UtilHelper;

@RestController
public class CrimeController {
	
	private final static Logger LOGGER = Logger.getLogger(CrimeController.class);
	
	/**
	 * Declaring our Model class Name which is downloaded as POJO object.
	 * crime_pretrained is our model class name, it differs for each model based on user's naming.
	 */
	private static String modelClassName = "crime_pretrained";
	
	/**
	 * This API is to implement BinomialModel Classification prediction.
	 * Here the input is passed as JSON string in POST call. Converting into valid Java Collections using Object Mapper.
	 * Made the ModelClass name as a valid Prediction object using GenModel.
	 * Appended the results in RowData and passed the object into prediction model. The prediction model is based on h2o-genmodel.jar.
	 * 
	 * @param json_string
	 * @return
	 */
	@RequestMapping(value = "/crime_check", method = { RequestMethod.GET, RequestMethod.POST })
	public Object predictCrimeData(@RequestBody String json_string) {
		List<Map<String, Object>> crimeList = null;
		try {
			 ObjectMapper mapper = new ObjectMapper();
			 crimeList = mapper.readValue(json_string, new TypeReference<List<Map<String, Object>>>(){});
			 System.out.println("Crime List"+crimeList);
			
			 hex.genmodel.GenModel rawModel;
			 rawModel = (hex.genmodel.GenModel) Class.forName(modelClassName).newInstance();
			 EasyPredictModelWrapper model = new EasyPredictModelWrapper(rawModel);
			
			 BufferedWriter output = new BufferedWriter(new FileWriter("OUTPUT_PATH"));
			 output.write("predict");
			 output.write(",");
			 output.write("Arrested");
			 output.write(",");
			 output.write("Not Arrested");
			 output.write("\n");
			
			 int iteration = 0;
			RowData data = null;
			for(Map<String, Object> crimeMap : crimeList) {
				
				if(iteration == 0) {
			        iteration++;  
			        continue;
			    }
				data = new RowData();
				data.put("rpt_dist_no", crimeMap.get("rpt_dist_no"));
				data.put("vict_sex", crimeMap.get("vict_sex"));
                data.put("weapon_desc", crimeMap.get("weapon_desc"));
                data.put("crm_cd", crimeMap.get("crm_cd"));
                data.put("premis_desc", crimeMap.get("premis_desc"));
                data.put("area_id", crimeMap.get("area_id"));
                data.put("vict_age", crimeMap.get("vict_age"));
                data.put("vict_descent", crimeMap.get("vict_descent"));
                
                System.out.println("Row data"+data.toString());
                
                BinomialModelPrediction p = model.predictBinomial(data);
                output.write(p.label);
                output.write(",");
	              	
                for (int i = 0; i < p.classProbabilities.length; i++) {
 	              	if (i > 0) {
 	              		output.write(",");
 	              	}
 	              	output.write(UtilHelper.myDoubleToString(p.classProbabilities[i]));
                }
                output.write("\n");
			}
		}catch(Exception e) {
			e.printStackTrace();
			CrimeController.LOGGER.error("Execption Occured at predictCrimeData"+e.getMessage());
		}
		return crimeList;
	}
	
	/**
	 * This API is to implement BinomialModel Classification prediction.
	 * Here the input is passed as File in POST call. Converting into valid Java Collections using Object Mapper.
	 * Made the ModelClass name as a valid Prediction object using GenModel.
	 * Appended the results in RowData and passed the object into prediction model. The prediction model is based on h2o-genmodel.jar.
	 * 
	 * @param file
	 * @return
	 */
	@RequestMapping(value="/crime",method = { RequestMethod.GET, RequestMethod.POST })
	public String predictCrimeDataFromFile(@RequestParam(value = "file", required = false) CommonsMultipartFile file){
		
		 if (file != null) {
			 try {
				 hex.genmodel.GenModel rawModel;
				 rawModel = (hex.genmodel.GenModel) Class.forName(modelClassName).newInstance();
				 EasyPredictModelWrapper model = new EasyPredictModelWrapper(rawModel);
				 
				 InputStream inputStream = file.getInputStream();
				 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				 String line = "";
				 
				 BufferedWriter output = new BufferedWriter(new FileWriter("OUTPUT_PATH"));
				 output.write("predict");
				 output.write(",");
				 output.write("Arrested");
				 output.write(",");
				 output.write("Not Arrested");
				 output.write("\n");
				 
				 RowData data = null;
				 int iteration = 0;
				 while ((line = bufferedReader.readLine()) != null) {
					if(iteration == 0) {
				        iteration++;  
				        continue;
				    }
					data = new RowData();
	                String[] user = line.split(",");
	                data.put("rpt_dist_no", user[12]);
	                data.put("vict_sex", user[17]);
	                data.put("weapon_desc", user[19]);
	                data.put("crm_cd", user[2]);
	                data.put("premis_desc", user[11]);
	                data.put("area_id", user[0]);
	                data.put("vict_age", user[18]);
	                data.put("vict_descent", user[16]);
	                
	                System.out.println("Row data"+data.toString());
	                
	                BinomialModelPrediction p = model.predictBinomial(data);
	                output.write(p.label);
	                output.write(",");
 	              	
	                for (int i = 0; i < p.classProbabilities.length; i++) {
	 	              	if (i > 0) {
	 	              		output.write(",");
	 	              	}
	 	              	output.write(UtilHelper.myDoubleToString(p.classProbabilities[i]));
	                }
	                output.write("\n");
				 }
				 bufferedReader.close();
			 }catch(Exception e) {
				 e.printStackTrace();
				 CrimeController.LOGGER.info("Exception Occured at CrimeController"+e.getMessage());
			 }
		 }else{
			 System.out.println("File is Empty!!!!!!");
		 }		 
		return "hello";
	}
	
	@RequestMapping(value = "/systemcheck", method = { RequestMethod.GET, RequestMethod.POST })
	public String samleCheck(){
		return "sucess...";
	}
	
	public static void main(String args[]) throws InstantiationException, IllegalAccessException, ClassNotFoundException, PredictException, IOException {
		hex.genmodel.GenModel rawModel;
	    rawModel = (hex.genmodel.GenModel) Class.forName(modelClassName).newInstance();
	    EasyPredictModelWrapper model = new EasyPredictModelWrapper(rawModel);
		//ModelCategory category = model.getModelCategory();
		//System.out.println(category);
	    
	    RowData data = new RowData();
	    data.put("rpt_dist_no", "185");
	    data.put("vict_sex", "H");
	    data.put("weapon_desc", "STRONG-ARM (HANDS: FIST: FEET OR BODILY FORCE)");
	    data.put("crm_cd", "946");
	    data.put("premis_desc", "VEHICLE: PASSENGER/TRUCK");
	    data.put("area_id", "1");
	    data.put("vict_age", "36.101201841");
	    data.put("vict_descent", "X");
	    
	    BufferedWriter output = new BufferedWriter(new FileWriter("OUTPUT_PATH"));
	     
	     BinomialModelPrediction p = model.predictBinomial(data);
	     output.write(p.label);
	     output.write(",");
	     System.out.println(p.classProbabilities.length);
         for (int i = 0; i < p.classProbabilities.length; i++) {
         	System.out.println(UtilHelper.myDoubleToString(p.classProbabilities[i]));
            	if (i > 0) {
            		output.write(",");
            	}
            	output.write(UtilHelper.myDoubleToString(p.classProbabilities[i]));
            	output.write("\n");
         }
	   }
}
