import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class HttpRequest implements Runnable {

	private final String OK_200 = "200 OK";
	private final String NOT_FOUND_404 = "404 Not Found";
	private final String NOT_IMPLEMENTED_501 = "501 Not Implemented";
	private final String BAD_REQUEST_400 = "400 Bad Request";
	private final String INTERNAL_SERVER_ERROR_500 = "500 Internal Server Error";

	private HashMap<String, String> contentType = new HashMap<>();

	private final static String CRLF = "\r\n";// CLRF “\r” == 0x0d ; [LF] == “\n” == 0x0a										
	private final static String SERVER_NAME = "Snir Itay Server";
	private Socket clientSocket;
	private Semaphore threadPool;
	private Request requestOfClient;

	// Constructor
	public HttpRequest(Socket socket, ConfigData data, Semaphore threadPool) {
		requestOfClient = new Request(data);
		clientSocket = socket;
		this.threadPool = threadPool;
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
			proccessRequest();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
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

	private void proccessRequest() throws Exception {
		requestOfClient.ParseRequest(clientSocket);
		System.out.println("Header: \n" + requestOfClient.Header.toString());
		// web crawler need to be initialized
		if (requestOfClient.Header.toString().startsWith("POST /params_info.html HTTP/1.1")) {
			WebCrawler webCrawler = new WebCrawler(requestOfClient.serverData, requestOfClient.paramsFromClient); // TODO: think how to wisely pass the DataConfig paramater
			webCrawler.Run();
			//webCrawler.sendResult();
			// TODO: return the message "Crawler Started" "Crawler failed"
		}
		else { // all other cases than the above request the server serves as normal server
			System.out.println("For some reason " + requestOfClient.Header.toString() + requestOfClient.Body.toString() + "End!");
			responseHandler();
		}
	}
	
	private String getExtension(String fileName) {
		String extension = "";

		int indexLastPoint = fileName.lastIndexOf('.') + 1;

		if (indexLastPoint != -1 && fileName.length() > indexLastPoint) {
			extension = fileName.substring(indexLastPoint).toLowerCase().trim();
		}

		return extension;
	}
	
	private String getContentTypeInHeader(String requestPage){
		String contentTypeHeader = null;
		String extension = getExtension(requestPage);
		if (contentType.containsKey(extension)) {
			contentTypeHeader = contentType.get(extension);
		}
		else {
			contentTypeHeader = "content-type: application/octet-stream";
		}
		
		return contentTypeHeader;
	}

	private void responseHandler() {
		StringBuilder httpResponseHeader = new StringBuilder();
		byte[] httpResponseBody = null;
		String date = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzzz", Locale.US).format(System.currentTimeMillis());
		switch (requestOfClient.responseCode) {
		case OK_200:
			
			httpResponseBody = getBodyOfResponse();
			httpResponseHeader.append("HTTP/1.0 " + OK_200 + CRLF);
			httpResponseHeader.append("Date: " + date + CRLF);
			httpResponseHeader.append("Server: " + SERVER_NAME + CRLF);
			httpResponseHeader.append(getContentTypeInHeader(requestOfClient.requestedPage) + CRLF);
			if (requestOfClient.isChunked) {
				httpResponseHeader.append("transfer-encoding: chunked");
			}
			else {
				httpResponseHeader.append("Content-Length: " + httpResponseBody.length + CRLF);
			}
			
			httpResponseHeader.append(CRLF);
			break;
		case NOT_FOUND_404:
			httpResponseBody = createHtmlFormat(NOT_FOUND_404);
			httpResponseHeader.append("HTTP/1.0 " + NOT_FOUND_404 + CRLF);
			httpResponseHeader.append("Date: " + date + CRLF);
			httpResponseHeader.append("Server: " + SERVER_NAME + CRLF);
			httpResponseHeader.append("Content-Type: text/html" + CRLF);
			httpResponseHeader.append("Content-Length: " + httpResponseBody.length + CRLF);
			httpResponseHeader.append(CRLF);
			break;
		case NOT_IMPLEMENTED_501:
			httpResponseBody = createHtmlFormat(NOT_IMPLEMENTED_501);
			httpResponseHeader.append("HTTP/1.0 " + NOT_IMPLEMENTED_501 + CRLF);
			httpResponseHeader.append("Date: " + date + CRLF);
			httpResponseHeader.append("Server: " + SERVER_NAME + CRLF);
			httpResponseHeader.append("Content-Type: text/html" + CRLF);
			httpResponseHeader.append("Content-Length: " + httpResponseBody.length + CRLF);
			httpResponseHeader.append(CRLF);
			break;
		case BAD_REQUEST_400:
			httpResponseBody = createHtmlFormat(BAD_REQUEST_400);
			httpResponseHeader.append("HTTP/1.0 " + BAD_REQUEST_400 + CRLF);
			httpResponseHeader.append("Date: " + date + CRLF);
			httpResponseHeader.append("Server: " + SERVER_NAME + CRLF);
			httpResponseHeader.append("Content-Type: text/html" + CRLF);
			httpResponseHeader.append("Content-Length: " + httpResponseBody.length + CRLF);
			httpResponseHeader.append(CRLF);
			break;
		case INTERNAL_SERVER_ERROR_500:
			httpResponseBody = createHtmlFormat(INTERNAL_SERVER_ERROR_500);
			httpResponseHeader.append("HTTP/1.0 " + INTERNAL_SERVER_ERROR_500 + CRLF);
			httpResponseHeader.append("Date: " + date + CRLF);
			httpResponseHeader.append("Server: " + SERVER_NAME + CRLF);
			httpResponseHeader.append("Content-Type: text/html" + CRLF);
			httpResponseHeader.append("Content-Length: " + httpResponseBody.length + CRLF);
			httpResponseHeader.append(CRLF);
			break;

		default:
			break;
		}

		sendMessageToClient(httpResponseHeader.toString(), httpResponseBody);

	}

	
	private byte[] getBodyOfResponse(){
		byte[] bodyToReturn = null;
		switch (requestOfClient.requestType) {
		case GET:
			bodyToReturn = getFileContent(requestOfClient.requestedFile);
			break;
		case HEAD:
			bodyToReturn = getFileContent(requestOfClient.requestedFile);
			break;
		case POST:
			bodyToReturn = getFileContent(requestOfClient.requestedFile);
			break;
		case TRACE:
			bodyToReturn = requestOfClient.Header.toString().getBytes();
			break;
		case OPTIONS:
			StringBuilder allRequest = new StringBuilder();
			allRequest.append("Allow: ");
			int counterOfRequest = RequestType.values().length;
			for (RequestType requestRunner : RequestType.values()) {
				if (counterOfRequest-- > 1) {
					allRequest.append(requestRunner.toString() + ", ");					
				}
				else {
					allRequest.append(requestRunner);
				}
			}
			
			bodyToReturn = allRequest.toString().getBytes();
			break;
		}
		
		return bodyToReturn;
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
		} catch (Exception e) {
			System.out.println("Error in reading request page!");
			requestOfClient.responseCode = HttpResponseCode.INTERNAL_SERVER_ERROR_500;
			responseHandler();
		} finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException e) {
					System.out.println("Error in closing resources!");
				}
			}
		}

		return pageContent;
	}

	private void sendMessageToClient(String responseHeader, byte[] responseBody) {

		DataOutputStream writerToClient = null;
		try {

			writerToClient = new DataOutputStream(clientSocket.getOutputStream());
			// printing response header
			System.out.println("Response Header:\n" + responseHeader);

			writerToClient.writeBytes(responseHeader);

			// writing Body to client
			if (responseBody != null && requestOfClient.requestType != RequestType.HEAD) {
				if (requestOfClient.isChunked) { //in case chunked
					int currentChunkSize = -1;
					int pageContentCounter = 0;
					while (pageContentCounter < responseBody.length) {
						currentChunkSize = new Random().nextInt(100) + 1;
						int fillChunkCounter = 0;
						byte[] chunkedData = new byte[currentChunkSize];
						// loading the chunk
						while (fillChunkCounter < currentChunkSize && pageContentCounter < responseBody.length) {
							chunkedData[fillChunkCounter++] = responseBody[pageContentCounter++];
						}
						// writing the chunk to client
						writerToClient.writeBytes(Integer.toHexString(fillChunkCounter) + CRLF);
						writerToClient.writeBytes(CRLF);
						writerToClient.write(chunkedData, 0, fillChunkCounter);	
						writerToClient.writeBytes(CRLF);
						writerToClient.flush();
					}
					// write ending chunk
					writerToClient.writeBytes(0 + CRLF);
					writerToClient.writeBytes(CRLF);
					writerToClient.flush();
				}
				else {
					writerToClient.write(responseBody);
					writerToClient.flush();					
				}
			}

		} catch (IOException e) {
			System.out.println("Error in writing response to client!");
			e.printStackTrace();
			System.out.println("Error sending " + responseHeader + "*" );
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
