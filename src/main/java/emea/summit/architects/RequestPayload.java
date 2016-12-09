package emea.summit.architects;

import java.util.HashMap;
import java.util.List;

/**
 * The payload all services will exchange and the validate service will validate against
 * @author stelios@redhat.com
 *
 */
public class RequestPayload {
	private String teamName;
	private List<TeamContent> teamsContentsList;

	private String reindeerName;
	private HashMap<String, String> nameToEmail;

	public RequestPayload() {
		
	}
	
	public RequestPayload(String teamName, List<TeamContent> listOfContents) {
		super();
		this.teamName = teamName;
		this.teamsContentsList = listOfContents;
	}

	private class TeamContent {
		private String reindeerName;
		private HashMap<String, String> namEmaiMap;

		public TeamContent(){

		}

		public TeamContent(String reindeerName, HashMap<String, String> nameToEmail) {
			super();
			this.reindeerName = reindeerName;
			this.namEmaiMap = nameToEmail;
		}
	}
}
