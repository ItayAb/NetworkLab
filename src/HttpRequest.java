import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Socket;


public class HttpRequest implements Runnable {

	private final String OK_200 = "200 OK";
	private final String NOT_FOUND_404 = "404 Not Found";
	private final String NOT_IMPLEMENTED_501 = "501 Not Implemented";
	private final String BAD_REQUEST_400 = "400 Bad Request";
	private final String INTERNAL_SERVER_ERROR_500 = "500 Internal Server Error";
	
	private final static String CRLF = "\r\n";//CLRF “\r” == 0x0d ; [LF] == “\n” == 0x0a
	
	Socket clientSocket;
	ConfigData data;
    
    // Constructor
    public HttpRequest(Socket socket, ConfigData data) 
	{
		clientSocket = socket;
		this.data = data;
    }
    
    // Implement the run() method of the Runnable interface.
    public void run()
	{
		try
		{
		    processRequest();
		}
		catch (Exception e)
		{
		    System.out.println(e);
		}
	}
    
    

	private void processRequest() throws Exception
	{
		
		BufferedReader bufferedReader = null;
		
		bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		StringBuilder clientRequest = new StringBuilder();
		while (bufferedReader.ready()) {
			// TODO: check about readline						
			clientRequest.append(bufferedReader.readLine() + CRLF);					
		}
		// printing the client's request
		System.out.println("\n" + clientRequest.toString());
		
		String[] clientRequestArray = clientRequest.toString().split(CRLF);
		System.out.println("printing header");
		System.out.println(clientRequestArray[0]);		
		// TODO: Check validity of header
		if(clientRequestArray[0].startsWith("GET")) { // GET request
			String pageRequest = extractPageFromHeader(clientRequestArray[0]);
			System.out.println(pageRequest);
			File requestedPageFile = new File(data.getRoot() + File.separator + pageRequest);
			if (requestedPageFile.exists()) {
				//read content
				System.out.println("File exist!");
			}
			else {
				//TODO: return 404
			}
		}
		else if(clientRequestArray[0].startsWith("POST")) {
			
		}
		else {
			//TODO: return 501
		}
		
		clientSocket.close();
    }
	
	private String extractPageFromHeader(String header) {
		String pageToReturn = null;
		String substringHeader = header.substring(header.indexOf('/') + 1, header.indexOf("HTTP"));
		int indexOfQuestion = substringHeader.indexOf('?');
		if (indexOfQuestion != -1) {
			pageToReturn = substringHeader.substring(0, indexOfQuestion);
		}
		else {
			pageToReturn = substringHeader;
		}
		
		return pageToReturn;
	}
}
