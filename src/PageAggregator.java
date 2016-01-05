import java.util.HashMap;


public class PageAggregator {

	public int m_NumOfPages;
	public int m_TotalSizeOfPagesInBytes;
	public int m_NumOfInternalLinks;
	public int m_NumOfExternalLinks;
	public int m_NumOfDomainsConnectedTo;
	public HashMap<String, String> m_DomainsItConnectedTo; // if already crawled, then the link should be the key's value
	
	public PageAggregator() {
		// TODO Auto-generated constructor stub
	}
}
