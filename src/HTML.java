
public class HTML {

	private String m_Header;
	private String m_Body;
	private String m_ContentType;
	
	public HTML(String header, String body, String contentType) { // TODO: no need for content Type
		m_Header = header;
		m_Body = body;
	}
	
	public String GetHeader()
	{
		return m_Header;
	}
	
	public String GetBody()
	{
		return m_Body;
	}
	
	public String GetContentType()
	{
		return m_ContentType;
	}
	
	@Override
	public String toString() {
		String toString = m_Header + "\n\n" + m_Body;
		return toString;
	}
	
	public enum TypeOfHTML{
		VIDEO,
		IMAGE,
		TEXT,
		DOCUMENT,
	}
}

