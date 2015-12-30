import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class Downloader implements Runnable {

	private SynchronizedQueue<String> m_DownloadQueue;
	private SynchronizedQueue<HTML> m_AnalyzeQueue;
	private ConfigData m_ConfigData;
	private String m_Domain;
	private int m_Port;

	public Downloader(ConfigData configurationData, SynchronizedQueue<String> downloadQueue, SynchronizedQueue<HTML> analyzeQueue, String Domain, int port) {
		m_DownloadQueue = downloadQueue;
		m_AnalyzeQueue = analyzeQueue;
		m_ConfigData = configurationData;
		m_Domain = Domain;
		m_Port = port;

	}

	@Override
	public void run() {
		// while any of the queues has an item the thread still runs
		while (m_DownloadQueue.getSize() > 0 || m_AnalyzeQueue.getSize() > 0) {
			String urlDequeued = m_DownloadQueue.dequeue();
			Socket socketConnection = null;
			if (urlDequeued != null) {
				try {
					socketConnection = new Socket(m_Domain, m_Port);
					PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketConnection.getOutputStream())));
					out.println("GET /index.html HTTP/1.0");
					out.println();
					out.flush();
					Request requestFromWeb = new Request(m_ConfigData);
					requestFromWeb.ParseRequest(socketConnection);
					String header = requestFromWeb.Header.toString();
					String body = requestFromWeb.Body.toString();
					String contentType = requestFromWeb.m_TypeContent;
					m_AnalyzeQueue.enqueue(new HTML(header, body, contentType));

				} catch (UnknownHostException e) { //TODO: fine tune this exception messages
					System.out.println("Couldnt download " + urlDequeued);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println("Couldnt download " + urlDequeued);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println("Couldn't Download " + urlDequeued);
				} finally {
					if (socketConnection != null) {
						try {
							socketConnection.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out.println("Downloader: couldn't close connection");
						}
					}

				}
			}
		}

	}

}
