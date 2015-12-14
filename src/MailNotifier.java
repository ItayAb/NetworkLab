import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailNotifier {

	private MailNotifierDataLoader dataLoader;
	private final String FROM = "LabNetworksWebServer@gmail.com";
	private final String PASSWORD = "Networks2015";
	private final String SUBJECT = "Activity in server"; 
	private final String BODY_MESSAGE = "There has been a connection to the server";
	
	public MailNotifier() {
		dataLoader = new MailNotifierDataLoader();
		try {
			dataLoader.loadData();
			if (!dataLoader.IsLoaded()) {
				System.out.println("Mail Notifier Service is unavailable");
			}
		} catch (Exception e) {
			System.out.println("Error in loading data for Mail Notifier.");
			System.out.println("Mail Notifier Service is unavailable");
		}
	}
	
	public boolean IsServiceAvailabe(){
		return dataLoader.IsLoaded();
	}

	// send message only if isLoaded==true
	public void SendEmail(){
		
		if (dataLoader.IsLoaded()) {
			String to = dataLoader.Recipient();
			Properties props = new Properties();
			props.put("mail.smtp.starttls.enable", "true"); 
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.user", FROM); // User name
			props.put("mail.smtp.password", PASSWORD); // password
			props.put("mail.smtp.port", "587");
			props.put("mail.smtp.auth", "true");
			
			Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator(){
		        protected PasswordAuthentication getPasswordAuthentication() {
		            return new PasswordAuthentication(
		                FROM, PASSWORD);}});

			String msgBody = BODY_MESSAGE;

			try {
				Message msg = new MimeMessage(session);
				msg.setFrom(new InternetAddress(FROM, "NoReply"));
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
				msg.setSubject(SUBJECT);
				msg.setText(msgBody);
				Transport.send(msg);
				System.out.println("An email notification was sent to " + dataLoader.Recipient());

			} catch (AddressException e) {
				System.out.println("Error in sending mail");
			} catch (MessagingException e) {
				System.out.println("Error in sending mail");
			} catch (UnsupportedEncodingException e) {
				System.out.println("Error in sending mail");
			}
		}
	}
}
