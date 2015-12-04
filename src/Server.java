import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

	private ConfigData data;

	public Server() {
		data = new ConfigData();
		try {
			data.Load();
		} catch (Exception e) {
			System.out.println("Could not open Server!");
			System.out.println("Error in loading data! please check config.ini");
			System.out.println("Error " + e.getMessage());
			System.out.println("Server will shut down...");
		}
	}

	public void Begin() {
		// Establish the listen socket.
		ServerSocket serverSocket = null;
		Socket client = null;
		try {
			serverSocket = new ServerSocket(data.getPort());

			// Process HTTP service requests in an infinite loop.
			while (true) {				
				// Listen for a TCP connection request.
				client = serverSocket.accept();

				// TODO: thread safe 
				
				// Construct an object to process the HTTP request message.
				HttpRequest request = new HttpRequest(client, data);

				// Create a new thread to process the request.
				Thread thread = new Thread(request);

				// Start the thread.
				thread.start();
				
			}
		} catch (IOException e) {			
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
				// TODO Auto-generated catch block
				System.out.println("Error while shutting down, could not close all resources");
			}
		}
	}
}
