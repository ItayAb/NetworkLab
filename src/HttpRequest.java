import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class HttpRequest implements Runnable {

	private final String OK_200 = "200 OK";
	private final String NOT_FOUND_404 = "404 Not Found";
	private final String NOT_IMPLEMENTED_501 = "501 Not Implemented";
	private final String BAD_REQUEST_400 = "400 Bad Request";
	private final String INTERNAL_SERVER_ERROR_500 = "500 Internal Server Error";

	private final static String CRLF = "\r\n";// CLRF “\r” == 0x0d ; [LF] ==
												// “\n” == 0x0a

	Socket clientSocket;
	ConfigData data;

	// Constructor
	public HttpRequest(Socket socket, ConfigData data) {
		clientSocket = socket;
		this.data = data;
	}

	// Implement the run() method of the Runnable interface.
	public void run() {
		try {
			processRequest();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private void processRequest() throws Exception {

		BufferedReader bufferedReader = null;

		bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		StringBuilder clientRequest = new StringBuilder();
		while (bufferedReader.ready()) {
			// TODO: check about readline
			clientRequest.append(bufferedReader.readLine() + CRLF);
		}

		String[] clientRequestArray = clientRequest.toString().split(CRLF);
		// printing the client's header request
		System.out.println(clientRequestArray[0]);
		// TODO: Check validity of header
		if (clientRequestArray[0].startsWith("GET")) { // GET request
			String pageRequest = extractPageFromRequest(clientRequestArray[0]);
			File requestedPageFile = new File(data.getRoot() + File.separator + pageRequest);
			if (requestedPageFile.exists()) {
				// 200_OK
				// read content
				String responseMessage = createHttpRespones(HttpResponseCode.OK_200, requestedPageFile);
				sendMessageToClient(responseMessage, requestedPageFile);
			} else {
				// TODO: return 404
				sendMessageToClient(NOT_FOUND_404, null);
			}
		} else if (clientRequestArray[0].startsWith("POST")) {

		} else {
			// TODO: return 501
			sendMessageToClient(NOT_IMPLEMENTED_501, null);
		}

		clientSocket.close();
	}

	private String extractPageFromRequest(String header) {
		String pageToReturn = null;
		String substringHeader = header.substring(header.indexOf('/') + 1, header.indexOf("HTTP"));
		int indexOfQuestion = substringHeader.indexOf('?');
		if (indexOfQuestion != -1) {
			pageToReturn = substringHeader.substring(0, indexOfQuestion);
		} else {
			pageToReturn = substringHeader;
		}

		return pageToReturn;
	}

	private String createHttpRespones(HttpResponseCode typeOfResponse, File requestedPage) {
		StringBuilder httpResponse = new StringBuilder();

		switch (typeOfResponse) {
		case OK_200:
			httpResponse.append("HTTP/1.0 200 OK" + CRLF);
			httpResponse.append("Content-Type: text/html" + CRLF);
			httpResponse.append("Content-Length: " + requestedPage.getTotalSpace() + CRLF);
			break;
		case NOT_FOUND_404:			
			break;
		case NOT_IMPLEMENTED_501:

			break;
		case BAD_REQUEST_400:

			break;
		case INTERNAL_SERVER_ERROR_500:
			break;

		default:
			break;
		}

		httpResponse.append(CRLF);

		return httpResponse.toString();
	}

	private void sendMessageToClient(String responseMessage, File requestedPage) {

		DataOutputStream writerToClient = null;
		FileInputStream fileReader = null;
		try {
			writerToClient = new DataOutputStream(clientSocket.getOutputStream());

			if (requestedPage != null) {
				fileReader = new FileInputStream(requestedPage);
				byte[] pageContent = new byte[(int) requestedPage.length()];
				// printing response header
				System.out.println(responseMessage.substring(0, responseMessage.indexOf(CRLF)));
				writerToClient.writeBytes(responseMessage);

				while (fileReader.available() != 0) {
					fileReader.read(pageContent, 0, (int) requestedPage.length());
				}

				writerToClient.write(pageContent);
			} else {
				String entityBody = "<HTML>" + "<HEAD><TITLE>" + responseMessage + "</TITLE></HEAD>" + "<BODY><H1>" + responseMessage + "</H1></BODY></HTML>";
				writerToClient.writeBytes(entityBody);
			}

			writerToClient.flush();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			// TODO: if connection is closed while closing
			e.printStackTrace();
		} finally {
			try {
				if (writerToClient != null) {
					writerToClient.close();
				}
				if (fileReader != null) {
					fileReader.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// Error in closing resources
				e.printStackTrace();
			}
		}
	}
}
