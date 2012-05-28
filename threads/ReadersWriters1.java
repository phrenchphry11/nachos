package nachos.threads;

import nachos.machine.*;
import java.util.Random;

public class ReadersWriters1 {
static Random rgen = new Random();
static Lock lock = new Lock();
static Condition okToRead = new Condition(lock);
static Condition okToWrite = new Condition(lock);
static int activeReaders = 0;
static int activeWriters = 0;
static int waitingReaders = 0;
static int waitingWriters = 0;
static void Reader() {
lock.acquire();
while (activeWriters + waitingWriters > 0) {
waitingReaders++;
okToRead.sleep();
waitingReaders--;
}
activeReaders++;
lock.release();
// reading stuff here
System.out.println("reading stuff");
lock.acquire();
activeReaders--;
System.out.println("done reading");
if (activeReaders==0 && waitingWriters>0)
okToWrite.wake();
lock.release();
}
static void Writer() {
lock.acquire();
while (activeWriters + activeReaders > 0) {
waitingWriters++;
okToWrite.sleep();
waitingWriters--;
}
activeWriters++;
lock.release();
// writing stuff here
System.out.println("writing stuff");
lock.acquire();
activeWriters--;
System.out.println("done writing");
if (waitingWriters > 0)
okToWrite.wake();
else if (waitingReaders > 0)
okToRead.wakeAll();
lock.release();
}
  public static void selfTest() {
        //Restroom potty = new Restroom();
        System.out.println("\n***running Readers & Writers self-test***\n");
        
        // 20 people show up at the door with sex chosen randomly for each.
        // women take a random number of clock ticks up to 1,000 inside the 
        // restroom, men take a random number up to 500, to make it more interesting.
        for (int i=0; i<10; i++) {
            if (rgen.nextInt(2)==0) {
                new KThread(new Runnable() { 
                        public void run() {
                            Reader();
                            
                        }
                    }).setName(""+i).fork();
            }
            else {
                new KThread(new Runnable() { 
                        public void run() {
                            Writer();
                        }
                    }).setName(""+i).fork();
            }
        }

        // one last man (21st) person waits 2000 clock ticks to ensure that this main 
        // thread will be interrupted and so this guy will arrive at the bathroom last
        // so that all of the other threads can finish before the program exits
        ThreadedKernel.alarm.waitUntil(2000);
        Writer();
        ThreadedKernel.alarm.waitUntil(rgen.nextInt(500));
    }
}
