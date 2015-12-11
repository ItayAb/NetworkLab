import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class HttpRequest implements Runnable {

	private final String OK_200 = "200 OK";
	private final String NOT_FOUND_404 = "404 Not Found";
	private final String NOT_IMPLEMENTED_501 = "501 Not Implemented";
	private final String BAD_REQUEST_400 = "400 Bad Request";
	private final String INTERNAL_SERVER_ERROR_500 = "500 Internal Server Error";

	private HashMap<String, String> contentType = new HashMap<>();

	private final static String CRLF = "\r\n";// CLRF “\r” == 0x0d ; [LF] ==
												// “\n” == 0x0a
	private final static String SERVER_NAME = "Snir Itay Server";
	private Socket clientSocket;
	private ConfigData data;
	private Semaphore threadPool;
	private HashMap<String, String> paramsFormClient;
	private String requestHeader;
	private Request requestOfClient;

	// Constructor
	public HttpRequest(Socket socket, ConfigData data, Semaphore threadPool) {
		requestOfClient = new Request();
		clientSocket = socket;
		this.data = data;
		this.threadPool = threadPool;
		initDictionary();
		paramsFormClient = new HashMap<>();
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
			//processRequest();
			proccessRequestTest();
		} catch (Exception e) {
			System.out.println("Error: could not send response!");
		} finally {
			threadPool.release();
			
			if (!clientSocket.isClosed()) {
				try {
					clientSocket.close();
				} catch (IOException e) {
					System.out.println("Error while closing connection");
				}
			}
		}
	}

	private void proccessRequestTest() throws IOException{
		requestOfClient.ParseRequest(clientSocket);
		System.out.println("Header: \n" + requestOfClient.Header.toString());
		System.out.println("Body: \n" + requestOfClient.Body.toString());
		getHandler(requestOfClient.Header.toString().split("\n"), requestOfClient.requestType);
		
	}
	// TODO: implement Chunks	
	private void processRequest() throws Exception {
		BufferedReader bufferedReader = null;
		String inputMessage = "";
		bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		StringBuilder clientRequest = new StringBuilder();
		while ((inputMessage = bufferedReader.readLine()) != null && inputMessage.length() > 0) {
			// TODO: check about readline
			clientRequest.append(inputMessage + "\n"); //TODO: see if /n or CRLF
		}
		String[] clientRequestArray = clientRequest.toString().split(CRLF);
		
		// printing the client's header request
		System.out.println("Header is: \n" + clientRequest.toString());
		// TODO: Check validity of header
		
		String requestType = clientRequestArray[0];
		
		if (requestType.startsWith(RequestType.GET.toString())) {
			getHandler(clientRequestArray, RequestType.GET);
		} else if (requestType.startsWith(RequestType.POST.toString())) {
			getHandler(clientRequestArray, RequestType.POST);
		} else if (requestType.startsWith(RequestType.HEAD.toString())) {
			getHandler(clientRequestArray, RequestType.HEAD);
		} else if (requestType.startsWith(RequestType.TRACE.toString())) {
			getHandler(clientRequestArray, RequestType.TRACE);
		} else {
			// TODO: return 501
			System.out.println("Problem Header: \n**" + clientRequest.toString() + "**");
			responseHandler(HttpResponseCode.NOT_IMPLEMENTED_501, null, null, null);
		}

		clientSocket.close();
	}

	private void getHandler(String[] clientRequestArray, RequestType type) {
		File requestedPageFile;
		requestedPageFile = new File(data.getRoot() + File.separator + extractPageFromRequest(clientRequestArray[0]));
		updateGetParamaters(clientRequestArray[0]);
		if (requestedPageFile.exists()) {
			String extension = getExtension(requestedPageFile);

			if (contentType.containsKey(extension)) {
				// 200_OK
				responseHandler(HttpResponseCode.OK_200, requestedPageFile, extension, type);
			} else {
				// TODO : not supported
				responseHandler(HttpResponseCode.BAD_REQUEST_400, null, null, type);
			}
		}
		else {
			// TODO: return 404
			responseHandler(HttpResponseCode.NOT_FOUND_404, null, null, type);
		}
	}

	private void updateGetParamaters(String request) {
		int startParamaters = request.indexOf('?') + 1;
		int lastParamaters = request.indexOf(" ", startParamaters - 1);
		if (startParamaters != -1) {
			if (lastParamaters != -1) {
				String paramaters = request.substring(startParamaters, lastParamaters).trim();
				String[] paramatersArray = paramaters.split("&");
				for (int i = 0; i < paramatersArray.length; i++) {
					String[] split = paramatersArray[i].split("=");
					if (split.length == 2) {
						paramsFormClient.put(split[0], split[1]);
					} else if (split.length == 1) { // TODO: if there is a case
													// of "x= "
						paramsFormClient.put(split[0], "");
					}
				}
			}
		}
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

		if (pageToReturn.contains("../")) { // TODO: check if there are several occurrences
			int indexOfForbidden = pageToReturn.indexOf("../");
			pageToReturn = pageToReturn.substring(0, indexOfForbidden - 1)
					+ pageToReturn.substring(indexOfForbidden + 2);
		}
		
		return pageToReturn;
	}

	private String getExtension(File requestedPage) {

		String path = requestedPage.getPath();
		String extension = "";

		int indexLastPoint = path.lastIndexOf('.') + 1;

		if (indexLastPoint != -1 && path.length() > indexLastPoint) {
			extension = path.substring(indexLastPoint);
		}

		return extension.toLowerCase().trim();

	}

	private void responseHandler(HttpResponseCode typeOfResponse, File requestedPage, String extension,
			RequestType type) {
		// TODO : content-type: application/octet-stream
		StringBuilder httpResponse = new StringBuilder();
		byte[] pageContent = null;	
		if (requestedPage.getAbsoluteFile().equals("C:\\serverroot\\params_info.html")) {
			pageContent = requestOfClient.initRequestedPage().getBytes();
		}
		// TODO: get time Stamp!
		switch (typeOfResponse) {
		case OK_200:
			if (type == RequestType.TRACE) {
				pageContent = requestHeader.getBytes();
			}
			else if (type != RequestType.HEAD) {
				pageContent = getFileContent(requestedPage);
			}// TODO: server:?, host:?
			httpResponse.append("HTTP/1.0 " + OK_200 + CRLF);
			httpResponse.append("Date: "  + CRLF);
			httpResponse.append("Server: " + SERVER_NAME + CRLF);
			httpResponse.append(contentType.get(extension) + CRLF);
			httpResponse.append("Content-Length: " + pageContent.length + CRLF);
			httpResponse.append(CRLF);
			break;
		case NOT_FOUND_404:
			pageContent = createHtmlFormat(NOT_FOUND_404);
			httpResponse.append("HTTP/1.0 " + NOT_FOUND_404 + CRLF);
			httpResponse.append("Date: "   + CRLF);
			httpResponse.append("Server: " + SERVER_NAME + CRLF);
			httpResponse.append("Content-Type: text/html" + CRLF);
			httpResponse.append("Content-Length: " + pageContent.length + CRLF);
			httpResponse.append(CRLF);
			break;
		case NOT_IMPLEMENTED_501:
			pageContent = createHtmlFormat(NOT_IMPLEMENTED_501);			
			httpResponse.append("HTTP/1.0 " + NOT_IMPLEMENTED_501 + CRLF);
			httpResponse.append("Date: "   + CRLF);
			httpResponse.append("Server: " + SERVER_NAME  + CRLF);
			httpResponse.append("Content-Type: text/html" + CRLF);
			httpResponse.append("Content-Length: " + pageContent.length + CRLF);
			httpResponse.append(CRLF);
			break;
		case BAD_REQUEST_400:
			pageContent = createHtmlFormat(BAD_REQUEST_400);
			httpResponse.append("HTTP/1.0 " + BAD_REQUEST_400 + CRLF);
			httpResponse.append("Date: "   + CRLF);
			httpResponse.append("Server: " + SERVER_NAME + CRLF);
			httpResponse.append("Content-Type: text/html" + CRLF);
			httpResponse.append("Content-Length: " + pageContent.length + CRLF);
			httpResponse.append(CRLF);
			break;
		case INTERNAL_SERVER_ERROR_500:
			break;

		default:
			break;
		}

		sendMessageToClient(httpResponse.toString(), pageContent);

	}

	private byte[] createHtmlFormat(String text) {
		String entityBody = "<HTML>" + "<HEAD><TITLE>" + text + "</TITLE></HEAD>" + "<BODY><H1>" + text
				+ "</H1></BODY></HTML>";
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
		} catch (Exception e) {
			System.out.println("Error in reading request page!");
			sendMessageToClient(INTERNAL_SERVER_ERROR_500, createHtmlFormat(INTERNAL_SERVER_ERROR_500));

		} finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Error in closing resources!");
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
			System.out.println("Response Header:\n" + responseMessage);
		
			writerToClient.writeBytes(responseMessage);

			if (pageContent != null) {
				writerToClient.write(pageContent);
				writerToClient.flush();
			}

		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.out.println("Error in writing response to client!");
			// TODO: if connection is closed while closing
		} finally {
			try {
				if (writerToClient != null) {
					writerToClient.close();
				}
			} catch (IOException e) {
				// Error in closing resources
				System.out.println("Error in closing client output stream!");
			}
		}
	}
}
