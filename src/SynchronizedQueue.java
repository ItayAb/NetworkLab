import java.util.ArrayList;

public class SynchronizedQueue<T> {

	private ArrayList<T> m_InnerList;

	public <T> SynchronizedQueue() {
		// TODO Auto-generated constructor stub
		m_InnerList = new ArrayList<>();
	}

	public void enqueue(T item) {
		synchronized (this) {
			m_InnerList.add(item);
			this.notifyAll();
		}
	}

	public T dequeue() {
		synchronized (this) {
			while (m_InnerList.size() == 0) {
				try {	
					if (ThreadManager.isRunning) {
						this.wait();						
					}
					
					if (!ThreadManager.isRunning) {
						this.notifyAll();						
						return null;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}
			}

			T toReturn = m_InnerList.get(m_InnerList.size() - 1);
			m_InnerList.remove(toReturn);

			this.notifyAll();
			return toReturn;
		}
	}

	public ArrayList<T> getAllQueue() {
		synchronized (this) {
			ArrayList<T> toReturn = new ArrayList<>();

			for (T runner : m_InnerList) {
				toReturn.add(runner);
			}

			return toReturn;
		}
	}

	public int getSize(){
		synchronized (this) {
			return m_InnerList.size();
		}
	}
	
	public boolean Exists(T item) {
		synchronized (this) {
			for (T runner : m_InnerList) {
				if (runner.equals(item)) {
					return true;
				}
			}
			
			return false;
		}
	}		
}
