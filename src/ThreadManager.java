public class ThreadManager {

	public static Boolean isRunning = true;
	public static Object Lock = new Object();
	private static SynchronizedQueue<String> m_Urls;
	private static SynchronizedQueue<HTML> m_Html;
	private static Thread[] m_DownloadersThreadArray;
	private static Thread[] m_AnalyzersThreadArray;
	private static int m_DownloaderWaiting;
	private static int m_AnalyzerWaiting;
	private static int m_MaxDownloader;
	private static int m_MaxAnalyzer;

	public ThreadManager(SynchronizedQueue<String> urlS, SynchronizedQueue<HTML> htmls, int maxDownloader, int maxAnalyzer, Thread[] analyzers, Thread[] downloader) {
		// TODO Auto-generated constructor stub
		m_Urls = urlS;
		m_Html = htmls;
		m_DownloadersThreadArray = downloader;
		m_AnalyzersThreadArray = analyzers;
	}

	public static void initThreadManager(SynchronizedQueue<String> URLS, SynchronizedQueue<HTML> HTML, int maxDownloads, int maxAnalyzers) {
		m_Urls = URLS;
		m_Html = HTML;
		m_MaxDownloader = maxDownloads;
		m_MaxAnalyzer = maxAnalyzers;

	}

	public static void updateStateDownloader(boolean isWait) {
		synchronized (Lock) {
			if (isWait) {

				m_DownloaderWaiting++;
				if (m_DownloaderWaiting == m_MaxDownloader) {
					System.out.println("Downlaoder called this");
					System.out.println("m_AnalyzerWaiting (" + m_AnalyzerWaiting + ")== m_MaxAnalyzer (" + m_MaxAnalyzer + ")");
					System.out.println("m_DownloaderWaiting (" + m_DownloaderWaiting + ")== m_MaxDownloader (" + m_MaxDownloader + ")");
					checkIfEndOfCrawl();
				}
			} else {
				m_DownloaderWaiting--;
			}
		}
	}

	public static void updateStateAnalyzer(boolean isWait) {
		synchronized (Lock) {
			
			if (isWait) {
				m_AnalyzerWaiting++;
				if (m_AnalyzerWaiting == m_MaxAnalyzer) {
					System.out.println("Analyzer called this");
					System.out.println("m_DownloaderWaiting (" + m_DownloaderWaiting + ")== m_MaxDownloader (" + m_MaxDownloader + ")");
					System.out.println("m_AnalyzerWaiting (" + m_AnalyzerWaiting + ")== m_MaxAnalyzer (" + m_MaxAnalyzer + ")");
					checkIfEndOfCrawl();
				}
			} else {
				m_AnalyzerWaiting--;
			}
		}

	}

	public static void checkIfEndOfCrawl() {

		System.out.println("!!! checking end of crawl!!!");
		if (m_Html.getSize() == 0 && m_Urls.getSize() == 0) {
			System.out.println("m_Html.getSize() "+m_Html.getSize()+" && m_Urls.getSize()"+ m_Urls.getSize()+"");
			if (m_AnalyzerWaiting == m_MaxAnalyzer && m_DownloaderWaiting == m_MaxDownloader) {
				System.out.println("!!!!!Crawl need to end!!!!!!!");
				isRunning = false;
				m_Html.WakeAll();
				m_Urls.WakeAll();
				System.out.println("after wake up isRunning is: " + isRunning);
			}
		}

	}
}
