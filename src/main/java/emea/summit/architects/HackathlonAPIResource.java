/**
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package emea.summit.architects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.dmr.ModelNode;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.http.DefaultSpanNameProvider;
import com.github.kristofa.brave.httpclient.BraveHttpRequestInterceptor;
import com.github.kristofa.brave.httpclient.BraveHttpResponseInterceptor;
import com.openshift.internal.restclient.model.Route;
import com.openshift.internal.restclient.model.properties.ResourcePropertiesRegistry;
import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;

import feign.Logger;
import feign.Logger.Level;
import feign.httpclient.ApacheHttpClient;
import feign.hystrix.HystrixFeign;
import feign.jackson.JacksonDecoder;
import io.swagger.annotations.ApiOperation;
import io.undertow.client.ClientRequest;

/**
 * 
 * @author stelios@redhat.com
 *
 */
@Path("/")
public class HackathlonAPIResource {

	
	
	private static final String API_PAYLOAD = "[  \n"+ 
			"   {  \n"+ 
			"      \"teamName\":\"\",\n"+ 
			"      \"reindeerName\":\"\",\n"+ 
			"      \"nameEmaiMap\":{  \n"+ 
			"         \" \":\" \"\n"+ 
			"      }\n"+ 
			"   }\n"+ 
			"]\n";


	private static final String API_PAYLOAD_EXAMPLE = 
			"[  \n"+
					"   {  \n"+
					"      \"teamName\":\"santas-helpers-c-team\",\n"+
					"      \"reindeerName\":\"blixen\",\n"+
					"      \"nameEmaiMap\":{  \n"+
					"         \"Andrea Tarrochi\":\"atarocch@redhat.com\",\n"+
					"         \"Stelios Kousouris\":\"stelios@redhat.com\"\n"+
					"      }\n"+
					"   },\n"+
					"   {  \n"+
					"      \"teamName\":\"santas-helpers-a-team\",\n"+
					"      \"reindeerName\":\"dancer\",\n"+
					"      \"nameEmaiMap\":{  \n"+
					"         \"Matteo Renzi\":\"mrenzi@redhat.com\",\n"+
					"         \"Antonis Tsipras\":\"atsipras@redhat.com\"\n"+
					"      }\n"+
					"   }\n"+
					"]\n";

	private static final int ZERO = 0;
	private static final String VALID_RESPONSE = "The service is valid and Reindeers in order";
	private static final String INVALID_RESPONSE = "The service is invalid and Reindeers are out of order \n ";

	private static LinkedList<String> serviceRoutes = new LinkedList<String>(Arrays.asList("http://santas-helpers-a-team.router.default.svc.cluster.local",
			"http://santas-helpers-b-team.router.default.svc.cluster.local",
			"http://santas-helpers-c-team.router.default.svc.cluster.local",
			"http://santas-helpers-d-team.router.default.svc.cluster.local",
			"http://santas-helpers-e-team.router.default.svc.cluster.local",
			"http://swarm-email-santas-list.router.default.svc.cluster.local"));
	
	private static Map<String, String> namespacesServicesMap = new HashMap<String, String>(){{
//		put("santas-helpers-a-team", "bushy-evergreen");
//		put("santas-helpers-b-team", "shinny-upatree");
//		put("santas-helpers-c-team", "wunorse-openslae");
//		put("santas-helpers-d-team", "pepper-minstix");
//		put("santas-helpers-e-team", "alabaster-snowball");
		put("santas-helpers-a-team", "test-milan");
		put("santas-helpers-b-team", "test-milan");
		put("santas-helpers-c-team", "test-milan");
		put("santas-helpers-d-team", "test-milan");
		put("santas-helpers-e-team", "test-milan");
	}};
	
	// This is mapping of the current service providing the payload and the following to call by discovering it from OCP ENV Variables
	private static Map<String, String> serviceENVVariableMap = new HashMap<String, String>(){{
		put("proxy-api", "PROXY_API");
//		put("bushy-evergreen", "BUSHY_EVERGREEN");
//		put("shinny-upatree", "SHINY_UPATREE");
//		put("wunorse-openslae", "WUNORSE_OPENSLAE");
//		put("pepper-minstix", "PEPPER_MINSTIX");
//		put("alabaster-snowball", "ALABASTER_SNOWBALL");
		put("NONE", "BUSHY_EVERGREEN");
		put("bushy-evergreen", "SHINNY_UPATREE");
		put("shinny-upatree", "WUNORSE_OPENSLAE");
		put("wunorse-openslae", "PEPPER_MINSTIX");
		put("pepper-minstix", "ALABASTER_SNOWBALL");
		put("alabaster-snowball", "PROXY_API");
	}};
	
	private static Map<String, String> servicesRouteMap = new HashMap<String, String>(){{
		put("bushy-evergreen", "http://bushy-evergreen-santas-helpers-a-team.router.default.svc.cluster.local");
		put("shinny-upatree", "http://shinny-upatree-santas-helpers-b-team.router.default.svc.cluster.local");
		put("wunorse-openslae", "http://santas-helpers-c-team.router.default.svc.cluster.local");
		put("pepper-minstix", "http://santas-helpers-e-team.router.default.svc.cluster.local");
		put("alabaster-snowball", "http://santas-helpers-e-team.router.default.svc.cluster.local");
	}};
	

	@Inject
	private Brave brave;

	@Context
	private SecurityContext securityContext;

	@Context
	private HttpServletRequest servletRequest;

	private IClient createOCPClient() {
		//IClient client = new ClientBuilder("https://api.preview.openshift.com")
		IClient client = new ClientBuilder("https://35.156.133.70:8443")
				.usingToken("RRhGF3JrhkwtmUoj51WwV4pnBLGzxpq1n2X1grqK4bg")
//			    .withUserName("admin")
//			    .withPassword("admin123")
			    .build();
		//client.getAuthorizationContext().setToken("RRhGF3JrhkwtmUoj51WwV4pnBLGzxpq1n2X1grqK4bg");
	
		return client;
	}
	
	private String namespaceFromService(String serviceName) {
		
		for(Map.Entry<String,String> entry: namespacesServicesMap.entrySet()) {
			System.out.println(entry.getKey() + ": " + entry.getValue());
			if (serviceName.equalsIgnoreCase(entry.getValue())); {
				System.out.println("Namespace "+entry.getValue()+" found for Service Name "+serviceName);
				return entry.getValue();
			}
		}
//		// In Java 8 you can write
//		namespacesServicesMap.forEach((k, v) -> System.out.println(k + ": " + v));
		return null;
	}
		    
	private List<String> emailsOfTeam(TeamPayload request) {
		
		String teamName = namespaceFromService(request.getServiceName());
		
		for(RequestPayload entry : request.getPayload()) {
			System.out.println("if "+entry.getTeamName()+" EQUALS"+ teamName+" --> "+entry.getTeamName().equalsIgnoreCase(teamName));
			if (entry.getTeamName().equalsIgnoreCase(teamName)) {
				System.out.println("Team Emails"+entry.getNameEmaiMap().values());
				return (List<String> ) entry.getNameEmaiMap().values();
			}
		}
		return null;
	}


	@POST  
	@Path("/service/proxy")
	@Consumes("application/json")
	@ApiOperation("Receives request, validates request so far, identifies next service to contact, contacts the service OR if no more sends the email to SANTA")
	public String submit(TeamPayload request) {
		boolean PROD_ENV = System.getenv("ENVIRONMENT") != null &&  System.getenv("ENVIRONMENT").equalsIgnoreCase("PROD")? true : false;
		
//		IClient ocpClient = createOCPClient();
		
//		System.out.println("<------------------ ROUTE DETAILS ------------------>");
//		System.out.println(ocpClient.get(ResourceKind.ROUTE, namespaceFromService(request.getServiceName())));
//		System.out.println("<--------------------------------------------------->");
//		ModelNode node = ModelNode.fromJSONString(Samples.V1_ROUTE_WO_TLS.getContentAsString());
//        Route route = new Route(node, ocpClient, ResourcePropertiesRegistry.getInstance().get("v1", ResourceKind.ROUTE));
//		ocpClient.getResourceURI(arg0)
		
		System.out.println("==================REQUESTING SERVICE: "+request.getServiceName()+"=======================");
		System.out.println("PAYLOAD");
		System.out.println(request.getPayload().toString());
		
		String host = System.getenv(serviceENVVariableMap.get(request.getServiceName())+"_SERVICE_HOST");
		String port = System.getenv(serviceENVVariableMap.get(request.getServiceName())+"_SERVICE_PORT");
		
		String Ahost = System.getenv(serviceENVVariableMap.get("NONE")+"_SERVICE_HOST");
		String Aport = System.getenv(serviceENVVariableMap.get("NONE")+"_SERVICE_PORT");
		
		String Bhost = System.getenv(serviceENVVariableMap.get("bushy-evergreen")+"_SERVICE_HOST");
		String Bport = System.getenv(serviceENVVariableMap.get("bushy-evergreen")+"_SERVICE_PORT");
		
		String Chost = System.getenv(serviceENVVariableMap.get("shinny-upatree")+"_SERVICE_HOST");
		String Cport = System.getenv(serviceENVVariableMap.get("shinny-upatree")+"_SERVICE_PORT");
		
		String Dhost = System.getenv(serviceENVVariableMap.get("wunorse-openslae")+"_SERVICE_HOST");
		String Dport = System.getenv(serviceENVVariableMap.get("wunorse-openslae")+"_SERVICE_PORT");
		
		String Ehost = System.getenv(serviceENVVariableMap.get("pepper-minstix")+"_SERVICE_HOST");
		String Eport = System.getenv(serviceENVVariableMap.get("pepper-minstix")+"_SERVICE_PORT");	
		
		String Fhost = System.getenv(serviceENVVariableMap.get("alabaster-snowball")+"_SERVICE_HOST");
		String Fport = System.getenv(serviceENVVariableMap.get("alabaster-snowball")+"_SERVICE_PORT");
		
		System.out.println("Would call [proxy-api] \n POST   http://"+host+":"+port);
		
		System.out.println("Would call [bushy-evergreen] \n POST   http://"+Ahost+":"+Aport);
		httpCall("POST", "http://"+Ahost+":"+Aport+"/api/test", request.toString());
		
		System.out.println("Would call [shinny-upatree] \n POST   http://"+Bhost+":"+Bport);
		httpCall("POST", "http://"+Bhost+":"+Bport+"/api/test", request.toString());	
		
		System.out.println("Would call [wunorse-openslae] \n POST   http://"+Chost+":"+Cport);
		httpCall("POST", "http://"+Chost+":"+Cport+"/api/test", request.toString());
		
		System.out.println("Would call [pepper-minstix] \n POST   http://"+Dhost+":"+Dport);
		httpCall("POST", "http://"+Dhost+":"+Dport+"/api/test", request.toString());

		System.out.println("Would call [alabaster-snowball] \n POST   http://"+Ehost+":"+Eport);
		httpCall("POST", "http://"+Ehost+":"+Eport+"/api/test", request.toString());
		
		System.out.println("Would call [/service/email-santa] \n POST   http://"+Ehost+":"+Eport);
		EmailPayload email = new EmailPayload(null, "SUCCESS", null);
		httpCall("POST", "http://"+Ehost+":"+Eport+"/api/service/email-santa", email.toString());

		
		if (PROD_ENV) {
			if (validate(request.getPayload()).equalsIgnoreCase(VALID_RESPONSE)){
				System.out.println("Valid...sending to next service");
				// TODO 
				// find the next service and send OR send email to SANTA
				
//				"oc describe route "+request.getServiceName()
//				"oc describe route bushy-evergreen"
//				"oc describe route shinny-upatree"
//				"oc describe route wunorse-openslae"
//				"oc describe route pepper-minstix"
//				"oc describe route alabaster-snowball"
				
//				String host = System.getenv(serviceENVVariableMap.get(request.getServiceName())+"_SERVICE_HOST");
//				String port = System.getenv(serviceENVVariableMap.get(request.getServiceName())+"_SERVICE_PORT");
				
				System.out.println("ABOUT To call\n POST   https://"+host+":"+port);
				System.out.println(request.toString());
				//httpCall("POST", "https://"+host+":"+port, request.toString());
				
			} else {
				// Send a failed response to the requestors and an email.
				System.out.println("INVALID_RESPONSE");
				System.out.println("Sent to team "+namespaceFromService(request.getServiceName())+" emailing "+emailsOfTeam(request));
				

				
				try {
					JavaMailService.generateAndSendEmail(INVALID_RESPONSE+"\n\n"+request.getPayload(), "HACKATHLON Santa Helper "+request.getServiceName()+" sent INVALID Request ", emailsOfTeam(request));
				} catch (MessagingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "Email Failed due to "+e.getMessage();
				}
			}
		} else {
			String hostReal = System.getenv(serviceENVVariableMap.get(request.getServiceName())+"_SERVICE_HOST");
			String portReal = System.getenv(serviceENVVariableMap.get(request.getServiceName())+"_SERVICE_PORT");
			//System.out.println("Would call [/service/email-santa] \n POST   http://"+Ehost+":"+Eport);
			//if (namespaceFromService(request.getServiceName()).equalsIgnoreCase("santas-helpers-e-team")) {
			
			System.out.println("Validation Statement: "+validate(request.getPayload()).equalsIgnoreCase(VALID_RESPONSE));
			
			if (namespaceFromService(request.getServiceName()).equalsIgnoreCase("test-milans")) {
				System.out.println("Next Service we would have called if NOT in DEV Mode would have been [/service/email-santa] \n POST   http://"+hostReal+":"+portReal+"/service/email-santa");
			} else {
				System.out.println("Next Service we would have called if NOT in DEV Mode would have been [/] http://"+hostReal+":"+portReal);
			}
			
			
		}
		
		
//		try {
//		JavaMailService.generateAndSendEmail(email.getContent().toString(), email.getSubject(), email.getEmailAddresses());
//	} catch (MessagingException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//		return "Email Failed due to "+e.getMessage();
//	}
		System.out.println("Request from ["+request.getServiceName()+"] submitted successfully to " +serviceENVVariableMap.get(request.getServiceName())+"]");
		System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");

		
		return "Request from ["+request.getServiceName()+"] submitted successfully to [" +serviceENVVariableMap.get(request.getServiceName()+"]");
	}
	
	private String httpCall(String httpMethod, String serviceURL, String data){
		String result = "Not valid request for \n"+
				"HTTP METHOD" + httpMethod +"\n"+
				"URL" +serviceURL + "\n"+
				"Content" + data;

		System.out.println("<===================== Calling External Service  ======================>");
		System.out.println("     HTTP METHOD : "+httpMethod);
		System.out.println("     URL         : "+serviceURL);
		System.out.println("     Content     : "+data);
		System.out.println("<======================================================================>");

		if (httpMethod != null || httpMethod.equals("GET") || httpMethod.equals("POST") || httpMethod.equals("PUT")) {

			try {
				HttpClientBuilder builder = HttpClientBuilder.create();
				CloseableHttpClient client = builder.build();

				//HttpUriRequest request = new HttpGet(serviceURL+"api/hackathlon/info");
				HttpUriRequest request;
				if (httpMethod.equalsIgnoreCase("GET")){
					result = getRequest(serviceURL, "application/json");
				} else if (httpMethod.equalsIgnoreCase("POST")) {
					// eg. http://www.programcreek.com/java-api-examples/org.apache.http.entity.StringEntity
					StringEntity content = new StringEntity(data,"UTF-8");
					result = postRequest(serviceURL, "application/json", content);
				} else {
					result = putRequest(serviceURL, "application/json");
				}

				System.out.println("<=================== RESPONSE ====================> ");
				System.out.println("    "+result); 
				System.out.println("<=======================================> ");

			} catch (Exception e) {
				System.out.println("****************************************************************");
				//System.out.println("FAILED - CALLING ANOTHER SERVICE FROM "+serviceURL+"api/hackathlon/info");
				System.out.println("FAILED - CALLING ANOTHER SERVICE FROM "+serviceURL);
				System.out.println(e.getMessage());
				System.out.println("****************************************************************");
				return result;
			}
			System.out.println("****************************************************************");
			//System.out.println("SUCCESS - CALLING ANOTHER SERVICE FROM "+serviceURL+"api/hackathlon/info");
			System.out.println("SUCCESS - CALLING ANOTHER SERVICE FROM "+serviceURL);
			System.out.println("****************************************************************");
			return result;

		}
		System.out.println("****************************************************************");
		//System.out.println("FAILED - CALLING ANOTHER SERVICE FROM "+serviceURL+"api/hackathlon/info");
		System.out.println("FAILED - CALLING ANOTHER SERVICE FROM "+serviceURL);
		System.out.println("****************************************************************");
		return result;
	}
	
	
	@POST  
	@Path("/service/email-santa")
	@Consumes("application/json")
	@ApiOperation("Sends the email to a list of participants, with subject and payload")
	public String sendEmailNotification(EmailPayload email) {

		try {
			JavaMailService.generateAndSendEmail(email.getContent().toString(), email.getSubject(), email.getEmailAddresses());
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "Email Failed due to "+e.getMessage();
		}
		return "Email was submitted successfully";
	}

	@POST  
	@Path("/service/validate")
	@Consumes("application/json")
	@ApiOperation("Sends the expected payload of your service and it will validate construct and ordering")
	public String validate(List<RequestPayload> request) {

		System.out.println("Request Object ---->" +request.toString());

		boolean ordered = inOrder(request.iterator(), null);

		if (!ordered) {
			return INVALID_RESPONSE+request.toString();

		}

		return VALID_RESPONSE;
	}

	@GET
	@Path("/info")
	@Produces("application/json")
	@ApiOperation("Returns the greeting in Spanish")
	public String info() {

		String info = "\n\n================================================"
				+"\n     EMEA ARCHITECTS HATCKATHLON INFORMATION "
				+"\n================================================"
				+"\n\nAPI PAYLOAD"
				+"\n-----------------------"
				+"\n"+API_PAYLOAD
				+"\n\nEXAMPLE : \n"
				+"\n"+API_PAYLOAD_EXAMPLE
				+"\n===========================================";

		System.out.println(info);
		return info;
	}

	@POST
	@Path("/next-service")
	@Consumes("application/json")
	@Produces("application/json")
	@ApiOperation("Returns the URL of the next MSA in the teams of Santa Helpers to be used to communicate with")
	public String nextService(String yourServiceName) {

		System.out.println("Current Service ("+yourServiceName+")");

		// TODO - Read via PARAM so that each team will have to go and declare their own property
		if (yourServiceName != null) {
			System.out.println("Current Service ("+yourServiceName+") NOT NULL ");
			return getNextServiceRouteURL(yourServiceName);
			//    		if (yourServiceName.equalsIgnoreCase("santas-helpers-a-team")) {
			//    			return "http://santas-helpers-b-team.router.default.svc.cluster.local";
			//    		}else if (yourServiceName.equalsIgnoreCase("santas-helpers-b-team")) {
			//    			return "http://santas-helpers-c-team.router.default.svc.cluster.local";
			//    		} else if (yourServiceName.equalsIgnoreCase("santas-helpers-c-team")) {
			//    			return "http://santas-helpers-d-team.router.default.svc.cluster.local";
			//    		} else if (yourServiceName.equalsIgnoreCase("santas-helpers-d-team")) {
			//    			return "http://santas-helpers-e-team.router.default.svc.cluster.local";
			//    		} else if (yourServiceName.equalsIgnoreCase("santas-helpers-e-team")) {
			//    			return "http://swarm-email-santas-list.router.default.svc.cluster.local";
			//
			//    		}
		}
		return "ERROR: No matching next service for the provided Santa Team";
	}

	//    @POST
	//    @Path("/service/register")
	//    @Consumes("application/json")
	//    @Produces("application/json")
	//    @ApiOperation("Registers the URL of the servce against the TEAM name, rejects if team name is not in the pre-defined list")
	//    public String registerService(String teamName, String yourServiceEndpointURL) {
	//    	
	//    	System.out.println("Team : Service URL Registration ("+teamName+":"+yourServiceEndpointURL+")\n\n");
	//    	
	//    	System.out.println(servicesURLMap.toString());
	//    	
	//		return servicesURLMap.toString();
	//    }
	//    
	//    @POST  
//	@PUT
//	@Path("/email-santa/{emailContent}")
//	@Consumes("application/json")
//	@ApiOperation("Sends the email to Santa with the list")
//	public String sendEmailNotification(@PathParam(value = "emailContent") String emailContent) {
//
//		// TODO - Read via PARAM so that each team will have to go and declare their own property
//		try {
//			JavaMailService.generateAndSendEmail(emailContent);
//		} catch (MessagingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return "Email Failed due to "+e.getMessage();
//		}
//		return "Email was submitted successfully";
//	}

	
	@POST
	@Path("/other-service")
	@Consumes("application/json")
	@Produces("application/json")
	@ApiOperation("Call service to service")
	public String callOtherService(String jsonRequest) {

		ObjectMapper mapper = new ObjectMapper(); //Jackson's JSON marshaller
		PostServiceBean requestContent = null;
		try {
			requestContent = mapper.readValue(jsonRequest, PostServiceBean.class );
		} catch (IOException e) {
			System.out.println("Request was invalid cause: "+e.getMessage());
			return "Request was invalid cause: "+e.getMessage();
		}

		String httpMethod = requestContent.getHttpMethod();
		String serviceURL =  requestContent.getUrl();
		String data = requestContent.getContent();

		String result = "Not valid request for \n"+
				"HTTP METHOD" + httpMethod +"\n"+
				"URL" +serviceURL + "\n"+
				"Content" + data;

		System.out.println("<===================== Calling External Service  ======================>");
		System.out.println("     HTTP METHOD : "+httpMethod);
		System.out.println("     URL         : "+serviceURL);
		System.out.println("     Content     : "+data);
		System.out.println("<======================================================================>");

		if (httpMethod != null || httpMethod.equals("GET") || httpMethod.equals("POST") || httpMethod.equals("PUT")) {

			try {
				HttpClientBuilder builder = HttpClientBuilder.create();
				CloseableHttpClient client = builder.build();

				//HttpUriRequest request = new HttpGet(serviceURL+"api/hackathlon/info");
				HttpUriRequest request;
				if (httpMethod.equalsIgnoreCase("GET")){
					result = getRequest(serviceURL, "application/json");
				} else if (httpMethod.equalsIgnoreCase("POST")) {
					// eg. http://www.programcreek.com/java-api-examples/org.apache.http.entity.StringEntity
					StringEntity content = new StringEntity(data,"UTF-8");
					result = postRequest(serviceURL, "application/json", content);
				} else {
					result = putRequest(serviceURL, "application/json");
				}

				System.out.println("<=================== RESPONSE ====================> ");
				System.out.println("    "+result); 
				System.out.println("<=======================================> ");

			} catch (Exception e) {
				System.out.println("****************************************************************");
				//System.out.println("FAILED - CALLING ANOTHER SERVICE FROM "+serviceURL+"api/hackathlon/info");
				System.out.println("FAILED - CALLING ANOTHER SERVICE FROM "+serviceURL);
				System.out.println(e.getMessage());
				System.out.println("****************************************************************");
				return result;
			}
			System.out.println("****************************************************************");
			//System.out.println("SUCCESS - CALLING ANOTHER SERVICE FROM "+serviceURL+"api/hackathlon/info");
			System.out.println("SUCCESS - CALLING ANOTHER SERVICE FROM "+serviceURL);
			System.out.println("****************************************************************");
			return result;

		}
		System.out.println("****************************************************************");
		//System.out.println("FAILED - CALLING ANOTHER SERVICE FROM "+serviceURL+"api/hackathlon/info");
		System.out.println("FAILED - CALLING ANOTHER SERVICE FROM "+serviceURL);
		System.out.println("****************************************************************");
		return result;
	}


	private boolean inOrder(Iterator<RequestPayload> reindeersIt, String reindeer) {
		String nextReindeer = null;
		if (reindeersIt == null || !reindeersIt.hasNext()){
			return true;
		}
		if (reindeersIt.hasNext()){
			nextReindeer = reindeersIt.next().getReindeerName();
			System.out.println(" Compare "+reindeer+" vs "+nextReindeer);
			if (reindeer != null && reindeer.compareToIgnoreCase(nextReindeer) > ZERO) {
				return false;
			}
			return inOrder(reindeersIt, nextReindeer);
		}
		return true;
	}

	private String getNextServiceRouteURL(String currentService) {
		Iterator<String> routeIterator = serviceRoutes.iterator();
		while (routeIterator.hasNext()) {
			String svcURL = routeIterator.next();
			if(currentService.equalsIgnoreCase(svcURL)) {
				System.out.println("Match Found on SVC --> "+currentService);
				svcURL = routeIterator.next();
				System.out.println("RETURNED Next Service --> "+svcURL);
				return svcURL;
			}
			System.out.println("Next Service --> "+svcURL);
		}
		return null;
	}


	private static String postRequest(String url, String contentType, HttpEntity entity) throws Exception {
		HttpClientBuilder builder = HttpClientBuilder.create();
		CloseableHttpClient client = builder.build();
		HttpPost post = new HttpPost(url);
		post.setHeader("Content-Type", contentType);
		post.setEntity(entity);

		HttpResponse response = client.execute(post);
		StatusLine status = response.getStatusLine();			
		String content = EntityUtils.toString(response.getEntity());
		//JSONObject json = new JSONObject(content);

		return content;
	}

	private static String getRequest(String url, String contentType) throws Exception {
		HttpClientBuilder builder = HttpClientBuilder.create();
		CloseableHttpClient client = builder.build();
		HttpUriRequest get = new HttpGet(url);
		get.setHeader("Content-Type", contentType);

		HttpResponse response = client.execute(get);
		StatusLine status = response.getStatusLine();			
		String content = EntityUtils.toString(response.getEntity());
		//JSONObject json = new JSONObject(content);

		return content;
	}

	private static String putRequest(String url, String contentType) throws Exception {
		HttpClientBuilder builder = HttpClientBuilder.create();
		CloseableHttpClient client = builder.build();
		HttpUriRequest put = new HttpPut(url);
		put.setHeader("Content-Type", contentType);

		HttpResponse response = client.execute(put);
		StatusLine status = response.getStatusLine();			
		String content = EntityUtils.toString(response.getEntity());
		//JSONObject json = new JSONObject(content);

		return content;
	}

	//    @GET
	//    @Path("/hackathlon/chaining/{serviceName}")
	//    @Produces("application/json")
	//    @ApiOperation("Returns the greeting plus the next service in the chain")
	//    public List<String> hackathlonServiceChaining(@PathParam(value = "serviceName") String yourServiceName) {
	//        List<String> greetings = new ArrayList<>();
	//        greetings.add(hola());
	//        greetings.addAll(getNextService().aloha());
	//        return greetings;
	//    }
	//    
	//    @GET
	//    @Path("/hola-chaining")
	//    @Produces("application/json")
	//    @ApiOperation("Returns the greeting plus the next service in the chain")
	//    public List<String> holaChaining() {
	//        List<String> greetings = new ArrayList<>();
	//        greetings.add(hola());
	//        greetings.addAll(getNextService().aloha());
	//        return greetings;
	//    }

	//    @GET
	//    @Path("/hola-secured")
	//    @Produces("text/plain")
	//    @ApiOperation("Returns a message that is only available for authenticated users")
	//    public String holaSecured() {
	//        // this will set the user id as userName
	//        String userName = securityContext.getUserPrincipal().getName();
	//
	//        if (securityContext.getUserPrincipal() instanceof KeycloakPrincipal) {
	//            @SuppressWarnings("unchecked")
	//            KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) securityContext.getUserPrincipal();
	//
	//            // this is how to get the real userName (or rather the login name)
	//            userName = kp.getKeycloakSecurityContext().getToken().getName();
	//        }
	//        return "This is a Secured resource. You are loged as " + userName;
	//
	//    }

	//    @GET
	//    @Path("/logout")
	//    @Produces("text/plain")
	//    @ApiOperation("Logout")
	//    public String logout() throws ServletException {
	//        servletRequest.logout();
	//        return "Logged out";
	//    }

	/**
	 * This is were the "magic" happens: it creates a Feign, which is a proxy interface for remote calling a REST endpoint with
	 * Hystrix fallback support.
	 *
	 * @return The feign pointing to the service URL and with Hystrix fallback.
	 */
	private AlohaService getNextService() {
		final String serviceName = "aloha";
		// This stores the Original/Parent ServerSpan from ZiPkin.
		final ServerSpan serverSpan = brave.serverSpanThreadBinder().getCurrentServerSpan();
		final CloseableHttpClient httpclient =
				HttpClients.custom()
				.addInterceptorFirst(new BraveHttpRequestInterceptor(brave.clientRequestInterceptor(), new DefaultSpanNameProvider()))
				.addInterceptorFirst(new BraveHttpResponseInterceptor(brave.clientResponseInterceptor()))
				.build();
		String url = String.format("http://%s:8080/", serviceName);
		return HystrixFeign.builder()
				// Use apache HttpClient which contains the ZipKin Interceptors
				.client(new ApacheHttpClient(httpclient))
				// Bind Zipkin Server Span to Feign Thread
				.requestInterceptor((t) -> brave.serverSpanThreadBinder().setCurrentSpan(serverSpan))
				.logger(new Logger.ErrorLogger()).logLevel(Level.BASIC)
				.decoder(new JacksonDecoder())
				.target(AlohaService.class, url,
						() -> Collections.singletonList("Aloha response (fallback)"));
	}

}
