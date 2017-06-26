package iappsqainterview;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class testITunesSearchAPI {
	/*
	 * Testplan:
	 * 
	 * Functional Tests:
	 * 
	 * Section 1:	
	 * TC#      Parameters passed               Expected Results
	 * ==================================================================================================
	 * 1.        term    =   jack                 Response code: 200 (OK)
	 *           limit   ->  unspecified          Verify Json response as specified in Section 2.
	 *           media   ->  unspecified          Every result entry must contain "jack"                  
	 *           country ->  unspecified
	 * 
	 * 2.        term   ->  unspecified          Response code: 200(OK)
	 *                                           Json response should have resultCount = 0
	 *                                           Verify Json response as specified in Section 2
	 * 
	 * 
	 * 3.        term  --> jack                  Response code: 200(OK)
	 *           limit --> 1                     JSON response should have resultCount <= 1
	 *                                            VErify Json response as specified in Section 2
	 *                                          
	 * 4.        term  ---> jack                 Response code: 200(OK)
	 *           media ---> movie                Verify Json response as specified in Section 2.
	 *           
	 *             
	 * 5.        term --> jack                    Response code: 400
	 *           media ---> unknown               JSON response should contain errorMessages
	 *                                            Verify Json response as specified in Section 2.
	 *                                            
	 * 6.        term --> jack                    Response code: 200
	 *           country --> ZW                   Verify Json response as specified in Section 2
	 *           
	 * 7.        term --> jack 				      Response code: 400
	 *           country --> ZZ;				  JSON response should contain errorMessages
	 *                                            Verify Json response as specified in Section 2.
	 *           
	 *                                      
	 * Section 2: 
	 * Verify a  Json response as follows:
	 * 
	 * Json response if status = 200:
	 * 
	 * Must contain following fields:
	 * 		"resultCount"   
	 * 		"results"
	 * 
	 * "resultCount" should be <= 50 if limit is unspecified 
	 * "resultCount" should be <= limit otherwise
	 * length of "results" should be equal to "resultCount"
	 * if "resultCount" > 0 ( i,e has result entries):
	 * 		every "result" entry should contain "term".
	 * 		
	 * 	
	 * Json response if status = 400:
	 *   
	 * Must contain following fields:
	 * errorMessage
	 * queryParameters
	 * 
	 * errorMessage must show country if country is invalid
	 * errorMessage must show media if media is invalid
	 * 	
	 * 									 
	 */
	@Test(dataProvider = "testData")
	public void testSearchAPI(String term,Integer limit,String country,String media,int expResponseCode,String errMsgKey) throws ClientProtocolException, IOException, JSONException {
		List<NameValuePair> parameterkeyvalue = new ArrayList<>();
		
		if (term != null) {
			parameterkeyvalue.add(new BasicNameValuePair("term",term));
		} 
		if (limit != null) {
			parameterkeyvalue.add(new BasicNameValuePair("limit",limit+""));
		} else {
			limit = (term != null)? 50:0;
		}
		if (media != null) {
			parameterkeyvalue.add(new BasicNameValuePair("media",media));
		}
		if (country != null) {
			parameterkeyvalue.add(new BasicNameValuePair("country",country));
		}
		
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpUriRequest getRequest = new HttpGet("https://itunes.apple.com/search?" + URLEncodedUtils.format(parameterkeyvalue, Consts.UTF_8));
		HttpResponse httpResponse = httpClient.execute(getRequest);
		
		//Verify if status is as expected
		Assert.assertEquals(expResponseCode, httpResponse.getStatusLine().getStatusCode());
		
		String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
		if (expResponseCode == 200) {
			//Verify if Json response has resultCount and results
			JSONObject obj = new JSONObject(jsonResponse);
			Assert.assertEquals(obj.has("resultCount"),true);
			Assert.assertEquals(obj.has("results"),true);
			
			//Verify that the resultCount is <= limit
			int resultCount = obj.getInt("resultCount");
			Assert.assertEquals(resultCount <= limit, true);
			
			//Verify that the results list contain as many results as resultCount
			JSONArray resultArr = (JSONArray) obj.get("results");
			Assert.assertEquals(resultCount, resultArr.length());
			
			//Verify that every result has the term specified
			//Otherwise the resultCount will be zero
			for(int i = 0; i < resultArr.length(); i++){
				Assert.assertEquals(resultArr.toString().contains(term),true);
			}
			
			
		}
		
		if (expResponseCode == 400) {
			//Verify if Json response has errorMessage and queryParameters
			JSONObject obj = new JSONObject(jsonResponse);
			Assert.assertEquals(obj.has("errorMessage"),true);
			Assert.assertEquals(obj.has("queryParameters"),true);
			
			//Verify errormessage contains proper field -- country/media
			Assert.assertEquals(obj.getString("errorMessage").contains(errMsgKey), true);
		}
		
		
	}
	
	//null value means unspecified
	@DataProvider(name = "testData")
	public static Object[][] testDataSet () {
		return new Object [][] {
			{"jack",null,null,null,200,null},
			{null,null,null,null,200,null},
			{null,null,"ZW",null,200,null},
			{"jack",5,null,null,200,null},
			{"jack",null,"ZW",null,200,null},
			{"jack",null,"ZZ",null,400,"country"},
			{"jack",null,null,"ebook",200,null},
			{"jack",null,null,"unkown",400,"media"},
			{"jack",200,"US","all",200,null}
		};
	}
}
