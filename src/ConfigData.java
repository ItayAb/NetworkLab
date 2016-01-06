import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigData {

	// constants for the config.ini
	private final String CONFIG_NAME = "config.ini";
	private final String PORT = "port";
	private final String ROOT = "root";
	private final String DEFAULT_PAGE = "defaultPage";
	private final String MAX_THREADS = "maxThreads";
	private final String MAX_DOWNLOADERS = "maxDownloaders";
	private final String MAX_ANALYZERS = "maxAnalyzers";
	private final String IMAGE_EXTENSION = "imageExtensions"; 
	private final String VIDEO_EXTENSION = "videoExtensions";
	private final String DOCUMENT_EXTENSION = "documentExtension";

	// variables to hold the data
	private int port;
	private String root;
	private String defaultPage;
	private int maxThreads;
	private int m_MaxDownloads;
	private int m_MaxAnalyzers;
	private String[] m_ImageExtension;
	private String[] m_VideoExtension;
	private String[] m_DocumentExtension;

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

	public int getMaxDownloaders() {
		return m_MaxDownloads;
	}

	public int getMaxAnalyzers() {
		return m_MaxAnalyzers;
	}

	public String[] getImageExtensions() {
		return m_ImageExtension;
	}

	public String[] getVideoExtensions() {
		return m_VideoExtension;
	}

	public String[] getDocumentExtensions() {
		return m_DocumentExtension;
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
		int indexOfEquals = inputTrimmed.indexOf('=');
		// check existence of '='
		if (indexOfEquals != -1) {
			// check existence of data past the '='
			if (inputTrimmed.length() > indexOfEquals) {
				// check port
				if (inputTrimmed.startsWith(PORT)) {
					try {
						value = lineOfInput.substring(indexOfEquals + 1, lineOfInput.length()).trim();
						port = Integer.parseInt(value);
					} catch (Exception e) {
						throw new ExceptionInInitializerError("Error with the port value!");
					}
				}
				// check root
				else if (inputTrimmed.startsWith(ROOT)) {
					value = lineOfInput.substring(indexOfEquals + 1, lineOfInput.length()).trim();
					if (new File(value).exists() && new File(value).isDirectory()) {
						root = value;
					} else {
						throw new ExceptionInInitializerError("Error with the root path!");
					}
				} else if (inputTrimmed.startsWith(DEFAULT_PAGE)) { // TODO: read default without root directory
					value = lineOfInput.substring(indexOfEquals + 1, lineOfInput.length()).trim();
					if (new File(value).exists() && new File(value).isFile()) {
						defaultPage = value;
					} else {
						throw new ExceptionInInitializerError("Error with the default page path!");
					}
				}

				// check max threads
				else if (inputTrimmed.startsWith(MAX_THREADS)) {
					try {
						value = lineOfInput.substring(indexOfEquals + 1, lineOfInput.length()).trim();
						maxThreads = Integer.parseInt(value);
						if (maxThreads < 1) {
							System.out.println("Error in maxThread value! please check the config.ini");
							throw new ExceptionInInitializerError("value must be a whole number and larger than 0");
						}

					} catch (Exception e) {
						throw new ExceptionInInitializerError("Error in maxThread value! please check the config.ini");
					}
				// check Max Downloaders
				} else if (inputTrimmed.startsWith(MAX_DOWNLOADERS)) {
					try {
						value = lineOfInput.substring(indexOfEquals + 1, lineOfInput.length()).trim();
						m_MaxDownloads = Integer.parseInt(value);
						if (m_MaxDownloads < 1) {
							System.out.println("Error in maxThread value! please check the config.ini");
							throw new ExceptionInInitializerError("value must be a whole number and larger than 0");
						}
					} catch (Exception e) {
						throw new ExceptionInInitializerError("Error with the maxDownloaders value!");
					}
				}
				// check max Analyzers
				else if (inputTrimmed.startsWith(MAX_ANALYZERS)) {
					try {
						value = lineOfInput.substring(indexOfEquals + 1, lineOfInput.length()).trim();
						m_MaxAnalyzers = Integer.parseInt(value);
						if (m_MaxAnalyzers < 1) {
							System.out.println("Error in maxThread value! please check the config.ini");
							throw new ExceptionInInitializerError("value must be a whole number and larger than 0");
						}
					} catch (Exception e) {
						throw new ExceptionInInitializerError("Error with the maxAnalyzers value!");
					}
				// check Image Extension
				} else if (inputTrimmed.startsWith(IMAGE_EXTENSION)) {
					value = lineOfInput.substring(indexOfEquals + 1, lineOfInput.length()).trim();
					String[] allImageExtension;
					try {
						allImageExtension = value.split(",");
						for (int i = 0; i < allImageExtension.length; i++) {
							allImageExtension[i] = allImageExtension[i].trim();
						}
						m_ImageExtension = allImageExtension;
					} catch (Exception e) {
						throw new ExceptionInInitializerError("Error with the imageExtension values!");
					}
				// check Video Extension
				} else if (inputTrimmed.startsWith(VIDEO_EXTENSION)) {
					value = lineOfInput.substring(indexOfEquals + 1, lineOfInput.length()).trim();
					String[] allVideoExtension;
					try {
						allVideoExtension = value.split(",");
						for (int i = 0; i < allVideoExtension.length; i++) {
							allVideoExtension[i] = allVideoExtension[i].trim();
						}
						m_VideoExtension = allVideoExtension;
					} catch (Exception e) {
						throw new ExceptionInInitializerError("Error with the videoExtension values!");
					}
				// check Document Extension
				} else if (inputTrimmed.startsWith(DOCUMENT_EXTENSION)) {
					value = lineOfInput.substring(indexOfEquals + 1, lineOfInput.length()).trim();
					String[] allDocumentExtension;
					try {
						allDocumentExtension = value.split(",");
						for (int i = 0; i < allDocumentExtension.length; i++) {
							allDocumentExtension[i] = allDocumentExtension[i].trim();
						}
						m_DocumentExtension = allDocumentExtension;
					} catch (Exception e) {
						throw new ExceptionInInitializerError("Error with the videoExtension values!");
					}
				}

			} else { // if there aren't any values past the '='
				throw new ExceptionInInitializerError("Data in the config.ini is malformed|corrupt");
			}
		} else {// in case there wasn't '='
			throw new ExceptionInInitializerError("Data in the config.ini is malformed|corrupt");
		}

	}
}
