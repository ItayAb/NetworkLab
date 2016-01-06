import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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
	private SynchronizedQueue<Integer> m_PortScannerList;
	private boolean m_IsStarting;

	public WebCrawler(ConfigData configData, HashMap<String, String> webCrawlerSetting) {
		m_Port = 8080; // TODO: change to 80
		m_ConfigData = configData;
		m_Analyzers = new Thread[m_ConfigData.getMaxAnalyzers()];
		m_Downloaders = new Thread[m_ConfigData.getMaxDownloaders()];
		m_ListOfTraversedLinks = new SynchronizedQueue<>();
		m_ParamsForWebCrawler = webCrawlerSetting;
		m_UrlQueue = new SynchronizedQueue<>();
		m_HtmlQueue = new SynchronizedQueue<>();		
		m_ThreadManager.initThreadManager(m_UrlQueue, m_HtmlQueue, m_ConfigData.getMaxDownloaders(), m_ConfigData.getMaxAnalyzers());
		initParams();
		m_IsStarting = false;
		m_PortScannerList = new SynchronizedQueue<>();
		if (m_Domain.length() == 0) {
			// TODO: throw exception ?
		}		
	}

	private void initParams() {		
		for (String keyInSettingDic : m_ParamsForWebCrawler.keySet()) {
			String variable = keyInSettingDic.toLowerCase();
			System.out.println("Key: " + variable + " Value: " + m_ParamsForWebCrawler.get(keyInSettingDic));
			if (variable.equals("domain")) {
				m_Domain = m_ParamsForWebCrawler.get(keyInSettingDic);
			} else if (variable.equals("isfulltcpscan")) {
				m_IsFullTCPPortScan = true;
			} else if (variable.equals("isdisrespect")) {
				m_IsDisrespectRobot = true;
			}
		}

	}
	
	public void Run() throws Exception {
		try {
			checkIfDomainValid();			
		} catch (Exception e) {
			// TODO: handle exception
			//TODO: return failure message
			
		}
		if (m_IsStarting) {
			System.out.println("Domain is Valid");
			System.out.println("Full TCP SCAN: " + m_IsFullTCPPortScan);
			if (m_IsFullTCPPortScan) {
				performPortScanning();			
			}
			m_UrlQueue.enqueue(""); // TODO: this way I am searching for the default in first iteration
			
			Timestamp time = new Timestamp(System.currentTimeMillis());		
			m_TimeStamp = new SimpleDateFormat("_MM.dd.yyyy_HH-mm-ss").format(time);
			m_NameOfCrawlIteration = m_Domain + m_TimeStamp + ".html";
			initDownloaders();
			initAnalyzers();
			startCrawling();
			endCrawling();		
		}
	}
	
	private void performPortScanning() {
		// TODO Auto-generated method stub
		System.out.println("Started Port Scanning..");
		int numOfThread = 10;
		SynchronizedQueue<PortScanner> threadPoolForPortCheckers = new SynchronizedQueue<>();
		for (int i = 0; i < numOfThread; i++) {
			threadPoolForPortCheckers.enqueue(new PortScanner(threadPoolForPortCheckers, m_Domain, m_PortScannerList));
		}
		
		for (int i = 8000; i < 8100; i++) { // TODO: choose a reasonable range
			PortScanner portChecker = threadPoolForPortCheckers.dequeue();
			portChecker.setPortToCheck(i);
			portChecker.start();
			System.out.println("Checking port " + i);
		}		
		System.out.println("Port Scan Complete!");
	}

	private void checkIfDomainValid() {
		// TODO Auto-generated method stub
		Socket testDomaint = new Socket();
		try {
			testDomaint.connect(new InetSocketAddress(m_Domain, m_Port), 1000);
			if (testDomaint.isConnected()) {
				m_IsStarting = true;			
			}			
		} catch (Exception e) {
			// TODO: handle exception
			throw new ExceptionInInitializerError("Couldn't connect to " + m_Domain + " on port " + m_Port);
		}
		finally{
			if (!testDomaint.isClosed()) {
				try {
					testDomaint.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Couldn't close the socket that check validity of domain");
				}
			}
		}
		
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

		//ThreadManager.checkIfEndOfCrawl();
		
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
		File resultPage = new File(m_ConfigData.getRoot() + File.separator + "debug", m_NameOfCrawlIteration); // TODO: delete debug
		System.out.println("Attempting to create " + m_ConfigData.getRoot() + File.separator + m_NameOfCrawlIteration);
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
		if (m_IsFullTCPPortScan) {
			htmlTable.append("<td>" + "Ports" + "</td>\n");
			for (Integer portScanned : m_PortScannerList.getAllQueue()) {
				htmlTable.append("<td>" + portScanned.intValue() + "</td>\n");
			}
		}
		htmlTable.append("</tr>\n");

		htmlTable.append("</table>\n");
		htmlTable.append("</body>\n");
		htmlTable.append("</html>\n");

		m_HtmlResultPage = htmlTable.toString();
	}
	
	private void initAnalyzers() {
		for (int i = 0; i < m_Analyzers.length; i++) {
			m_Analyzers[i] = new Thread(new Analyzer(m_ConfigData, m_ListOfTraversedLinks, m_UrlQueue, m_HtmlQueue, m_Domain, m_ResultWrapper));
		}

	}

	private void initDownloaders() {
		// TODO Auto-generated method stub
		for (int i = 0; i < m_Downloaders.length; i++) {
			m_Downloaders[i] = new Thread(new Downloader(m_ListOfTraversedLinks, m_ConfigData, m_UrlQueue, m_HtmlQueue, m_Domain, 8080));
		}
	}
}
