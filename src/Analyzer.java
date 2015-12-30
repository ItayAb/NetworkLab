import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Analyzer implements Runnable{

	private final String IMAGE = "image";
	private final String VIDEO = "video";
	private final String DOCUMENT = "document"; //TODO: check exactly what is the prefix for this
	private SynchronizedQueue<String> m_DownloadQueue;
	private SynchronizedQueue<HTML> m_AnalyzeQueue;
	
	
	public Analyzer(SynchronizedQueue<String> downloadQueue, SynchronizedQueue<HTML> analyzeQueue) {
		m_DownloadQueue = downloadQueue;
		m_AnalyzeQueue = analyzeQueue;
	}

	@Override
	public void run() {
		m_DownloadQueue.registerProducer();
		
		while (m_DownloadQueue.getSize() !=0 || m_AnalyzeQueue.getSize() != 0) { // TODO: think about this condition 
			HTML dequeuedHtml = m_AnalyzeQueue.dequeue();
			if (dequeuedHtml != null) {
				String contentTypeOfHtml = dequeuedHtml.GetContentType().toLowerCase();
				if (contentTypeOfHtml.contains(IMAGE)) { // in case of image
					
				}				
				else if (contentTypeOfHtml.contains(VIDEO)) {
					
				}
				else if (contentTypeOfHtml.contains(DOCUMENT)) {
					
				}
				else if (contentTypeOfHtml.contains("text")) {
					String regex ="<a href=\"(.*?)\">";
					Pattern pattern = Pattern.compile(regex);
					Matcher m = pattern.matcher(dequeuedHtml.GetBody());				
					while (m.find()) {
						System.out.println("enqueue to Downloader: " + m.group(1));
						m_DownloadQueue.enqueue(m.group(1));
					}
				}
				else {
					System.out.println("Downloader: Error in content type of " + dequeuedHtml.toString()  );
				}
				
			}
		}
		
		m_DownloadQueue.unregisterProducer();
		
	}
	
}
