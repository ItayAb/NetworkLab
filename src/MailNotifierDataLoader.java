import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MailNotifierDataLoader {

	private final String NAME_OF_DATA_FILE = "MailNotifierData.ini";
	private String recipient;
	private boolean isLoaded = false;
	private String pathOfData;

	public MailNotifierDataLoader() {
		recipient = null;
		pathOfData = System.getProperty("user.dir");
	}

	public String Recipient(){
		return recipient;
	}
	
	public boolean IsLoaded(){
		return isLoaded;
	}
	
	public void loadData() {
		File mailDataIni = null;
		InputStream in = null;
		InputStreamReader read = null;
		BufferedReader dataReader = null;
		StringBuilder fileContent = new StringBuilder();
		try {
			mailDataIni = new File(pathOfData + File.separator + NAME_OF_DATA_FILE);
			in = new FileInputStream(mailDataIni);
			read = new InputStreamReader(in);
			dataReader = new BufferedReader(read);

			String lineInput;
			while (dataReader.ready()) {
				lineInput = dataReader.readLine();
				fileContent.append(lineInput);
			}
			
			updateData(fileContent);
			
		} catch (Exception e) {
			System.out.println("Error in loading data for the mail notifier!");

		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (read != null) {
					read.close();
				}
				if (dataReader != null) {
					dataReader.close();
				}
			} catch (IOException e) {
				System.out.println("Error in closing resources in Mail Notifier Data loader");
			}
		}
	}

	private void updateData(StringBuilder fileContent) {
		String[] content = fileContent.toString().split("\n");
		String result = null;
		boolean done = false;
		for (int i = 0; i < content.length && !done; i++) {
			if (content[i].contains("=")) {
				done = true;
				String email = content[i].substring(content[i].indexOf("=")+1).trim(); 
				if (!email.isEmpty()) {
					result = email;
				}
			}
		}
		
		if (!isValidEmail(result)) {
			throw new IllegalArgumentException();
		}
		
		if (result != null) {
			isLoaded = true;
		}
		
		recipient = result;
	}
	
	private boolean isValidEmail(String emailToCheck){
		int indexOfWhiteSpace = emailToCheck.indexOf(" ");
		int indexOfShtrudel = emailToCheck.indexOf("@");
		int lastIndexOfDot = emailToCheck.lastIndexOf(".");
		
		if (indexOfWhiteSpace == -1 && indexOfShtrudel < lastIndexOfDot && lastIndexOfDot < emailToCheck.length()) {
			return true;
		}
		else {
			return false;
		}
	}
}
