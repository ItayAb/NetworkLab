
public class Program {
	
	public static void main(String[] args) {
		Server server = new Server();
		//server.Begin();
		MailNotifier mail = new MailNotifier();
		if (mail.IsServiceAvailabe()) {
			mail.SendEmail();
		}
	}

}
