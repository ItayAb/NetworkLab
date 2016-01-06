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
	// private SynchronousQueue<String> m_DownloadQueue;
	// private SynchronousQueue<HTML> m_AnalyzeQueue;
	private SynchronizedQueue<String> m_AllTraversedLinks;
	private String m_Domain;
	private CrawlelResultForDomain m_ResultWrapper;
	private ConfigData m_ConfigData;

	public Analyzer(ConfigData configData, SynchronizedQueue<String> AllTraversedLinks, SynchronizedQueue<String> m_UrlQueue, SynchronizedQueue<HTML> m_HtmlQueue, String domain,
			CrawlelResultForDomain resultWrapper) {
		m_DownloadQueue = m_UrlQueue;
		m_AnalyzeQueue = m_HtmlQueue;
		// m_DownloadQueue.registerProducer();
		m_Domain = domain;
		m_ResultWrapper = resultWrapper;
		m_AllTraversedLinks = AllTraversedLinks;
		m_ConfigData = configData;
	}

	@Override
	public void run() {

		while (ThreadManager.isRunning) {// m_DownloadQueue.getSize() !=0 ||
			// m_AnalyzeQueue.getSize() != 0) { // TODO: think about
			// this condition
			// HTML dequeuedHtml = m_AnalyzeQueue.dequeue();
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
						// TODO: check extension supported
						// handleCaseOfImage(dequeuedHtml);
						HandleHTML(dequeuedHtml, HTML.TypeOfHTML.IMAGE);

					} else if (contentTypeOfHtml.contains(VIDEO)) { // in case
						System.out.println("Analzyer: it is an video"); // of
																		// Video
						// handle case of video
					} else if (contentTypeOfHtml.contains(DOCUMENT)) { // in
						// TODO: what is the command for document? <a href? <doc
						// src=? // case
						System.out.println("Analzyer: it is an document"); // //
																			// of
						// Document
						// handle case of document
					} else if (contentTypeOfHtml.contains("text")) {
						HandleHTML(dequeuedHtml, HTML.TypeOfHTML.TEXT);
						// int contentLenght =
						// extractContentLength(dequeuedHtml); // TODO: should
						// be done in different method using enum
						// if (contentLenght != -1) {
						// synchronized (m_ResultWrapper) {
						// m_ResultWrapper.m_PageAggregator.m_NumOfPages++;
						// m_ResultWrapper.m_PageAggregator.m_TotalSizeOfPagesInBytes+=
						// contentLenght;
						// }
						// }
						// System.out.println("(Data of page was aggregated) content is text");
						// getImgLinks(dequeuedHtml);
						// getUrlLinks(dequeuedHtml); // TODO: can links have
						// HTTP:// ?
					} else {
						System.out.println("Downloader: Error in content type of " + dequeuedHtml.toString());
					}
				} catch (Exception e) {
					System.out.println("Couldnt get content type from: " + dequeuedHtml.GetHeader());
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		System.out.println("Analyzer Finished!");
		// m_DownloadQueue.unregisterProducer();

	}

	private void HandleHTML(HTML dequeuedHtml, HTML.TypeOfHTML typeOfHtml) {
		int contentLenght = extractContentLength(dequeuedHtml); 
		switch (typeOfHtml) {
		case TEXT:
			if (contentLenght != -1) {
				synchronized (m_ResultWrapper) {
					m_ResultWrapper.m_PageAggregator.m_NumOfPages++;
					m_ResultWrapper.m_PageAggregator.m_TotalSizeOfPagesInBytes += contentLenght;
				}
			}
			System.out.println("(Data of page was aggregated) content is text");
			getImgLinks(dequeuedHtml);
			getUrlLinks(dequeuedHtml); // TODO: can links have
										// HTTP:// ?
			break;
		case IMAGE:
			if (contentLenght != -1) {
				synchronized (m_ResultWrapper) {
					m_ResultWrapper.m_ImageAggregator.m_NumOfImages++;
					m_ResultWrapper.m_ImageAggregator.m_TotalSizeInBytes += contentLenght;
				}
			} else { // TODO: Delete this, deubgging purposes
				System.out.println(dequeuedHtml.GetHeader() + " will not be aggregated!!!");
			}
			break;

		default:
			break;
		}
	}

	private void handleCaseOfImage(HTML dequeuedHtml) {
		// TODO Auto-generated method stub
		int contentLength = extractContentLength(dequeuedHtml);
		if (contentLength != -1) {
			synchronized (m_ResultWrapper.m_ImageAggregator) {
				m_ResultWrapper.m_ImageAggregator.m_NumOfImages++;
				m_ResultWrapper.m_ImageAggregator.m_TotalSizeInBytes += contentLength;
			}
		} else {
			System.out.println(dequeuedHtml.GetHeader() + " will not be aggregated!!!");
		}
		// int ContentLength = -1;
		// String regex = "Content-Length:\\s+([\\d]*)";
		// Pattern pat = Pattern.compile(regex);
		// Matcher m = pat.matcher(dequeuedHtml.GetHeader());
		// if (m.find()) {
		// try {
		// ContentLength = Integer.parseInt(m.group(1));
		// System.out.println("content length is: " + ContentLength);
		// // TODO: saveSomewhere
		// synchronized (m_ResultWrapper.m_ImageAggregator) {
		// m_ResultWrapper.m_ImageAggregator.m_NumOfImages++;
		// m_ResultWrapper.m_ImageAggregator.m_TotalSizeInBytes +=
		// ContentLength;
		// }
		// } catch (NumberFormatException e) {
		// // TODO: handle exception
		// System.out.println("Could not parse Content-Lenght header is malformed\n"
		// + dequeuedHtml.GetHeader());
		// }
		// }
	}

	private int extractContentLength(HTML dequeuedHtml) {
		int ContentLength = -1;
		String regex = "Content-Length:\\s+([\\d]*)";
		Pattern pat = Pattern.compile(regex);
		Matcher m = pat.matcher(dequeuedHtml.GetHeader());
		if (m.find()) {
			try {
				ContentLength = Integer.parseInt(m.group(1));
				System.out.println("content length is: " + ContentLength);
			} catch (NumberFormatException e) {
				// TODO: handle exception
				System.out.println("Could not parse Content-Lenght, header is malformed\n" + dequeuedHtml.GetHeader());
			}
		}

		return ContentLength;
	}

	private void getImgLinks(HTML dequeuedHtml) {
		// TODO Auto-generated method stub
		String[] allValidExtension = m_ConfigData.getImageExtensions();
		StringBuilder extenstionAddToRegex = new StringBuilder();
		extenstionAddToRegex.append("(");
		for (int i = 0; i < allValidExtension.length; i++) {
			if (i == allValidExtension.length - 1) {
				extenstionAddToRegex.append(allValidExtension[i].toLowerCase() + "|");
				extenstionAddToRegex.append(allValidExtension[i].toUpperCase() + ")");
			} else {
				extenstionAddToRegex.append(allValidExtension[i].toLowerCase() + "|");
				extenstionAddToRegex.append(allValidExtension[i].toUpperCase() + "|");
			}
		}

		String regex = "<(img|IMG)\\s+(src|SRC)=\"(.*?\\." + extenstionAddToRegex.toString() + ")\"";
		System.out.println("Regex for image is " + regex);
		Pattern pattern = Pattern.compile(regex);
		Matcher m = pattern.matcher(dequeuedHtml.GetBody());
		System.out.println("fetching  image links");
		while (m.find()) {
			String link = m.group(3);
			System.out.println("enqueue to Downloader: " + link);
			if (!m_AllTraversedLinks.Exists(link)) {
				// m_DownloadQueue.enqueue(link);
				m_DownloadQueue.enqueue(link);
				// m_AllTraversedLinks.enqueue(link);
				System.out.println("Analyzer: adding image link: " + link);
			}
		}
		System.out.println("Done with fetching image links");
	}

	private void getUrlLinks(HTML dequeuedHtml) {
		// String regex = "<(a|A) (href|HREF)=\"(http|HTTP)://" + m_Domain +
		// "/(.*?)\">";
		String regex = "<(a|A)\\s+(href|HREF)=\"(?!#)(.*?)\">"; // TODO: do HTTP
																// prefix means
																// its external?
		Pattern pattern = Pattern.compile(regex);
		Matcher m = pattern.matcher(dequeuedHtml.GetBody());
		System.out.println("fetching links");
		while (m.find()) {
			String link = m.group(3);
			System.out.println("enqueue to Downloader: " + link);
			if (!m_AllTraversedLinks.Exists(link)) {
				// m_DownloadQueue.enqueue(link);
				m_DownloadQueue.enqueue(link);
				// m_AllTraversedLinks.enqueue(link);
				System.out.println("Analyzer: adding link: " + link);
			}
		}
		System.out.println("Done with fetching links");
	}

	private String getContentTypeFromHeader(String header) throws Exception {
		String contentType = "";
		String regex = "Content-Type:\\s*([A-Za-z/]*)";
		Pattern pat = Pattern.compile(regex);
		Matcher m = pat.matcher(header);
		if (m.find()) {
			contentType = m.group(1);
			System.out.println("found : " + m.group(1));
		}

		return contentType;
	}

}
