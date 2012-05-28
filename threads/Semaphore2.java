package nachos.threads;

import nachos.machine.*;

/**
 * A <tt>Semaphore</tt> is a synchronization primitive with an unsigned value.
 * A semaphore has only two operations:
 *
 * <ul>
 * <li><tt>P()</tt>: waits until the semaphore's value is greater than zero,
 * then decrements it.
 * <li><tt>V()</tt>: increments the semaphore's value, and wakes up one thread
 * waiting in <tt>P()</tt> if possible.
 * </ul>
 *
 * <p>
 * Note that this API does not allow a thread to read the value of the
 * semaphore directly. Even if you did read the value, the only thing you would
 * know is what the value used to be. You don't know what the value is now,
 * because by the time you get the value, a context switch might have occurred,
 * and some other thread might have called <tt>P()</tt> or <tt>V()</tt>, so the
 * true value might now be different.
 */
public class Semaphore2 {
    /**
     * Allocate a new semaphore2.
     *
     * @param	initialValue	the initial value of this semaphore.
     */
    BinSem sem2;
    BinSem sem2LockP;
    BinSem sem2LockV;
    public Semaphore2(int initialValue) {
		value = initialValue;
		if (initialValue > 0)
			sem2 = new BinSem(1);
		else
			sem2 = new BinSem(0);
        sem2LockP = new BinSem(1);
        sem2LockV = new BinSem(1);
    }

    /**
     * Atomically wait for this semaphore to become non-zero and decrement it.
     */
    public void P() {
    	sem2LockP.P();
        if (value == 0)
    	   sem2.P();
        else if (value > 0)
            value--;
        sem2LockP.V();

	
    }

    /**
     * Atomically increment this semaphore and wake up at most one other thread
     * sleeping on this semaphore.
     */
    public void V() {
    	sem2LockV.P();
        if (value == 0)
		  sem2.V();
        value++;
        sem2LockV.V();

    }

    private static class PingTest implements Runnable {
	PingTest(Semaphore2 ping, Semaphore2 pong) {
	    this.ping = ping;
	    this.pong = pong;
	}
	
	public void run() {
	    for (int i=0; i<10; i++) {
        System.out.println("P");
		ping.P();
		pong.V();
	    }
	}

	private Semaphore2 ping;
	private Semaphore2 pong;
    }

    /**
     * Test if this module is working.
     */
    public static void selfTest() {
	Semaphore2 ping = new Semaphore2(0);
	Semaphore2 pong = new Semaphore2(0);

	new KThread(new PingTest(ping, pong)).setName("ping").fork();

	for (int i=0; i<10; i++) {
        System.out.println("hit");
	    ping.V();
        pong.P();

	}
    }

    private int value;
    private ThreadQueue waitQueue =
	ThreadedKernel.scheduler.newThreadQueue(false);
}
