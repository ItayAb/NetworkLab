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
	
	public MailNotifier() {
		dataLoader = new MailNotifierDataLoader();
		try {
			dataLoader.loadData();
		} catch (Exception e) {
			System.out.println("Error in loading data for Mail Notifier.");
			System.out.println("Mail Notifier Service is currently unavailable");
		}
	}
	
	public boolean IsServiceAvailabe(){
		return dataLoader.IsLoaded();
	}

	// send message only if isLoaded==true
	public void SendEmail(){
		
		if (dataLoader.IsLoaded()) {
			String to = dataLoader.Recipient();
			final String from = "";
			final String password = "XXX";
			Properties props = new Properties();
			props.put("mail.smtp.starttls.enable", "true"); 
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.user", from); // User name
			props.put("mail.smtp.password", password); // password
			props.put("mail.smtp.port", "587");
			props.put("mail.smtp.auth", "true");
			
			Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator(){
		        protected PasswordAuthentication getPasswordAuthentication() {
		            return new PasswordAuthentication(
		                from, password);}});

			String msgBody = "Sending email using JavaMail API...";

			try {
				Message msg = new MimeMessage(session);
				msg.setFrom(new InternetAddress(from, "NoReply"));
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to, "Mr. Recipient"));
				msg.setSubject("Welcome To Java Mail API");
				msg.setText(msgBody);
				Transport.send(msg);
				System.out.println("Email sent successfully...");

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
