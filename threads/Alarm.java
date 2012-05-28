package nachos.threads;
import nachos.machine.*;
import java.util.*;
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 *
	 * <p><b>Note</b>: Nachos will not function correctly with more than one
	 * alarm.
	 */
	PriorityQueue<TimeTuple> timerQueue;
	Lock lock;
	public Alarm() {
	timerQueue = new PriorityQueue(1, new TimeCompare());
	lock = new Lock();
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
		});
	}

	public class TimeCompare implements Comparator{
	public int compare(Object o1, Object o2) {
		TimeTuple t1 = (TimeTuple) o1;
		TimeTuple t2 = (TimeTuple) o2;
		if (t1.wakeTime > t2.wakeTime)
		return 1;
		else if (t1.wakeTime < t2.wakeTime)
		return -1;
		else return 0;
	}
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread
	 * that should be run.
	 */
	public void timerInterrupt() {	
	long curTime = Machine.timer().getTime();
	TimeTuple cur = timerQueue.peek();
	if (cur == null) KThread.currentThread().yield();
		
	else {
		TimeTuple curTuple = timerQueue.peek();
		long curWakeTime = curTuple.wakeTime;
			
		while (curWakeTime <= curTime) {
		curTuple = timerQueue.poll();
		curTuple.sem.V();

		curTuple = timerQueue.peek();
				
		if (curTuple == null) break;

		curWakeTime = curTuple.wakeTime;
		}
		KThread.currentThread().yield();
	}
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks,
	 * waking it up in the timer interrupt handler. The thread must be
	 * woken up (placed in the scheduler ready set) during the first timer
	 * interrupt where
	 *
	 * <p><blockquote>
	 * (current time) >= (WaitUntil called time)+(x)
	 * </blockquote>
	 *
	 * @param	x	the minimum number of clock ticks to wait.
	 *
	 * @see	nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
	
	long wakeTime = Machine.timer().getTime() + x;
	Semaphore2 sem = new Semaphore2(0);
	TimeTuple timeTuple = new TimeTuple(wakeTime, sem);
	lock.acquire();
	timerQueue.add(timeTuple);
	lock.release();
	sem.P();

	}
	
	private static class PingAlarmTest implements Runnable {
	PingAlarmTest(int which, Alarm alarm) {
		this.which = which;
		this.alarm = alarm;
		
	}
	Alarm alarm;

	public void run() {
		System.out.println("thread " + which + " started.");
		alarm.waitUntil(which);
		System.out.println("thread " + which + " ran.");
		
	}

	private int which;
	}


	public static void selfTest() {
	Alarm myAlarm = new Alarm();

	System.out.println("*** Entering Alarm self test");
	KThread thread1 = new KThread(new PingAlarmTest(1000,myAlarm));
	thread1.fork();

	KThread thread2 = new KThread(new PingAlarmTest(500,myAlarm));
	thread2.fork();

	new PingAlarmTest(2000,myAlarm).run();


	System.out.println("*** Exiting Alarm self test");
	}
	

	static class TimeTuple { 
	private long wakeTime;
	private Semaphore2 sem;
	
	TimeTuple(long wakeTime, Semaphore2 sem) {
		this.wakeTime = wakeTime;
		this.sem = sem;
	}

	public String toString() {
		String thing = String.valueOf(this.wakeTime);
		return thing;
	}
	}
}