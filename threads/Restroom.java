package nachos.threads;

import nachos.machine.*;
import java.util.Random;

public class Restroom {

    static Random rgen = new Random();
    static Lock lock = new Lock(); //this lock is like the door sign
    static Condition okayForMenToEnter = new Condition(lock);
    static Condition okayForWomenToEnter = new Condition(lock);
    static int waitingWomen = 0;
    static int waitingMen = 0;
    static int womenIn = 0;
    static int menIn = 0;


    
    static void womanArrives() {
        lock.acquire();
        System.out.println("woman arrives");
        while(menIn > 0){
            waitingWomen++;
            okayForWomenToEnter.sleep();
            waitingWomen--;
        }
        womenIn++;
        lock.release();

    }

    static void manArrives() {
        lock.acquire();
        System.out.println("man arrives");
        while (womenIn > 0){
            waitingMen++;
            okayForMenToEnter.sleep();
            waitingMen--;
        }
        menIn++;
        lock.release();
    }

    static void womanLeaves() {
        lock.acquire();
        System.out.println("woman leaves");
        womenIn--;
        if(womenIn==0) {
            okayForMenToEnter.wakeAll();
        }
        lock.release();

    }


    static void manLeaves() {
        lock.acquire();
        System.out.println("man leaves");
        menIn--;
        if(menIn==0){
            okayForWomenToEnter.wakeAll();
        }
        lock.release();

    }
   
    
    public static void selfTest() {
        //Restroom potty = new Restroom();
        System.out.println("\n***running Restroom self-test***\n");
        
        // 20 people show up at the door with sex chosen randomly for each.
        // women take a random number of clock ticks up to 1,000 inside the 
        // restroom, men take a random number up to 500, to make it more interesting.
        for (int i=0; i<10; i++) {
            if (rgen.nextInt(2)==0) {
                new KThread(new Runnable() { 
                        public void run() {
                            womanArrives();
                            ThreadedKernel.alarm.waitUntil(rgen.nextInt(1000));
                            womanLeaves();
                        }
                    }).setName(""+i).fork();
            }
            else {
                new KThread(new Runnable() { 
                        public void run() {
                            manArrives();
                            ThreadedKernel.alarm.waitUntil(rgen.nextInt(500));
                            manLeaves();
                        }
                    }).setName(""+i).fork();
            }
        }

        // one last man (21st) person waits 2000 clock ticks to ensure that this main 
        // thread will be interrupted and so this guy will arrive at the bathroom last
        // so that all of the other threads can finish before the program exits
        ThreadedKernel.alarm.waitUntil(2000);
        manArrives();
        ThreadedKernel.alarm.waitUntil(rgen.nextInt(500));
        manLeaves();
    }
}