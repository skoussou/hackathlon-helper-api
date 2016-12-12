package emea.summit.architects;

import java.util.List;

public class EmailPayload {
	
    private List<RequestPayload> content;
    private String subject;
    private List<String> emailAddresses;

    public EmailPayload(){
    	
    }
    
    public EmailPayload(List<RequestPayload> content, String subject, List<String> emailAddresses) {
		super();
		this.content = content;
		this.subject = subject;
		this.emailAddresses = emailAddresses;
	}

	public List<RequestPayload> getContent() {
		return content;
	}

	public void setContent(List<RequestPayload> content) {
		this.content = content;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public List<String> getEmailAddresses() {
		return emailAddresses;
	}

	public void setEmailAddresses(List<String> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}

    
}
