import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;


public class PortScanner extends Thread{

	
	private int m_PortToCheck;
	private SynchronizedQueue<Integer> m_PortScannedList;
	private String m_Domain;
	private SynchronizedQueue<PortScanner> m_QueueOfPortScanners;
	
	public PortScanner(SynchronizedQueue<PortScanner> queueOfPortScanners, String domain, SynchronizedQueue<Integer> portScannedList) {
		// TODO Auto-generated constructor stub		
		m_PortScannedList = portScannedList;
		m_Domain = domain;
		m_QueueOfPortScanners = queueOfPortScanners;
	}
	
	public void setPortToCheck(int portToCheck){
		m_PortToCheck = portToCheck;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		Socket testPort = new Socket();
		try {
			testPort.connect(new InetSocketAddress(m_Domain, m_PortToCheck), 1000);
			if (testPort.isConnected()) {
				System.out.println("Port " + m_PortToCheck + " is Valid!");
				m_PortScannedList.enqueue(Integer.valueOf(m_PortToCheck));
			}
		}catch (SocketTimeoutException e) {
			// TODO: handle exception
			
		}
		catch (ConnectException e) {
			// TODO: handle exception
		}
		catch (IllegalBlockingModeException e) {
			// TODO: handle exception
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();			
		}
		catch (Exception e) {
			// TODO: handle exception
			//TODO: what to do if exception
		}
		finally{
			if (!testPort.isClosed()) {
				try {
					testPort.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println("PortChecker: couldnt close with port " + m_PortToCheck);
				}
			}
			
			m_QueueOfPortScanners.enqueue(new PortScanner(m_QueueOfPortScanners, m_Domain, m_PortScannedList));
		}
		
	}
	
}
