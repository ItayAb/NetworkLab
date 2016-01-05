import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Analyzer implements Runnable {

	private final String IMAGE = "image";
	private final String VIDEO = "video";
	private final String DOCUMENT = "document"; // TODO: check exactly what is
												// the prefix for this
	private SynchronizedQueue<String> m_DownloadQueue;
	private SynchronizedQueue<HTML> m_AnalyzeQueue;
//	private SynchronousQueue<String> m_DownloadQueue;
//	private SynchronousQueue<HTML> m_AnalyzeQueue;
	private SynchronizedQueue<String> m_AllTraversedLinks;
	private String m_Domain;
	private CrawlelResultForDomain m_ResultWrapper;

	public Analyzer(SynchronizedQueue<String> TraversedListForThisThread, SynchronizedQueue<String> m_UrlQueue, SynchronizedQueue<HTML> m_HtmlQueue, String domain, CrawlelResultForDomain resultWrapper) {
		m_DownloadQueue = m_UrlQueue;
		m_AnalyzeQueue = m_HtmlQueue;
		m_AllTraversedLinks = TraversedListForThisThread;
		//m_DownloadQueue.registerProducer();
		m_Domain = domain;
		m_ResultWrapper = resultWrapper;
	}

	@Override
	public void run() {

		while (ThreadManager.isRunning) {// m_DownloadQueue.getSize() !=0 ||
						// m_AnalyzeQueue.getSize() != 0) { // TODO: think about
						// this condition
			//HTML dequeuedHtml = m_AnalyzeQueue.dequeue();
			ThreadManager.updateStateAnalyzer(true);
			HTML dequeuedHtml = m_AnalyzeQueue.dequeue();
			if (dequeuedHtml != null) {
				
				ThreadManager.updateStateAnalyzer(false);
				System.out.println("Analyzer: dequeued: \n Header:\n" + dequeuedHtml.GetHeader());
				try {
					String contentTypeOfHtml = getContentTypeFromHeader(dequeuedHtml.GetHeader());// dequeuedHtml.GetContentType().toLowerCase();
					if (contentTypeOfHtml.contains(IMAGE)) { // in case of image
						// handle case of image
						System.out.println("Analzyer: it is an image");
						//TODO: check extension supported
						handleCaseOfImage(dequeuedHtml);	
						
					} else if (contentTypeOfHtml.contains(VIDEO)) { // in case
						System.out.println("Analzyer: it is an video");											// of Video
						// handle case of video
					} else if (contentTypeOfHtml.contains(DOCUMENT)) { // in
																		// case
					System.out.println("Analzyer: it is an document");											// 											// of
																		// Document
						// handle case of document
					} else if (contentTypeOfHtml.contains("text")) {
						System.out.println("content is text");
						getImgLinks(dequeuedHtml);
						getUrlLinks(dequeuedHtml); // TODO: can links have HTTP:// ?
					} else {
						System.out.println("Downloader: Error in content type of " + dequeuedHtml.toString());
					}
				} catch (Exception e) {
					System.out.println("Error couldnt find content type");
				}
			}
		}
		System.out.println("Analyzer Finished!");
		// m_DownloadQueue.unregisterProducer();

	}

	private void handleCaseOfImage(HTML dequeuedHtml) {
		// TODO Auto-generated method stub
		int ContentLength = -1;
		String regex = "Content-Length:\\s+([\\d]*)";
		Pattern pat = Pattern.compile(regex);
		Matcher m = pat.matcher(dequeuedHtml.GetHeader());
		if (m.find()) {
			try {
				ContentLength = Integer.parseInt(m.group(1));
				System.out.println("content length is: " + ContentLength);
				// TODO: saveSomewhere
				synchronized (m_ResultWrapper.m_ImageAggregator) {
					m_ResultWrapper.m_ImageAggregator.m_NumOfImages++;
					m_ResultWrapper.m_ImageAggregator.m_TotalSizeInBytes += ContentLength;
				}
			} catch (NumberFormatException e) {
				// TODO: handle exception
				System.out.println("Could not parse Content-Lenght header is malformed\n" + dequeuedHtml.GetHeader());
			}
		}
	}
	
	private void getImgLinks(HTML dequeuedHtml) {
		// TODO Auto-generated method stub
		String regex = "<(img|IMG)\\s+(src|SRC)=\"(.*?)\"";
		Pattern pattern = Pattern.compile(regex);
		Matcher m = pattern.matcher(dequeuedHtml.GetBody());
		System.out.println("fetching  image links");
		while (m.find()) {
			String link = m.group(3);
			System.out.println("enqueue to Downloader: " + link);
			if (!m_AllTraversedLinks.Exists(link)) {
//				m_DownloadQueue.enqueue(link);
				m_DownloadQueue.enqueue(link);
				m_AllTraversedLinks.enqueue(link);
				System.out.println("Analyzer: adding image link: " + link);
			}
		}
		System.out.println("Done with fetching image links");		
	}

	private void getUrlLinks(HTML dequeuedHtml){
		//String regex = "<(a|A) (href|HREF)=\"(http|HTTP)://" + m_Domain + "/(.*?)\">";
		String regex = "<(a|A)\\s+(href|HREF)=\"(?!#)(.*?)\">";
		Pattern pattern = Pattern.compile(regex);
		Matcher m = pattern.matcher(dequeuedHtml.GetBody());
		System.out.println("fetching links");
		while (m.find()) {
			String link = m.group(3);
			System.out.println("enqueue to Downloader: " + link);
			if (!m_AllTraversedLinks.Exists(link)) {
//				m_DownloadQueue.enqueue(link);
				m_DownloadQueue.enqueue(link);
				m_AllTraversedLinks.enqueue(link);
				System.out.println("Analyzer: adding link: " + link);
			}
		}
		System.out.println("Done with fetching links");	
	}
	
	private String getContentTypeFromHeader(String header) throws Exception {
		String contentType = "";
		String regex = "Content-Type: ([A-Za-z/]*)";
		Pattern pat = Pattern.compile(regex);
		Matcher m = pat.matcher(header);
		if (m.find()) {
			contentType = m.group(1);
			System.out.println("found : " + m.group(1));
		}

		return contentType;
	}

}
