import java.awt.List;
import java.util.ArrayList;
import java.util.HashMap;


public class PageAggregator {

	public int m_NumOfPages;
	public int m_TotalSizeOfPagesInBytes;
	public int m_NumOfInternalLinks;
	public int m_NumOfExternalLinks;
	public int m_NumOfDomainsConnectedTo;
	public ArrayList<String> m_DomainsItConnectedTo; // if already crawled, then the link should be the key's value
	
	public PageAggregator() {
		// TODO Auto-generated constructor stub
		m_DomainsItConnectedTo = new ArrayList<>();
	}
}
