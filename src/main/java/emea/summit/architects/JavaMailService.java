package emea.summit.architects;

import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class JavaMailService {

	static Properties mailServerProperties;
	static Session getMailSession;
	static MimeMessage generateMailMessage;
 
	public static void main(String args[]) throws AddressException, MessagingException {
		generateAndSendEmail("HERE WILL BE THE REAL CONTENT");
		System.out.println("\n\n ===> Your Java Program has just sent an Email successfully. Check your email..");
	}
 
	public static void generateAndSendEmail(String emalContent) throws AddressException, MessagingException {
 
		// Step1
		System.out.println("\n 1st ===> setup Mail Server Properties..");
		mailServerProperties = System.getProperties();
		mailServerProperties.put("mail.smtp.port", "587");
		mailServerProperties.put("mail.smtp.auth", "true");
		mailServerProperties.put("mail.smtp.starttls.enable", "true");
		System.out.println("Mail Server Properties have been setup successfully..");
 
		// Step2
		System.out.println("\n\n 2nd ===> get Mail Session..");
		getMailSession = Session.getDefaultInstance(mailServerProperties, null);
		generateMailMessage = new MimeMessage(getMailSession);
		generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress("stelios@redhat.com"));
//		generateMailMessage.addRecipient(Message.RecipientType.CC, new InternetAddress("test2@crunchify.com"));
		generateMailMessage.setSubject("Greetings Santa from EMEA Architects PAAS Hackathlon..");
		String emailBody = "Test email by EMEA Architects PAAS Hackathlon. " + "<br><br> Regards, <br>Hackathlon Admin";
		emailBody = "<br><br>*********************************"+emalContent+"<br><br>*********************************";
		generateMailMessage.setContent(emailBody, "text/html");
		System.out.println("Mail Session has been created successfully..");
 
		// Step3
		System.out.println("\n\n 3rd ===> Get Session and Send mail");
		Transport transport = getMailSession.getTransport("smtp");
 
		// Enter your correct gmail UserID and Password
		// if you have 2FA enabled then provide App Specific Password
		//transport.connect("smtp.gmail.com", "<----- Your GMAIL ID ----->", "<----- Your GMAIL PASSWORD ----->");
		transport.connect("smtp.gmail.com", "koussouris.stelios@gmail.com", "greece106");
		transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
		transport.close();
	}
	
	public static void generateAndSendEmail(String emalContent, String emailSubject, List<String> emailList) throws AddressException, MessagingException {
		 
		// Step1
		System.out.println("\n 1st ===> setup Mail Server Properties..");
		mailServerProperties = System.getProperties();
		mailServerProperties.put("mail.smtp.port", "587");
		mailServerProperties.put("mail.smtp.auth", "true");
		mailServerProperties.put("mail.smtp.starttls.enable", "true");
		System.out.println("Mail Server Properties have been setup successfully..");
 
		// Step2
		System.out.println("\n\n 2nd ===> get Mail Session..");
		getMailSession = Session.getDefaultInstance(mailServerProperties, null);
		generateMailMessage = new MimeMessage(getMailSession);
		generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress("stelios@redhat.com"));
		System.out.println("Recipients: stelios@redhat.com");
		if (emailList != null) {
			for (String emailAddress : emailList) {
				generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress));
				System.out.println(emailAddress);
			}
		}
//		generateMailMessage.addRecipient(Message.RecipientType.CC, new InternetAddress("test2@crunchify.com"));
		if (emailSubject == null) {
			generateMailMessage.setSubject("Greetings Santa from EMEA Architects PAAS Hackathlon");
			System.out.println("Subject: Greetings Santa from EMEA Architects PAAS Hackathlon.");
		} else {
			generateMailMessage.setSubject(emailSubject);
			System.out.println(emailSubject);
		}
		String emailBody = "Email by EMEA Architects PAAS Hackathlon. " + "<br><br> Regards, <br>Hackathlon Admin";
		emailBody = "<br><br>*********************************\n"+emalContent+"\n<br><br>*********************************";
		generateMailMessage.setContent(emailBody, "text/html");
		System.out.println("Email Body: "+emailBody);
		System.out.println("Mail Session has been created successfully..");
 
		// Step3
		System.out.println("\n\n 3rd ===> Get Session and Send mail");
		Transport transport = getMailSession.getTransport("smtp");
 
		// Enter your correct gmail UserID and Password
		// if you have 2FA enabled then provide App Specific Password
		//transport.connect("smtp.gmail.com", "<----- Your GMAIL ID ----->", "<----- Your GMAIL PASSWORD ----->");
		transport.connect("smtp.gmail.com", "koussouris.stelios@gmail.com", "greece106");
		transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
		transport.close();
	}
}
