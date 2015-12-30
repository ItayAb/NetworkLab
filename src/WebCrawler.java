

public class WebCrawler {

	private SynchronizedQueue<String> m_UrlQueue;
	private SynchronizedQueue<HTML> m_HtmlQueue;
	private ConfigData m_ConfigData;
	private Thread[] m_Downloaders;
	private Thread[] m_Analyzers;
	private String m_Domain;
	private int m_Port;
	
	public WebCrawler(ConfigData configData)
	{
		m_ConfigData = configData;	
		m_Analyzers = new Thread[m_ConfigData.getMaxAnalyzers()];
		m_Downloaders = new Thread[m_ConfigData.getMaxDownloaders()];		
	}
	
	public void Run()
	{
		initDownloaders();
		initAnalyzers();
		startCrawling();		
	}

	private void startCrawling() {
		// TODO Auto-generated method stub
		
	}

	private void initAnalyzers() {
		for (int i = 0; i < m_Analyzers.length; i++) {
			m_Analyzers[i] = new Thread(new Analyzer(m_UrlQueue, m_HtmlQueue));			
		}
		
	}

	private void initDownloaders() {
		// TODO Auto-generated method stub
		for (int i = 0; i < m_Downloaders.length; i++) {
			m_Downloaders[i] = new Thread(new Downloader(m_ConfigData, m_UrlQueue, m_HtmlQueue, "", 80));
		}
	}
}
