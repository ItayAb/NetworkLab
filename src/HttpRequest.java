import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;

public class HttpRequest implements Runnable {

	private final String OK_200 = "200 OK";
	private final String NOT_FOUND_404 = "404 Not Found";
	private final String NOT_IMPLEMENTED_501 = "501 Not Implemented";
	private final String BAD_REQUEST_400 = "400 Bad Request";
	private final String INTERNAL_SERVER_ERROR_500 = "500 Internal Server Error";

	private HashMap<String, String> contentType = new HashMap<>();

	private final static String CRLF = "\r\n";// CLRF “\r” == 0x0d ; [LF] ==
												// “\n” == 0x0a

	Socket clientSocket;
	ConfigData data;

	// Constructor
	public HttpRequest(Socket socket, ConfigData data) {
		clientSocket = socket;
		this.data = data;
		initDictionary();
	}

	private void initDictionary() {
		contentType.put("bmp", "Content-Type: image");
		contentType.put("gif", "Content-Type: image");
		contentType.put("png", "Content-Type: image");
		contentType.put("jpg", "Content-Type: image");
		contentType.put("html", "Content-Type: text/html");
		contentType.put("ico", "Content-Type: icon");
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
				responseHandler(HttpResponseCode.OK_200, requestedPageFile);
			} else {
				// TODO: return 404
				responseHandler(HttpResponseCode.NOT_FOUND_404, null);
			}
		} else if (clientRequestArray[0].startsWith("POST")) {

		} else {
			// TODO: return 501
			responseHandler(HttpResponseCode.NOT_IMPLEMENTED_501, null);
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

	private void responseHandler(HttpResponseCode typeOfResponse, File requestedPage) {
		StringBuilder httpResponse = new StringBuilder();
		byte[] pageContent = null;

		switch (typeOfResponse) {
		case OK_200:
			httpResponse.append("HTTP/1.0 200 OK" + CRLF);
			httpResponse.append("Content-Type: text/html" + CRLF);
			httpResponse.append("Content-Length: " + requestedPage.getTotalSpace() + CRLF);
			httpResponse.append(CRLF);
			pageContent = getFileContent(requestedPage);

			break;
		// TODO: get the right size of page size of page
		case NOT_FOUND_404:
			//pageContent = createHtmlFormat(NOT_FOUND_404);
			httpResponse.append("HTTP/1.0 " + NOT_FOUND_404 + CRLF);
			// httpResponse.append("Content-Type: text/html" + CRLF);
			// httpResponse.append("Content-Length: " + pageContentStr.length()
			// + CRLF);
			//httpResponse.append(CRLF);
			// httpResponse.append(pageContentStr);
			break;
		case NOT_IMPLEMENTED_501:
			httpResponse.append("HTTP/1.0 " + NOT_IMPLEMENTED_501 + CRLF);
			httpResponse.append(CRLF);
			httpResponse.append(createHtmlFormat(NOT_IMPLEMENTED_501 + CRLF));
			break;
		case BAD_REQUEST_400:
			httpResponse.append("HTTP/1.0 " + BAD_REQUEST_400 + CRLF);
			httpResponse.append(CRLF);
			httpResponse.append(createHtmlFormat(BAD_REQUEST_400 + CRLF));

			break;
		case INTERNAL_SERVER_ERROR_500:
			break;

		default:
			break;
		}

		sendMessageToClient(httpResponse.toString(), pageContent);

	}

	private byte[] createHtmlFormat(String text) {
		String entityBody = "<HTML>" + "<HEAD><TITLE>" + text + "</TITLE></HEAD>" + "<BODY><H1>" + text + "</H1></BODY></HTML>";
		byte[] entityAsByteArray = new byte[entityBody.length()];
		for (int i = 0; i < entityBody.length(); i++) {
			entityAsByteArray[i] = (byte) entityBody.charAt(i);
		}
		return entityAsByteArray;
	}

	private byte[] getFileContent(File fileToGetContent) {
		FileInputStream fileReader = null;
		byte[] pageContent = null;
		try {
			fileReader = new FileInputStream(fileToGetContent);
			pageContent = new byte[(int) fileToGetContent.length()];
			while (fileReader.available() != 0) {
				fileReader.read(pageContent, 0, (int) fileToGetContent.length());
			}

			System.out.println("Size is: " + pageContent.length);
		} catch (Exception e) {
			// TODO: handle exception

		} finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return pageContent;
	}

	private void sendMessageToClient(String responseMessage, byte[] pageContent) {

		DataOutputStream writerToClient = null;
		try {
			writerToClient = new DataOutputStream(clientSocket.getOutputStream());
			// printing response header
			System.out.println(responseMessage.substring(0, responseMessage.indexOf(CRLF)));

			writerToClient.writeBytes(responseMessage);

			if (pageContent != null) {
				System.out.println("Sending content to Client!");
				writerToClient.write(pageContent);
				
				for (int i = 0; i < pageContent.length; i++) {
					System.out.print((char)pageContent[i]);
				}
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
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// Error in closing resources
				e.printStackTrace();
			}
		}
	}
}
