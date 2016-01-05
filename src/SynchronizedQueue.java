import java.util.ArrayList;

public class SynchronizedQueue<T> {

	private ArrayList<T> m_InnerList;

	public <T> SynchronizedQueue() {
		// TODO Auto-generated constructor stub
		m_InnerList = new ArrayList<>();
	}

	public void enqueue(T item) {
		synchronized (m_InnerList) {
			m_InnerList.add(item);
			m_InnerList.notifyAll();
		}
	}

	public T dequeue() {
		synchronized (m_InnerList) {
			while (m_InnerList.size() == 0) {
				try {	
					if (ThreadManager.isRunning) {
						m_InnerList.wait();						
					}
					
					if (!ThreadManager.isRunning) {
						m_InnerList.notifyAll();						
						return null;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}
			}

			T toReturn = m_InnerList.get(m_InnerList.size() - 1);
			m_InnerList.remove(toReturn);

			m_InnerList.notifyAll();
			return toReturn;
		}
	}

	public ArrayList<T> getAllQueue() {
		synchronized (m_InnerList) {
			ArrayList<T> toReturn = new ArrayList<>();

			for (T runner : m_InnerList) {
				toReturn.add(runner);
			}

			return toReturn;
		}
	}

	public int getSize(){
		synchronized (m_InnerList) {
			return m_InnerList.size();
		}
	}
	
	public boolean Exists(T item) {
		synchronized (m_InnerList) {
			for (T runner : m_InnerList) {
				if (runner.equals(item)) {
					return true;
				}
			}
			
			return false;
		}
	}
	
	public void WakeAll(){
		synchronized (m_InnerList) {
			m_InnerList.notifyAll();
		}
	}
	
}
