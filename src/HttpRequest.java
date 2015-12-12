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

	private void proccessRequest() throws IOException {
		requestOfClient.ParseRequest(clientSocket);
		System.out.println("Header: \n" + requestOfClient.Header.toString());
		System.out.println("Body: \n" + requestOfClient.Body.toString());
		responseHandler();
	}
	// TODO: implement Chunks
	
	private String getExtension(String fileName) {
		String extension = "";

		int indexLastPoint = fileName.lastIndexOf('.') + 1;

		if (indexLastPoint != -1 && fileName.length() > indexLastPoint) {
			extension = fileName.substring(indexLastPoint).toLowerCase().trim();
		}

		return extension;
	}

	private void responseHandler() {
		// TODO : content-type: application/octet-stream
		StringBuilder httpResponse = new StringBuilder();
		byte[] pageContent = null;
		String date = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzzz", Locale.US).format(System.currentTimeMillis());
		switch (requestOfClient.responseCode) {
		case OK_200:
			
			if (requestOfClient.requestType == RequestType.TRACE) {
				pageContent = requestOfClient.Header.toString().getBytes();
			} else if (requestOfClient.requestType != RequestType.HEAD) {
				pageContent = getFileContent(requestOfClient.requestedFile);
			}
			
			httpResponse.append("HTTP/1.0 " + OK_200 + CRLF);
			httpResponse.append("Date: " + date + CRLF);
			httpResponse.append("Server: " + SERVER_NAME + CRLF);
			httpResponse.append(contentType.get(getExtension(requestOfClient.requestedPage)) + CRLF);
			if (requestOfClient.isChunked) {
				httpResponse.append("transfer-encoding: chunked");
			}
			else {
				httpResponse.append("Content-Length: " + pageContent.length + CRLF);
			}
			
			httpResponse.append(CRLF);
			break;
		case NOT_FOUND_404:
			pageContent = createHtmlFormat(NOT_FOUND_404);
			httpResponse.append("HTTP/1.0 " + NOT_FOUND_404 + CRLF);
			httpResponse.append("Date: " + date + CRLF);
			httpResponse.append("Server: " + SERVER_NAME + CRLF);
			httpResponse.append("Content-Type: text/html" + CRLF);
			httpResponse.append("Content-Length: " + pageContent.length + CRLF);
			httpResponse.append(CRLF);
			break;
		case NOT_IMPLEMENTED_501:
			pageContent = createHtmlFormat(NOT_IMPLEMENTED_501);
			httpResponse.append("HTTP/1.0 " + NOT_IMPLEMENTED_501 + CRLF);
			httpResponse.append("Date: " + date + CRLF);
			httpResponse.append("Server: " + SERVER_NAME + CRLF);
			httpResponse.append("Content-Type: text/html" + CRLF);
			httpResponse.append("Content-Length: " + pageContent.length + CRLF);
			httpResponse.append(CRLF);
			break;
		case BAD_REQUEST_400:
			pageContent = createHtmlFormat(BAD_REQUEST_400);
			httpResponse.append("HTTP/1.0 " + BAD_REQUEST_400 + CRLF);
			httpResponse.append("Date: " + date + CRLF);
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
			sendMessageToClient(INTERNAL_SERVER_ERROR_500, createHtmlFormat(INTERNAL_SERVER_ERROR_500));

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

	private void sendMessageToClient(String responseMessage, byte[] pageContent) {

		DataOutputStream writerToClient = null;
		try {

			writerToClient = new DataOutputStream(clientSocket.getOutputStream());
			// printing response header
			System.out.println("Response Header:\n" + responseMessage);

			writerToClient.writeBytes(responseMessage);

			// TODO: chunked transer needs testing
			if (pageContent != null) {
				if (requestOfClient.isChunked) { //in case chunked
					int currentChunkSize = -1;
					int pageContentCounter = 0;
					while (pageContentCounter < pageContent.length) {
						currentChunkSize = new Random().nextInt(15) + 1;
						int fillChunkCounter = 0;
						byte[] chunkedData = new byte[currentChunkSize];
						// loading the chunk
						while (fillChunkCounter < currentChunkSize && pageContentCounter < pageContent.length) {
							chunkedData[fillChunkCounter++] = pageContent[pageContentCounter++];
						}
						// writing the chunk to client
						writerToClient.writeBytes(Integer.toHexString(fillChunkCounter) + CRLF);
						writerToClient.writeBytes(CRLF);
						writerToClient.write(chunkedData, 0, fillChunkCounter);			
					}
					// write ending chunk
					writerToClient.writeBytes(0 + CRLF);
					writerToClient.writeBytes(CRLF);
						
				}
				else {
					writerToClient.write(pageContent);
					writerToClient.flush();					
				}
			}

		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.out.println("Error in writing response to client!");
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
