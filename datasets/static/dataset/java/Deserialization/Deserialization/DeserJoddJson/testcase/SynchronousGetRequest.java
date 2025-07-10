<filename>SynchronousGetRequest.java<fim_prefix>

package com.nisum.app.client;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.corba.se.spi.orbutil.fsm.Guard.Complement;

import jodd.json.JsonParser;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SynchronousGetRequest {
	private String clientHost =  "http:///jcia2482:8080";
	//private String url = "http:///publicobject.com/helloworld.txt" ;
	private String endpoint = "/api/v1/promotions/promoCodes/";
	private String url ;
	private Map<String,String> responseHeaders = new HashMap<String,String>();
	private String responseBody;

	private String endPointArgs[] = new String [1];
	
	private  Map<String, String> requestParameters = new HashMap<>();
	
	private final OkHttpClient client = new OkHttpClient();
	
	private Map<String,List<Object>> flatResponseMap = new HashMap<String,List<Object>>();
	
	public void run() throws Exception {
		  setURL();
		  Request request = createRequestWithoutRequestParams();

		  Response response = client.newCall(request).execute();
		  if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

		  Headers responseHeaders = response.headers();
		  for (int i = 0; i < responseHeaders.size(); i++) {
			  //System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
			  setResponseHeaders(responseHeaders.name(i),responseHeaders.value(i));
		  }
		  setResponseBody(response.body().string());
		  System.out.println(getResponseBody());
		  //writeResponseToFile(getResponseBody());
		  convertJSonResponseToFlatMap();
	  }

	private Request createRequestWithoutRequestParams(){
		 Request request = new Request.Builder()
			        .url(url)
			        .build();
		 return request;
	}
	
	private Request createRequestWithtRequestParams(){
		 Request request = new Request.Builder()
			        .url(url)
			        .post(getRequestParams())
			        .build();
		 return request;
	}

	private RequestBody getRequestParams() {
		FormBody.Builder builder = new FormBody.Builder();
		  // Add Params to Builder
		  for ( Map.Entry<String, String> entry : requestParameters.entrySet() ) {
		      builder.add( entry.getKey(), entry.getValue() );
		  }
		  
		  RequestBody formBody = builder.build();
		return formBody;
	}
	  
	  
		public String getURL() {
			return url;
		}

		public void setURL() {
			endPointArgs[0] = "GREAT";
			url = clientHost+endpoint+endPointArgs[0];
		}

		public String getRequestParameters(String key) {
			return requestParameters.get(key);
		}

		public void setRequestParameters(String key, String value) {
			requestParameters.put(key, value);
		}

		public Map<String, String> getResponseHeaders() {
			return responseHeaders;
		}

		public void setResponseHeaders(String headerName, String value) {
			responseHeaders.put(headerName, value);
		}	
		
		public String getResponseBody() {
			return responseBody;
		}

		public void setResponseBody(String responseBody) {
			this.responseBody = responseBody;
		}
		
		public static void main(String [] args){
			SynchronousGetRequest getRequest = new SynchronousGetRequest();
			try {
				//getRequest.run();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			getRequest.convertJSonResponseToFlatMap();
		}
		
		public void writeResponseToFile(String responseBody){
			
		}
		
		/*public void parseJSONresponse(){
			JsonParser jsonParser = new JsonParser();
		    //Map map = jsonParser.parse(
		    //    "{ \"one\" : { \"two\" : 285 }, \"three\" : true}");
			Map map = jsonParser.parse(getResponseBody());
		   Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
		   while(iterator.hasNext()){
			   Map.Entry element = iterator.next();
			   System.out.println("key :"+ element.getKey());
			   if(!isElementMapOrList(element)){
				   System.out.println("value : "+ element.getValue());
			   }
			   
			  }
		   
		   
		   
		}*/

		/*private boolean isElementMapOrList(Map.Entry element) {
			if(element.getValue() instanceof Map){
				   printMap((Map)element.getValue());
			   }else if(element.getValue() instanceof List){
				   printList(element);
				   //System.out.println(element.getValue());
			   }else {
				   return false;
			   }
			return true;
		}*/

		/*private void printList(Map.Entry element) {
			List elementList = (List)element.getValue();
			   Iterator listIterator = elementList.iterator();
			   while(listIterator.hasNext()){
				   printMap((Map)listIterator.next()); 
			   }
		}*/
		
		
		/*private void printMap(Map element){
			Set elements = element.keySet();
			Iterator iterator = elements.iterator();
			while(iterator.hasNext()){
				String keyElement = (String)iterator.next(); 
				System.out.println("key : "+keyElement);
				if(element.get(keyElement) instanceof Map){
					printMap((Map)element.get(keyElement));
				}else 
					System.out.println("value :"+element.get(keyElement));
				
			}
		}*/
		
		public void convertJSonResponseToFlatMap(){
			Map flatResultMap = new HashMap();
			JsonParser jsonParser = new JsonParser();
		    //Map map = jsonParser.parse(
		    //    "{ \"one\" : { \"two\" : 285 }, \"three\" : true}");
            <fim_suffix>
		   Iterator<Map.Entry> iterator = map.entrySet().iterator();
		   //int deapth = 0;
		   while(iterator.hasNext()){
			   Map.Entry keyValue =  iterator.next();
			   String key = (String)keyValue.getKey();
			   //System.out.print(key+".");
			   Object value = ((Map.Entry)keyValue).getValue();	
			   if(!valueIsPrimitive(value)){
				   getKeyValue(value,key+".");
			   }else{
				   System.out.println("---------------------------");
			   }
		   }
		   Iterator iteratorFlatMap = flatResponseMap.keySet().iterator();
		   while(iteratorFlatMap.hasNext()){
			   String keyFlatMap = (String)iteratorFlatMap.next();
			   List l = flatResponseMap.get(keyFlatMap);
			   System.out.println(keyFlatMap+" : "+flatResponseMap.get(keyFlatMap));
		   }
		   
		   return;
		}

		private void getKeyValue(Object value,String keyElement) {
			//System.out.println(keyElement);
			if(value instanceof ArrayList){
				String flatMapKey = keyElement;
				for(int i = 0;i<((ArrayList)value).size();i++){
					Object completeElement = ((ArrayList) value).get(i);
					keyElement = flatMapKey+"["+i+"]"+".";
					 if(!valueIsPrimitive(completeElement)){
						 //getKeyValue(completeElement, ++deapth,keyElement);
						 getKeyValue(completeElement,keyElement);
					 } else {
						 populateFlatMap(keyElement, completeElement);
						 //System.out.println( keyElement+" =  "+completeElement);
					 }
				}
				if(((ArrayList)value).size()==0){
					 populateFlatMap(keyElement, new ArrayList());
				}
			} 
			
			if(value instanceof HashMap){
				   String flatMapKey = keyElement;
				   Iterator iterator = ((HashMap)value).keySet().iterator();
				   while(iterator.hasNext()){
					   String key = (String)iterator.next();
					   //System.out.print(key  + ".");
					   keyElement = flatMapKey+key+".";
					   Object mapValue = ((HashMap)value).get(key);
					   if(!valueIsPrimitive(mapValue)){
						   //getKeyValue(mapValue, ++deapth,keyElement);
						   getKeyValue(mapValue,keyElement);
						   keyElement = keyElement.substring(0,keyElement.length()-key.length()-1);
					   }else {
						   	populateFlatMap(keyElement, mapValue);
						   
					   }
				   }
			   }
			return;
			
		}

		private void populateFlatMap(String keyElement, Object completeElement) {
			List elementDescList = new ArrayList();
			 elementDescList.add(completeElement);
			 elementDescList.add(completeElement.getClass());
			 flatResponseMap.put(keyElement.substring(0,keyElement.length()-1), elementDescList);
		}
		

		private boolean valueIsPrimitive(Object value) {
			return value instanceof Integer || value instanceof Float || value instanceof Double 
					|| value instanceof Boolean || value instanceof String ;
		}
		
		public Map<String, List<Object>> getFlatResponseMap() {
			return flatResponseMap;
		}

		public void setFlatResponseMap(Map<String, List<Object>> flatResponseMap) {
			this.flatResponseMap = flatResponseMap;
		}
		
		String responseJson = "{	\"OffersResponse\":	{		\"promotionOffers\":		[			{				\"promoCode\":\"GREAT\",				\"promotions\":				[					{						\"id\":19859670,						\"promotionName\":\"TEST $ off Promotion W/ PROMO CODE\",\"description\":\"TEST $ off No Code Promotion Test: $10 off WITH PROMO CODE\",						\"global\":false,						\"promotionType\":\"Dollar Off Order\",						\"effectiveDate\":\"03/09/2015 12:00:00 AM \",						\"expirationDate\":\"03/16/2037 11:59:59 PM \",						\"promotionCode\":\"GREAT\",						\"promotionSources\":[\"MACYS\",\"BLOOMIES\"],						\"promotionCodeIds\":[],						\"promotionAttribute\":[],						\"triggers\":						[													{									\"triggerId\":29157072,								\"triggerType\":\"\",								\"hasParent\":false,								\"parentTriggerId\":0,								\"promotionId\":19859670,								\"tier\":1							}						],						\"operations\":						[								{									\"description\":\"TEST $ off No Code Promotion Test: $10 off WITH PROMO CODE\",									\"operationId\":\"58267409\",									\"promotionId\":19859670,									\"discountValue\":1.0,									\"threshold\":0,									\"operator\":\"AMOUNT\",									\"target\":\"SUBTOTAL\",									\"type\":\"DISCOUNT\",									\"tier\":1,									\"sequenceNumber\":1,									\"displayGift\":false,									\"giftId\":0,									\"quantity\":0								}													],						\"validWithOtherPromotions\":true,						\"tierList\":[],						\"incompatiblePromotionIds\":[],						\"sequenceNumber\":500,						\"maxTimesToApply\":1,						\"scope\":\"ORDER\",						\"gwp\":false,						\"promoCodes\":[\"GREAT\"]					}				]			}		]	}}";
		
	
}
<fim_middle>