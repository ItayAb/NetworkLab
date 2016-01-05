import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class WebCrawler {

	public static Object ImageCounterLock = new Object();
	public static Object DocumentCounterLock = new Object();
	public static Object VideoCounterLock = new Object();
	private SynchronizedQueue<String> m_UrlQueue;
	private SynchronizedQueue<HTML> m_HtmlQueue;
//	private SynchronousQueue<String> m_UrlQueue;
//	private SynchronousQueue<HTML> m_HtmlQueue;
	private ConfigData m_ConfigData;
	private Thread[] m_Downloaders;
	private Thread[] m_Analyzers;
	private String m_Domain;
	private boolean m_IsFullTCPPortScan = false;
	private boolean m_IsDisrespectRobot = false;
	private int m_Port;
	//private ArrayList<ArrayList<String>> m_ListOfTraversedLinks;
	private SynchronizedQueue<String> m_ListOfTraversedLinks;
	private HashMap<String, String> m_ParamsForWebCrawler;
	private String m_TimeStamp;
	private String m_NameOfCrawlIteration;
	private String m_HtmlResultPage;
	ThreadManager m_ThreadManager;	
	private HashMap<String, String> m_LinkTextToActualLink;
	private CrawlelResultForDomain m_ResultWrapper;

	public WebCrawler(ConfigData configData, HashMap<String, String> webCrawlerSetting) {
		m_ConfigData = configData;
		m_Analyzers = new Thread[m_ConfigData.getMaxAnalyzers()];
		m_Downloaders = new Thread[m_ConfigData.getMaxDownloaders()];
		m_ListOfTraversedLinks = new SynchronizedQueue<>();
		m_ParamsForWebCrawler = webCrawlerSetting;
		m_UrlQueue = new SynchronizedQueue<>();
		m_HtmlQueue = new SynchronizedQueue<>();		
		m_ThreadManager.initThreadManager(m_UrlQueue, m_HtmlQueue, m_ConfigData.getMaxDownloaders(), m_ConfigData.getMaxAnalyzers());
		initParams();
		if (m_Domain.length() == 0) {
			// TODO: throw exception ?
		}		
	}

	private void initParams() {
		for (String keyInSettingDic : m_ParamsForWebCrawler.keySet()) {
			String variable = keyInSettingDic.toLowerCase();
			if (variable.equals("domain")) {
				m_Domain = m_ParamsForWebCrawler.get(keyInSettingDic);
			} else if (variable.equals("isFullTcpScan")) {
				m_IsFullTCPPortScan = true;
			} else if (variable.equals("isDisrespect")) {
				m_IsDisrespectRobot = true;
			}
		}

	}
	
	public void Run() throws Exception {
		m_UrlQueue.enqueue(""); // TODO: this way I am searching for the default in first iteration
		
		Timestamp time = new Timestamp(System.currentTimeMillis());		
		m_TimeStamp = new SimpleDateFormat("_MM/dd/yyyy_HH-mm-ss").format(time);
		m_NameOfCrawlIteration = m_Domain + m_TimeStamp + ".html";
		initDownloaders();
		initAnalyzers();
		startCrawling();
	}
	
	public void SendResults(){
		// 
		
	}

	private void startCrawling() throws Exception {
		// TODO: update the time stamp m_TimeStamp =

		// TODO Auto-generated method stub
		
		for (int i = 0; i < m_Downloaders.length; i++) {
			m_Downloaders[i].start();
		}
		for (int i = 0; i < m_Analyzers.length; i++) {			
			m_Analyzers[i].start();
		}

		ThreadManager.checkIfEndOfCrawl();
		
		for (int i = 0; i < m_Downloaders.length; i++) {
			try {
				m_Downloaders[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				// TODO : should this exception be special type to easily
				// identfiy on its way up to next 'catch'
				throw new Exception("One of the downloaders couldn't close!");
			}
		}

		for (int i = 0; i < m_Analyzers.length; i++) {
			try {
				m_Analyzers[i].join();
			} catch (Exception e) {
				// TODO: handle exception
				// TODO : should this exception be special type to easily
				// identfiy on its way up to next 'catch'
				throw new Exception("One of the Analyzers couldn't close!");
			}
		}
		System.out.println("All Thread are dead..");
		
		endCrawling();
	}

	private void endCrawling() throws IOException {
		System.out.println("Crawling Ended!!");
		// TODO Auto-generated method stub
		createHtmlResultPage();
		try {
			writePageToFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IOException("Error in writing the resultPage!");
		}
	}

	private void writePageToFile() throws IOException {
		File resultPage = new File(m_ConfigData.getRoot() + File.separator + m_NameOfCrawlIteration);
		resultPage.createNewFile();
		FileWriter writer = null;
		try {
			writer = new FileWriter(resultPage);
			writer.write(m_HtmlResultPage);
		} finally {

			if (writer != null) {
				writer.close();
			}
		}
	}

	private void createHtmlResultPage() {
		// TODO Auto-generated method stub
		StringBuilder htmlTable = new StringBuilder();

		htmlTable.append("<html>\n");
		htmlTable.append("<body>\n");
		htmlTable.append("<table style=\"width:50%\">\n");
//		for (ArrayList<String> listOfStringFromCertainThread : m_ListOfTraversedLinks) {
//			htmlTable.append("<tr>\n");
//			for (String linkInCurretnArrayList : listOfStringFromCertainThread) {
//				htmlTable.append("<td>" + linkInCurretnArrayList + "</td>\n");
//			}
//		}
//		for (String linkInResultList : m_ListOfTraversedLinks) {
//			htmlTable.append("<td>" + linkInResultList + "</td>\n");
//		}
		ArrayList<String> allLinks = m_ListOfTraversedLinks.getAllQueue();
		for (String stringRunner : allLinks) {
			htmlTable.append("<td>" + stringRunner + "</td>\n");
		}
		htmlTable.append("</tr>\n");

		htmlTable.append("</table>\n");
		htmlTable.append("</body>\n");
		htmlTable.append("</html>\n");

		m_HtmlResultPage = htmlTable.toString();
	}
	
	private void initAnalyzers() {
		for (int i = 0; i < m_Analyzers.length; i++) {
			m_Analyzers[i] = new Thread(new Analyzer(m_ListOfTraversedLinks, m_UrlQueue, m_HtmlQueue, m_Domain, m_ResultWrapper));
		}

	}

	private void initDownloaders() {
		// TODO Auto-generated method stub
		for (int i = 0; i < m_Downloaders.length; i++) {
			m_Downloaders[i] = new Thread(new Downloader(m_ConfigData, m_UrlQueue, m_HtmlQueue, m_Domain, 8080));
		}
	}
}
