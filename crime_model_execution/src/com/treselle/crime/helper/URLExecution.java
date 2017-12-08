/**
 * 
 */
package com.treselle.crime.helper;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treselle.crime.util.StringUtil;

public class URLExecution {
	
	private final static Logger LOGGER = Logger.getLogger(URLExecution.class);
	
	public static final String  BODY_TYPE_REQUEST_METHODS     = "POST,PUT,DELETE";
	
	/**
	 * This method is to make a POST call to the API through URLConnection.
	 * All the necessary inputs are given to the connection object and valid API call is made.
	 * 
	 * @param requestMethod
	 * @param URL
	 * @param jsonBody
	 * @param requestProperty
	 * @return response
	 */
	public List<Map<String, Object>> executeURL(String requestMethod, String URL, String jsonBody, Map<String, Object> requestProperty) {
		List<Map<String, Object>> response = null;
		try{
			URL urlObj = new URL(URL);
			HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
			// add reuqest header
			con.setRequestMethod(requestMethod);
			
			// Executes the POST,PUT,DELETE with post data if available.
			if ((Arrays.asList(BODY_TYPE_REQUEST_METHODS.split(","))).contains(requestMethod) && StringUtil.isValidString(jsonBody)) {
				con.setDoInput(true);
				con.setDoOutput(true);
				if (!requestProperty.isEmpty()) {
					for(String key : requestProperty.keySet()) {
						con.setRequestProperty(key, (String) requestProperty.get(key));
					}
				}
				OutputStream wr = con.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(wr, "UTF-8");
				osw.write(jsonBody.toString());
				osw.flush();
				osw.close();
			}

			int responseCode = con.getResponseCode();
			URLExecution.LOGGER.info("Response Code"+responseCode);
			
			// Getting response from the executed URL.
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine = null;
			StringBuilder responseBuidler = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				responseBuidler.append(inputLine);
			}
			in.close();
			
			ObjectMapper mapper = new ObjectMapper();
			response = mapper.readValue(responseBuidler.toString(), new TypeReference<List<Map<String, Object>>>() {});
		}catch(Exception e) {
			URLExecution.LOGGER.error("Exception Occured at URLExecution"+e.getMessage());
		}
		return response;
	}
	 
}
