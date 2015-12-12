import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

// TODO: ask if content of config.ini has "root" or could have "Root"?

public class ConfigData {

	// constants for the config.ini
	private final String CONFIG_NAME = "config.ini";
	private final String PORT = "port";
	private final String ROOT = "root";
	private final String DEFAULT_PAGE = "defaultPage";
	private final String MAX_THREADS = "maxThreads";
	private final int DEFAUL_NUM_OF_THREADS = 1;

	// variables to hold the data
	private int port;
	private String root;
	private String defaultPage;
	private int maxThreads;

	// dynamically find the Config.ini path
	private String pathOfConfig;

	public ConfigData() {
		pathOfConfig = System.getProperty("user.dir");
	}

	public int getPort() {
		return port;
	}

	public String getRoot() {
		return root;
	}

	public String getDefaultPage() {
		return defaultPage;
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	public void Load() throws Exception {

		File configIniFile = null;
		InputStream in = null;
		InputStreamReader read = null;
		BufferedReader dataReader = null;
		try {
			configIniFile = new File(pathOfConfig + File.separator + CONFIG_NAME);
			in = new FileInputStream(configIniFile);
			read = new InputStreamReader(in);
			dataReader = new BufferedReader(read);

			String lineInput;
			while (dataReader.ready()) {
				lineInput = dataReader.readLine();
				parserInputLine(lineInput);
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (read != null) {
				read.close();
			}
			if (dataReader != null) {
				dataReader.close();
			}
		}
	}

	private void parserInputLine(String lineOfInput) throws Exception {
		String value;
		String inputTrimmed = lineOfInput.trim();
		int indexOfEqauls = inputTrimmed.indexOf('=');
		// check existence of '='
		if (indexOfEqauls != -1) {
			// check existence of data past the '='
			if (inputTrimmed.length() > indexOfEqauls) {
				// check port
				if (inputTrimmed.startsWith(PORT)) {
					value = lineOfInput.substring(indexOfEqauls + 1, lineOfInput.length()).trim();
					port = Integer.parseInt(value);
				}
				// check root
				else if (inputTrimmed.startsWith(ROOT)) {
					value = lineOfInput.substring(indexOfEqauls + 1, lineOfInput.length()).trim();
					if (new File(value).exists() && new File(value).isDirectory()) {
						root = value;
					} else {
						throw new Exception("Error with the root path!");
					}
				}
				// check default page TODO: check if file is .html
				else if (inputTrimmed.startsWith(DEFAULT_PAGE)) {
					value = lineOfInput.substring(indexOfEqauls + 1, lineOfInput.length()).trim();
					if (new File(value).exists() && new File(value).isFile()) {
						defaultPage = value;
					} else {
						throw new Exception("Error with the default page path!");
					}
				}

				// check max threads
				else if (inputTrimmed.startsWith(MAX_THREADS)) {
					value = lineOfInput.substring(indexOfEqauls + 1, lineOfInput.length()).trim();
					maxThreads = Integer.parseInt(value);
					if(maxThreads <= 0) {
						maxThreads = DEFAUL_NUM_OF_THREADS;
					}
				}

			} else { // if there aren't any values past the '='
				throw new Exception("Data in the config.ini is malformed|corrupt");
			}
		} else {// in case there wasn't '='
			throw new Exception("Data in the config.ini is malformed|corrupt");
		}

	}
}
