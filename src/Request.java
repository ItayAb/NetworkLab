import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;

import javax.mail.internet.ContentType;

public class Request {

	private final String CHUNKED = "chunked: yes";
	private final String PARAMS_PAGE_NAME = "params_info.html";
	private final String CONTENT_LENGTH = "Content-Length:";
	private final String CONTENT_TYPE = "Content-Type:";
	private String defaultPageName = "index.html";
	public HttpResponseCode responseCode;
	public RequestType requestType;
	public StringBuilder Header;
	public StringBuilder Body;
	public boolean isChunked;
	public String requestedPage;
	public File requestedFile;
	public String m_TypeContent;
	public HashMap<String, String> paramsFromClient;
	private int contentLength;
	public ConfigData serverData;  // TODO: made public so web crawler can get it from HttpRequest, need to think of 
	// a better way

	public Request(ConfigData data) {
		requestedFile = null;
		serverData = data;
		isChunked = false;
		responseCode = null;
		requestType = null;
		Header = new StringBuilder();
		Body = new StringBuilder();
		contentLength = -1;
		paramsFromClient = new HashMap<>();
	}

	public void ParseRequest(Socket clientSocket) throws Exception {
		String inputMessage = "";
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		boolean isFirstLine = true;
		boolean isValidHeader = false;
		// reading Header
		while ((inputMessage = bufferedReader.readLine()) != null) {
			if (isFirstLine) {
				isValidHeader = isValidHeader(inputMessage);
				if (!isValidHeader) {
					responseCode = HttpResponseCode.BAD_REQUEST_400;
					break;
				} else {
					extractRequestType(inputMessage);
					extractUrlVariables(inputMessage);
					extractRequestedPage(inputMessage);
				}

				isFirstLine = false;
			}

			if (inputMessage.equalsIgnoreCase(CHUNKED)) {
				isChunked = true;
			}

			if (inputMessage.startsWith(CONTENT_TYPE)) {
				extractContentType(inputMessage);
			}
			
			if (inputMessage.startsWith(CONTENT_LENGTH)) {
				extractContentLength(inputMessage);
				if (responseCode == HttpResponseCode.BAD_REQUEST_400) {
					isValidHeader = false;
					break;
				}
			}

			if (inputMessage.equals("")) { // end of header
				break;
			}

			Header.append(inputMessage + "\n");
		}

		// reading Body
		if (contentLength > 0) {
			int readInt;
			while ((readInt = bufferedReader.read()) != -1) {
				Body.append((char) readInt);
				if (Body.length() == contentLength) {
					break;
				}
			}
		}

		if (isValidHeader) {
			if (requestType == RequestType.POST) {
				parseParamsFromBody();
			}

			if (requestedPage.equals(PARAMS_PAGE_NAME)) {
				initParamsInfoHtml();
			}
			if (responseCode == null) {
				updateResponseCode();
			}
		} else {
			responseCode = HttpResponseCode.BAD_REQUEST_400;
		}
	}

	private void extractContentType(String inputLine) throws Exception {
		int indexOfSemiColon = inputLine.indexOf(":");
		String typeOfContent = "";
		if (indexOfSemiColon == -1) {
			 typeOfContent = inputLine.substring(inputLine.indexOf(CONTENT_TYPE) + 1);
		}
		else {
			if (((indexOfSemiColon - 1) - (inputLine.indexOf(CONTENT_TYPE)+  1)) < 0  ) { // TODO: check the extraction is correct
				throw new Exception("Couldn't get content type from: " + inputLine);
			}
			typeOfContent = inputLine.substring(inputLine.indexOf(CONTENT_TYPE) + 1, indexOfSemiColon - 1);
		}
		m_TypeContent = typeOfContent;
		
	}

	private void updateResponseCode() {
		requestedFile = new File(serverData.getRoot() + File.separator + requestedPage);
		if (requestedFile.exists()) {
			responseCode = HttpResponseCode.OK_200;
		} else {
			responseCode = HttpResponseCode.NOT_FOUND_404;
		}
	}

	private void parseParamsFromBody() {
		String[] body = Body.toString().split("&");

		for (int i = 0; i < body.length; i++) {
			String[] keyToVal = body[i].split("=");
			if (keyToVal.length == 1) {
				paramsFromClient.put(keyToVal[0], "");
			}
			if (keyToVal.length == 2) { // there is key value data
				paramsFromClient.put(keyToVal[0], keyToVal[1].replace('+', ' '));
			}
		}

	}

	private boolean isValidHeader(String inputMessage) {
		boolean isValid = false;
		String[] headerDivided = inputMessage.split(" ");
		if (headerDivided.length > 2) {
			if (headerDivided[0].length() > 0) {
				if (headerDivided[1].startsWith("/") && !(headerDivided[1].contains("/%20"))) {
					if (headerDivided[2].startsWith("HTTP/1.0") || headerDivided[2].startsWith("HTTP/1.1")) {

						isValid = true;
					}
				}
			}
		}

		return isValid;
	}

	private void extractRequestedPage(String inputMessage) {
		String pageToReturn = null;
		String substringHeader = inputMessage.substring(inputMessage.indexOf('/') + 1, inputMessage.indexOf("HTTP"));
		int indexOfQuestion = substringHeader.indexOf('?');
		if (indexOfQuestion != -1) {
			pageToReturn = substringHeader.substring(0, indexOfQuestion);
		} else {
			pageToReturn = substringHeader;
		}

		if (pageToReturn.trim().length() == 0) { // if no page was asked then
													// default
			pageToReturn = defaultPageName;
		}

		pageToReturn = pageToReturn.replaceAll("../", "");

		requestedPage = pageToReturn.trim();
	}

	private void extractUrlVariables(String inputMessage) {
		int indexOfQuestionMark = inputMessage.indexOf('?');
		int indexOfLastWhitespace = inputMessage.trim().lastIndexOf(" ");
		if (indexOfQuestionMark != -1) {
			if (indexOfLastWhitespace != -1) {
				String paramaters = inputMessage.substring(indexOfQuestionMark + 1, indexOfLastWhitespace).trim();
				String[] paramatersArray = paramaters.split("&");
				for (int i = 0; i < paramatersArray.length; i++) { // run on key
																	// value
					String[] split = paramatersArray[i].split("=");
					if (split.length == 2) { // there is key and value data
						paramsFromClient.put(split[0], split[1]);

					} else if (split.length == 1) {
						paramsFromClient.put(split[0], "");
					}
				}
			}
		}
	}

	private void extractContentLength(String lineOfInput) {
		try {
			int indexOfColon = lineOfInput.indexOf(":") + 1;
			String length = lineOfInput.substring(indexOfColon).trim();
			contentLength = Integer.parseInt(length);
		} catch (Exception e) {
			System.out.println("Error, corrupt content-length in header");
			responseCode = HttpResponseCode.BAD_REQUEST_400;
		}
	}

	private void extractRequestType(String firstInputLine) {
		for (RequestType runnerRequestType : RequestType.values()) {
			if (firstInputLine.startsWith(runnerRequestType.toString())) {
				requestType = runnerRequestType;
				break;
			}
		}

		if (requestType == null) {
			responseCode = HttpResponseCode.NOT_IMPLEMENTED_501;

		}
	}

	private void initParamsInfoHtml() throws IOException {
		StringBuilder htmlTable = new StringBuilder();

		htmlTable.append("<html>\n");
		htmlTable.append("<body>\n");
		htmlTable.append("<table style=\"width:50%\">\n");
		for (String variableName : paramsFromClient.keySet()) {
			htmlTable.append("<tr>\n");
			htmlTable.append("<td>" + variableName + "</td>\n");
			htmlTable.append("<td>" + paramsFromClient.get(variableName) + "</td>\n");
			htmlTable.append("</tr>\n");
		}

		htmlTable.append("</table>\n");
		htmlTable.append("</body>\n");
		htmlTable.append("</html>\n");

		writeHtmlOfParams(htmlTable.toString());
	}

	private void writeHtmlOfParams(String htmlPageContent) throws IOException {
		File requestedPageFile = new File(serverData.getRoot() + File.separator + requestedPage);
		FileWriter writer = null;
		try {
			writer = new FileWriter(requestedPageFile);
			writer.write(htmlPageContent);
		} finally {

			if (writer != null) {
				writer.close();
			}
		}
	}
}
