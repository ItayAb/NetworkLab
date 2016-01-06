import java.awt.List;


public class CrawlelResultForDomain {

	public boolean m_IsRespectRobots;
	public ImageAggregator m_ImageAggregator;
	public VideoAggregator m_VideoAggregator;
	public DocumentAggregator m_DocumentAggregator;
	public PageAggregator m_PageAggregator;
	public RttAggregator m_RttAggregator;
	
	
	public CrawlelResultForDomain() {
		// TODO Auto-generated constructor stub
		m_ImageAggregator = new ImageAggregator();
		m_VideoAggregator = new VideoAggregator();
		m_DocumentAggregator = new DocumentAggregator();
		m_PageAggregator = new PageAggregator();
		m_RttAggregator = new  RttAggregator();
	}	
}
