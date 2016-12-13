package emea.summit.architects;

import java.util.List;

public class TeamPayload {

	private String serviceName;
	private List<RequestPayload> payload;
	
	public TeamPayload() {
		
	}
	
	public TeamPayload(String serviceName, List<RequestPayload> payload) {
		super();
		this.serviceName = serviceName;
		this.payload = payload;
	}
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public List<RequestPayload> getPayload() {
		return payload;
	}
	public void setPayload(List<RequestPayload> payload) {
		this.payload = payload;
	}
	
	
}
