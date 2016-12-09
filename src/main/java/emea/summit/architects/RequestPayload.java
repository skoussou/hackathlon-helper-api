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
	//private List<TeamContent> teamsContentsList;
	private String reindeerName;
	private HashMap<String, String> nameEmaiMap;

	public RequestPayload() {
		
	}
	
//	public RequestPayload(String teamName, List<TeamContent> listOfContents) {
//		super();
//		this.teamName = teamName;
//		this.teamsContentsList = listOfContents;
//	}
	
	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

//	public List<TeamContent> getTeamsContentsList() {
//		return teamsContentsList;
//	}
//
//	public void setTeamsContentsList(List<TeamContent> teamsContentsList) {
//		this.teamsContentsList = teamsContentsList;
//	}

	public String getReindeerName() {
		return reindeerName;
	}

	public void setReindeerName(String reindeerName) {
		this.reindeerName = reindeerName;
	}

	

	public HashMap<String, String> getNameEmaiMap() {
		return nameEmaiMap;
	}

	public void setNameEmaiMap(HashMap<String, String> nameEmaiMap) {
		this.nameEmaiMap = nameEmaiMap;
	}
	
	

	@Override
	public String toString() {
		return "RequestPayload [teamName=" + teamName + ", reindeerName=" + reindeerName + ", nameEmaiMap="
				+ nameEmaiMap + "]";
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

		public String getReindeerName() {
			return reindeerName;
		}

		public void setReindeerName(String reindeerName) {
			this.reindeerName = reindeerName;
		}

		public HashMap<String, String> getNamEmaiMap() {
			return namEmaiMap;
		}

		public void setNamEmaiMap(HashMap<String, String> namEmaiMap) {
			this.namEmaiMap = namEmaiMap;
		}

	}
	
}
