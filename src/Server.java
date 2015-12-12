import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class Server {

	private ConfigData data;
	private Semaphore threadPool;
	private boolean isMailSent = false;
	MailNotifier mailNotfiyService;
	private static Object lock = new Object();
	
	public Server() {
		mailNotfiyService = new MailNotifier();
		data = new ConfigData();
		try {
			data.Load();
			threadPool = new Semaphore(data.getMaxThreads());
		} catch (Exception e) {
			System.out.println("Could not open Server!");
			System.out.println("Error in loading data! please check config.ini");
			System.out.println("Server will shut down...");
		}
	}	
	
	private void sendMail(){		
		if (!isMailSent && mailNotfiyService.IsServiceAvailabe()) {
			synchronized (lock) {
				if (!isMailSent && mailNotfiyService.IsServiceAvailabe()) {
					isMailSent = true;
					mailNotfiyService.SendEmail();
				}
			}
		}
	}
	
	public void Begin() {
		// Establish the listen socket.
		ServerSocket serverSocket = null;
		Socket client = null;
		try {
			serverSocket = new ServerSocket(data.getPort());		
			
			System.out.println("Listening on port: " + data.getPort());
			// Process HTTP service requests in an infinite loop.
			while (true) {				
				threadPool.acquire();
				// Listen for a TCP connection request.
				client = serverSocket.accept();
				// sending the mail
				sendMail();
					
				// Construct an object to process the HTTP request message.
				HttpRequest request = new HttpRequest(client, data, threadPool);
				
				// Create a new thread to process the request.
				Thread thread = new Thread(request);

				// Start the thread.
				thread.start();
				
				
			}
		} catch (IOException | InterruptedException e) {			
			System.out.println("Error in server initialization\nServer is shutting down...");
		} finally {
			
			try {
				if (serverSocket != null) {
					serverSocket.close();
				}
				if (client != null) {
					client.close();
				}
			} catch (IOException e) {
				System.out.println("Error while shutting down, could not close all resources");
			}
		}
	}
}
