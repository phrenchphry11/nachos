package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    /*
      NOTE: island B := molokai, island A := oahu

      Our general algorithm is to ferry all the children from A to B by having two children row together from A to B and only one rowing back to A. After all of the children go, then one rows back to A, wakes up an adult. The adult rows to B, wakes up a child, and then is done. The child on B who woke up, rows to A, takes both children from A to B, and then one child rows back and repeats the process if there is an adult waiting to go. When it reaches to point where there are no more adults on A, the child rows back to B and finishes.

      Because adults never row at first (because there are always 2 children on A at the beginning)
     */

    static BoatGrader bg;

    //we keep a count of the number of children on each island, 
    //the number of adults on each island, the number of children ready to go to B, and the number of
    //children (0, 1, or 2) who just rowed together from A to B
    private static int childrenOnA; 
    private static int childrenOnB;
    private static int adultsOnA;
    private static int adultsOnB;
    private static int cWaitBoatA;
    private static int childrenFromBoat;

    //boolean as to if the boat is on A or not
    private static boolean boatAtA;

    //lock an island when we're trying to access it so concurrent threads don't access an island at the same time
    private static Lock islandA = new Lock();
    private static Lock islandB = new Lock();

    //condition for adults; if there are 2 or more children on the island then sleep
    private static Condition adultA = new Condition(islandA);
    //three conditions for children: 1 for sleeping on B if they aren't the child boating back.
    //1 for waiting on A if there are already more than 2 children ready to go over
    //1 for waiting for the second child to go with to go to B
    private static Condition cWaitOnLandB = new Condition(islandB);    
    private static Condition cWaitOnLandA = new Condition(islandA);
    private static Condition cWaitForBoatA = new Condition(islandA);

    //the main thread waits until our code says it's done
    private static Semaphore doneSem = new Semaphore(0);
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();

	
	// System.out.println("\n ***Testing Boats with only 2 children***");
	// begin(0, 2, b);

	// System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
  	// begin(1, 2, b);

  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.

	// Instantiate global variables here
	bg = b;
	childrenOnA = children;
	childrenOnB = 0;
	adultsOnA = adults;
	adultsOnB = 0;
	cWaitBoatA = 0;
	childrenFromBoat = 0;
	boatAtA = true;
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.
	for (int i = 0; i < children; i++) {
	    new KThread(new Runnable () { public void run() { ChildItinerary(); } }).setName("c" + i).fork();
	}

	for (int i = 0; i < adults; i++) {
	    new KThread(new Runnable () { public void run() { AdultItinerary(); } }).setName("a" + i).fork();
	}

	doneSem.P();

	/*
	  Runnable r = new Runnable() {
	  public void run() {
	  SampleItinerary();
	  }
	  };
	  KThread t = new KThread(r);
	  t.setName("Sample Boat Thread");
	  t.fork();
	*/

    }

    static void AdultItinerary()
    {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	   bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
	//lock the island because we have an adult bro accessing the island
	islandA.acquire();
	while (childrenOnA > 1 || !boatAtA) {
	    //if there is more than one child, then the children should go first, so go to sleep
	    adultA.sleep();
	}
	//otherwise we are free to boat!  fewer adults on A.  release lock because bro left.  not on A anymore
	adultsOnA--;
	boatAtA = false;
	islandA.release();
	bg.AdultRowToMolokai();
	islandB.acquire();
	adultsOnB++;
	//when an adult gets to B, we want a child to pilot the boat back to A
	cWaitOnLandB.wake();
	//adult doesn't need lock anymore because they're not doing anything ever again
	islandB.release();
    }

    static void ChildItinerary()
    {
	//while there are people still on the island
	while (childrenOnA + adultsOnA > 1) {
	    //we're doing stuff to the island so we need to lock it
	    islandA.acquire();
	    if (childrenOnA == 1) {
		//we can only boat children if there are two of them, but otherwise, then an adult should go
		adultA.wake();
	    }
	    while (cWaitBoatA >= 2 || !boatAtA) {
		//there are enough children to boat over to B
		//let the other child go first
		cWaitOnLandA.sleep();
	    }
	    if (cWaitBoatA == 0) { //if no other child ready to go over
		cWaitBoatA++;
		//get a buddy by waking up thread
		cWaitOnLandA.wake();
		//wait for child to go across with
		cWaitForBoatA.sleep();
		bg.ChildRideToMolokai();
		//wake up the rower to get off on B
		cWaitForBoatA.wake();
	    } else { 
		//if there's already someone ready to go
		cWaitBoatA++;
		//wake up the passenger
		cWaitForBoatA.wake();
		bg.ChildRowToMolokai();
		//sleep to wait for passenger to come over
		cWaitForBoatA.sleep();
	    }
	    //they're off the boat
	    cWaitBoatA--;
	    //and off A
	    childrenOnA--;
	    boatAtA = false;
	    islandA.release();
	    //this is the point where they get to B
	    islandB.acquire();
	    //eventually both child threads will hit this, increment for each hit
	    childrenOnB++;
	    //marking that the child is off of the boat
	    childrenFromBoat++;
	    if (childrenFromBoat == 1) {
		//only one child from the previous 2 child boat trip sleeps
		cWaitOnLandB.sleep();
	    }
	    //always one child has to go back to A 
	    childrenOnB--;
	    //boat is going back over so children arriving from A gets reset
	    childrenFromBoat = 0;
	    islandB.release();
	    bg.ChildRowToOahu();
	    islandA.acquire();
	    childrenOnA++;
	    boatAtA = true;
	    islandA.release();
	}
	//to check if people are still on the original island
	islandA.acquire();
	childrenOnA--;
	islandA.release();
	bg.ChildRowToMolokai();
	islandB.acquire();
	childrenOnB++;
	islandB.release();
	System.out.println("Children A/B: " + childrenOnA + " / " + childrenOnB);
	System.out.println("Adults A/B: " + adultsOnA + " / " + adultsOnB);
	doneSem.V();
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
