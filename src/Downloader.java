import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class Downloader implements Runnable {

	private SynchronizedQueue<String> m_DownloadQueue;
	private SynchronizedQueue<HTML> m_AnalyzeQueue;
	// private SynchronousQueue<String> m_DownloadQueue;
	// private SynchronousQueue<HTML> m_AnalyzeQueue;
	private ConfigData m_ConfigData;
	private String m_Domain;
	private SynchronizedQueue<String> m_AllTraversedLinks;
	private int m_Port;
	private CrawlelResultForDomain m_resultCrawlerData;

	public Downloader(CrawlelResultForDomain resultCrawlerDatal, SynchronizedQueue<String> AllTraversedLinks, ConfigData configurationData, SynchronizedQueue<String> m_UrlQueue, SynchronizedQueue<HTML> m_HtmlQueue, String Domain, int port) {
		m_DownloadQueue = m_UrlQueue;
		m_AnalyzeQueue = m_HtmlQueue;
		m_ConfigData = configurationData;
		m_Domain = Domain;
		m_Port = port;
		m_AllTraversedLinks = AllTraversedLinks;
		m_resultCrawlerData = resultCrawlerDatal;
	}

	@Override
	public void run() {
		// while any of the queues has an item the thread still runs
		while (ThreadManager.isRunning) {// m_DownloadQueue.getSize() > 0 ||
											// m_AnalyzeQueue.getSize() > 0) {
											// // TODO: think of a better
											// condition
			// String urlDequeued = m_DownloadQueue.dequeue();
			Socket socketConnection = null;
			ThreadManager.updateStateDownloader(true);
			String urlDequeued = m_DownloadQueue.dequeue();
			if (urlDequeued != null) {
				ThreadManager.updateStateDownloader(false);
				try {
					System.out.println("Downloader dequeued : " + urlDequeued);
					System.out.println("Connecting...");
					System.out.println("domain is :" + m_Domain + " and port is: " + m_Port);
					socketConnection = new Socket(m_Domain, m_Port);

					long startTimeRTT = System.currentTimeMillis();		
					System.out.println("Start time RTT! : " + startTimeRTT);
					PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketConnection.getOutputStream())));
					System.out.println("GET /" + urlDequeued + " HTTP/1.0");
					out.println("GET /" + urlDequeued + " HTTP/1.0");
					out.println();
					out.flush();

					DownloaderReader requestFromWeb = new DownloaderReader(socketConnection);
					synchronized (m_AllTraversedLinks) {
						if (!m_AllTraversedLinks.Exists(urlDequeued)) {						
							m_AllTraversedLinks.enqueue(urlDequeued); // TODO: get unsuccessful urls?
						}
						
					}
					System.out.println(requestFromWeb.Header);
					long readResponseStart = System.currentTimeMillis(); 
					requestFromWeb.readFromSockect();
					long readResponseEnd = System.currentTimeMillis();
					long endTimeRTT = System.currentTimeMillis();
					System.out.println("End time RTT!: "+ endTimeRTT);
					synchronized (m_resultCrawlerData) {
						System.out.println("startRTT:"+ startTimeRTT); //TODO: figure out why this is wrong
						System.out.println("start RTT read:" + readResponseStart);
						System.out.println("end RTT read:" + readResponseEnd);
						System.out.println("endRTT:"  + endTimeRTT);
						long RttTime = (endTimeRTT-startTimeRTT) - (readResponseEnd - readResponseStart);
						m_resultCrawlerData.m_RttAggregator.m_TotalMilliseconds = RttTime ;
						System.out.println("updating RTT with :" + RttTime);
						m_resultCrawlerData.m_RttAggregator.m_TotalAmountOfRTTs++;
					}
					System.out.println("Downloader: read from queue and got\n " + requestFromWeb.Header);
					// m_AnalyzeQueue.enqueue(new
					// HTML(requestFromWeb.Header.toString(),
					// requestFromWeb.Body.toString(),
					// requestFromWeb.ContentType ));
					m_AnalyzeQueue.enqueue(new HTML(requestFromWeb.Header.toString(), requestFromWeb.Body.toString(), requestFromWeb.ContentType));

				} catch (UnknownHostException e) { // TODO: fine tune this
													// exception messages
					System.out.println("Couldnt download " + urlDequeued);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println("Couldnt download " + urlDequeued);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println("Couldn't Download " + urlDequeued);
					System.out.println(e.getMessage());
				} finally {
					if (socketConnection != null) {
						try {
							socketConnection.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out.println("Downloader: couldn't close connection");
							// m_AnalyzeQueue.unregisterProducer();
						}
					}
				}
			}
		}
		System.out.println("Downloader finished!");
	}

	public class DownloaderReader {
		private Socket m_Connection;
		public StringBuilder Header;
		public String ContentType;
		public StringBuilder Body;
		private int m_ContentLength;

		public DownloaderReader(Socket connection) {
			// TODO Auto-generated constructor stub
			m_Connection = connection;
			Header = new StringBuilder();
			Body = new StringBuilder();
		}

		public void readFromSockect() throws Exception {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(m_Connection.getInputStream()));
			String inputMessage;
			String regex = "Content-Length: ([\\d]*)";
			Pattern pat = Pattern.compile(regex);
			System.out.println("reading dequeued URL..");
			// reading Header
			while ((inputMessage = bufferedReader.readLine()) != null) {
				if (inputMessage.equals("")) { // end of header
					break;
				}
				Matcher m = pat.matcher(inputMessage);
				if (m.find()) {
					try {
						m_ContentLength = Integer.parseInt(m.group(1));
					} catch (NumberFormatException e) {
						System.out.println("regex got: " + m.group(1));
						throw new NumberFormatException("Ilegal Content Type value");
					}
				}
				Header.append(inputMessage + "\n");
			}

			// reading Body
			if (m_ContentLength > 0) {
				int readInt;
				while ((readInt = bufferedReader.read()) != -1) {
					Body.append((char) readInt);
					if (Body.length() == m_ContentLength) {
						break;
					}
				}
			}
		}
	}
}
