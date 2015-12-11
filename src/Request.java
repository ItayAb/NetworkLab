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
	private final String CONTENT_LENGTH = "Content-Length:";
	public HttpResponseCode responseCode;
	public RequestType requestType;
	public StringBuilder Header;
	public StringBuilder Body;
	public boolean isChunked;
	public String requestedPage;
	private HashMap<String, String> paramsFromClient;
	private int contentLength;
	
	
	
	public Request() {
		isChunked = false;
		responseCode = null;
		requestType = null;
		Header = new StringBuilder();
		Body = new StringBuilder();
		contentLength = 0;
		paramsFromClient = new HashMap<>();
	}
	
	public void ParseRequest(Socket clientSocket) throws IOException{
		BufferedReader bufferedReader = null;
		String inputMessage = "";
		bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		boolean isFirstLine = true;
		
		// reading Body
		while ((inputMessage = bufferedReader.readLine()) != null) {
			// TODO: check about readline
			if (isFirstLine) {
				if (isValidHeader(inputMessage)) {
					responseCode = HttpResponseCode.BAD_REQUEST_400;
				}
				extractRequestType(inputMessage);
				extractUrlVariables(inputMessage);
				extractRequestedPage(inputMessage);
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
		
		if (requestType.equals(RequestType.POST)) {
			parseParamsFromBody();
		}
	}

	private void parseParamsFromBody(){
		
		
		
	}
	
	private boolean isValidHeader(String inputMessage) {
		int indexOfFirstWhitespace = inputMessage.indexOf(" ");
		int indexOfSecondWhitespace = inputMessage.substring(indexOfFirstWhitespace + 1).indexOf(" ");
		boolean isContainHttp = inputMessage.endsWith("/HTTP/1.0") || inputMessage.endsWith("/HTTP/1.1");
		
		boolean isExistTwoWhitespace = (indexOfFirstWhitespace != 1 && indexOfSecondWhitespace != -1) ? true : false;
		
		return (isContainHttp && isExistTwoWhitespace);
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

		if (pageToReturn.contains("../")) { // TODO: check if there are several occurrences
			int indexOfForbidden = pageToReturn.indexOf("../");
			pageToReturn = pageToReturn.substring(0, indexOfForbidden - 1)
					+ pageToReturn.substring(indexOfForbidden + 2);
		}
		
		requestedPage = pageToReturn;	
	}

	private void extractUrlVariables(String inputMessage) {
		int startParamaters = inputMessage.indexOf('?') + 1;
		int lastParamaters = inputMessage.indexOf(" ", startParamaters - 1);
		if (startParamaters != -1) {
			if (lastParamaters != -1) {
				String paramaters = inputMessage.substring(startParamaters, lastParamaters).trim();
				String[] paramatersArray = paramaters.split("&");
				for (int i = 0; i < paramatersArray.length; i++) {
					String[] split = paramatersArray[i].split("=");
					if (split.length == 2) {
						paramsFromClient.put(split[0], split[1]);
					} else if (split.length == 1) { // TODO: if there is a case
													// of "x= "
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
			
			if (requestType == null) {
				responseCode = HttpResponseCode.NOT_IMPLEMENTED_501;
			}
		}
	}
	
	public boolean isValidHeader(){
		boolean isValid = false;
		if (requestType != null) {
			if (isValid) {
				
			}
		}
		
		return isValid;
	}
	
	public String initRequestedPage(){
		StringBuilder htmlTable = new StringBuilder();
		StringBuilder correspondingValuesToKeys = new StringBuilder();
		htmlTable.append("<html>");
		htmlTable.append("<body>");
		htmlTable.append("<table style=\"width:100%\">");
		htmlTable.append("<tr>");
		for (String variableName : paramsFromClient.keySet()) {
			htmlTable.append("<td>" + variableName + "</td>");
			correspondingValuesToKeys.append(paramsFromClient.get(variableName) + "\n");
		}
		htmlTable.append("</tr>");
		htmlTable.append("<tr>");
		String[] correspondingAsArray = correspondingValuesToKeys.toString().split("\n");
		for (int i = 0; i < correspondingAsArray.length; i++) {
			htmlTable.append("<td>" + correspondingAsArray[i] + "</td>");
		}
		htmlTable.append("</tr>");
		htmlTable.append("</table>");
		htmlTable.append("</body");
		htmlTable.append("</html>");
		
		return htmlTable.toString();
	}
	
	private void writeHtmlOfParams(String htmlPageContent, File paramsPage) throws IOException{
		FileWriter writer = new FileWriter(paramsPage);
		
		writer.write(htmlPageContent);
		
	}
}
