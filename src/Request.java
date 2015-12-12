import java.awt.print.Paper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;

import javax.swing.text.StyledEditorKit.BoldAction;


public class Request {

	private final String CHUNKED = "chunked: yes";
	private final String PARAMS_PAGE_NAME = "params_info.html";
	private final String CONTENT_LENGTH = "Content-Length:";
	private String defaultPageName = "index.html";
	public HttpResponseCode responseCode;
	public RequestType requestType;
	public StringBuilder Header;
	public StringBuilder Body;
	public boolean isChunked;
	public String requestedPage;
	private HashMap<String, String> paramsFromClient;
	private int contentLength;
	private ConfigData serverData;
	
	
	
	public Request(ConfigData data) {
		serverData = data;
		isChunked = false;
		responseCode = null;
		requestType = null;
		Header = new StringBuilder();
		Body = new StringBuilder();
		contentLength = -1;
		paramsFromClient = new HashMap<>();
	}
	
	public void ParseRequest(Socket clientSocket) throws IOException{
		BufferedReader bufferedReader = null;
		String inputMessage = "";
		bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		boolean isFirstLine = true;
		
		// reading Header
		while ((inputMessage = bufferedReader.readLine()) != null) {
			if (isFirstLine) {
				if (!isValidHeader(inputMessage)) {
					responseCode = HttpResponseCode.BAD_REQUEST_400;
				}else {
					extractRequestType(inputMessage);
					extractUrlVariables(inputMessage);
					extractRequestedPage(inputMessage);
				}
				
				isFirstLine = false;
			}
			
			if (inputMessage.equalsIgnoreCase(CHUNKED)) {
				isChunked = true;
			}
			
			if (inputMessage.startsWith(CONTENT_LENGTH)) {
				extractContentLength(inputMessage);
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
		
		// TODO: check cases of bad request
		if (requestType == RequestType.POST && contentLength == -1) {
			responseCode = HttpResponseCode.BAD_REQUEST_400;
		}
		
		if (requestType.equals(RequestType.POST)) { //TODO: are there more requestTypes the have params in body?
			parseParamsFromBody();
		}
		
		if (requestedPage.equals(PARAMS_PAGE_NAME)) {
			initRequestedPage();
		}
		if (responseCode == null) {
			extractResponseCode();
		}
	}
	
	private void extractResponseCode(){
		File requestedPageFile = new File(serverData.getRoot() + File.separator + requestedPage);
		if (requestedPageFile.exists()) {
			responseCode = HttpResponseCode.OK_200;
		}
		else {
			responseCode = HttpResponseCode.NOT_FOUND_404;
		}
	}

	private void parseParamsFromBody(){
		String[] body = Body.toString().split("&");
		
		for (int i = 0; i < body.length; i++) {
			String[] keyToVal = body[i].split("=");
			if (keyToVal.length == 1) {
				paramsFromClient.put(keyToVal[0], "");
			}
			if (keyToVal.length == 2) {
				paramsFromClient.put(keyToVal[0], keyToVal[1]);
			}
		}
		
		
	}
	
	private boolean isValidHeader(String inputMessage) {
		boolean isValid = false;
		String[] headerDivided = inputMessage.split(" ");
		if (headerDivided.length > 2) {
			if (headerDivided[0].length() > 0) {
				if (headerDivided[1].startsWith("/")) {
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
		
		if (pageToReturn.trim().length() == 0) { //if no page was asked then default
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
				for (int i = 0; i < paramatersArray.length; i++) {
					String[] split = paramatersArray[i].split("=");
					if (split.length == 2) {
						paramsFromClient.put(split[0], split[1]);
					} else if (split.length == 1) { // TODO: if there is a case of "x= "
													
						paramsFromClient.put(split[0], "");
					}
				}
			}
		}
	}

	private void extractContentLength(String lineOfInput){
		int indexOfColon = lineOfInput.indexOf(":") + 1;
		String length = lineOfInput.substring(indexOfColon).trim();
		contentLength = Integer.parseInt(length);
	}
	
	private void extractRequestType(String firstInputLine){
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
	
	
	private void initRequestedPage() throws IOException{
		StringBuilder htmlTable = new StringBuilder();
		
		htmlTable.append("<html>\n");
		htmlTable.append("<body>\n");
		htmlTable.append("<table style=\"width:10%\">\n");
		for (String variableName : paramsFromClient.keySet()) {
			htmlTable.append("<tr>\n");
			htmlTable.append("<td>" + variableName + "</td>\n");
			htmlTable.append("<td>" + paramsFromClient.get(variableName) + "</td>\n");
			htmlTable.append("</tr>\n");
		}
		htmlTable.append("</table>\n");
		htmlTable.append("</body\n");
		htmlTable.append("</html>\n");
		
		writeHtmlOfParams(htmlTable.toString());
	}
	
	private void writeHtmlOfParams(String htmlPageContent) throws IOException{
		File requestedPageFile = new File(serverData.getRoot() + File.separator + requestedPage);
		FileWriter writer = null;
		try {
			writer = new FileWriter(requestedPageFile);
			writer.write(htmlPageContent);
		} finally{
			
			if (writer != null) {
				writer.close();
			}
		}
		
		
	}
}
