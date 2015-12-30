//Itay Abramowsky 304826688

/**
 * A synchronized bounded-size queue for multithreaded producer-consumer applications.
 * 
 * @param <T> Type of data items
 */
public class SynchronizedQueue<T> {

	private T[] buffer;
	private int producers;
	private int insertIndex;
	private int extractIndex;
	private int size;
	
	/**
	 * Constructor. Allocates a buffer (an array) with the given capacity and
	 * resets pointers and counters.
	 * @param capacity Buffer capacity
	 */
	@SuppressWarnings("unchecked")
	public SynchronizedQueue(int capacity) {
		
		this.buffer = (T[])(new Object[capacity]);
		this.producers = 0;
		this.insertIndex = 0;
		this.extractIndex = 0;
		this.size = 0;
	}
	
	/**
	 * Dequeues the first item from the queue and returns it.
	 * If the queue is empty but producers are still registered to this queue, 
	 * this method blocks until some item is available.
	 * If the queue is empty and no more items are planned to be added to this 
	 * queue (because no producers are registered), this method returns null.
	 * 
	 * @return The first item, or null if there are no more items
	 * @see #registerProducer()
	 * @see #unregisterProducer()
	 */
	public T dequeue() {
		synchronized (this) {
			//if there are no items in list
			while (this.getSize()==0) {
				//no items in list and no producer to put items then return null
				if (producers==0) {
					return null;
				}
				try {
					//the list is empty but there is producer who might enqueue then wait
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			// returning the dequeued item and 'deleting' it from the queue
			T toReturn = buffer[extractIndex];
			buffer[extractIndex] = null;
			extractIndex = (extractIndex+1)%buffer.length;
			//updating the size
			size--;
			
			this.notifyAll();
			return toReturn;
		}
		

	}

	/**
	 * Enqueues an item to the end of this queue. If the queue is full, this 
	 * method blocks until some space becomes available.
	 * 
	 * @param item Item to enqueue
	 */
	public void enqueue(T item) {
		
		synchronized (this) {
			//if queue is full
			while (this.size == buffer.length) {
				try {
					//wait for someone to enqueue and item
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			//inserting a new item and updating the size of the queue
			buffer[insertIndex] = item;
			insertIndex = (insertIndex+1)%buffer.length;
			size++;
			this.notifyAll();
			
					
		}

	}

	/**
	 * Returns the capacity of this queue
	 * @return queue capacity
	 */
	public int getCapacity() {
		return buffer.length;

	}

	/**
	 * Returns the current size of the queue (number of elements in it)
	 * @return queue size
	 */
	public int getSize() {
		
		return size;
	}
	
	/**
	 * Registers a producer to this queue. This method actually increases the
	 * internal producers counter of this queue by 1. This counter is used to
	 * determine whether the queue is still active and to avoid blocking of
	 * consumer threads that try to dequeue elements from an empty queue, when
	 * no producer is expected to add any more items.
	 * Every producer of this queue must call this method before starting to 
	 * enqueue items, and must also call <see>{@link #unregisterProducer()}</see> when
	 * finishes to enqueue all items.
	 * 
	 * @see #dequeue()
	 * @see #unregisterProducer()
	 */
	public void registerProducer() {
		synchronized (this) {
			this.producers++;		
		}
	}

	/**
	 * Unregisters a producer from this queue. See <see>{@link #registerProducer()}</see>.
	 * 
	 * @see #dequeue()
	 * @see #registerProducer()
	 */
	public void unregisterProducer() {
		synchronized (this) {
			this.producers--;
			//notify all in case some thread is waiting for a producer to insert a new item
			this.notifyAll();
		}
	}
}
