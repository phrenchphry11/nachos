package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.Arrays;
import java.util.LinkedList;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	// change this to give this process a unique pid
	UserKernel.processIDSem.P();
	processID = UserKernel.newProcessID;
        UserKernel.newProcessID++;
	UserKernel.processIDSem.V();

	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,i,false,false,false,false);

	fileTable = new OpenFile[16];
	fileTable[0] = UserKernel.console.openForReading();
	fileTable[1] = UserKernel.console.openForWriting();
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// address translation
	int vpn = vaddr / pageSize;
	int voffset = vaddr % pageSize;
	TranslationEntry entry = pageTable[vpn];
	entry.used = true;
	int paddr = entry.ppn*pageSize + voffset;

	// if entry is not valid, then don't read
	if (paddr < 0 || paddr >= memory.length || !entry.valid)
	    return 0;

	int amount = Math.min(length, memory.length-paddr);

        // copies 'amount' bytes from byte array 'memory' starting at byte 'vaddr'
        // to byte array 'data' starting 'offset' bytes into 'data'
	System.arraycopy(memory, paddr, data, offset, amount);

	return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// address translation
	int vpn = vaddr / pageSize;
	int voffset = vaddr % pageSize;
	TranslationEntry entry = pageTable[vpn];
	entry.used = true;
	int paddr = entry.ppn*pageSize + voffset;

	// can't write it entry is not valid or read only
	if (paddr < 0 || paddr >= memory.length || !entry.valid || entry.readOnly)
	    return 0;

	entry.dirty = true;
	int amount = Math.min(length, memory.length-paddr);
	System.arraycopy(data, offset, memory, paddr, amount);

	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}
	// save the number of pages in the code section for later
	int codeSectionLength = numPages;

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments, numPages is now the total number
        // of pages needed for this process
	numPages++;

	// allocate pages for the stack and arguments
	for (int i = codeSectionLength; i < stackPages + codeSectionLength + 1; i++) {
	    TranslationEntry entry = pageTable[i];
	    UserKernel.freePagesSem.P();
	    int freePageNum = UserKernel.freePages.removeFirst();
	    UserKernel.freePagesSem.V();
	    entry.ppn = freePageNum;
	    entry.valid = true;
	}

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
			   argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages() || numPages > UserKernel.freePages.size()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");
	    for (int i=0; i < section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;
		TranslationEntry entry = pageTable[vpn];
		UserKernel.freePagesSem.P();
		int freePageNum = UserKernel.freePages.removeFirst();
		UserKernel.freePagesSem.V();
		entry.ppn = freePageNum;
		entry.valid = true;
		entry.readOnly = section.isReadOnly();
		section.loadPage(i, entry.ppn);
	    }
	}

	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
	for (int i = 0; i < pageTable.length; i++) {
	    TranslationEntry entry = pageTable[i];
	    if (entry.valid) {
		UserKernel.freePagesSem.P();
		UserKernel.freePages.add(entry.ppn);
		UserKernel.freePagesSem.V();
	    }
	}
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
	unloadSections();
	for (int i = 2; i < fileTable.length; i++) {
	    if (fileTable[i] != null)
		fileTable[i].close();
	}

	Machine.halt();
        Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }

    /**
     * Handle the exec() system call.
     * int  exec(char *name, int argc, char **argv);
     * Creates a new UserProcess to run process "name"
     * and forks a new UserThread to run it in.
     */
    private int handleExec(int name, int argc, int argv) {
        String[] args = new String[argc];

        // argv is in the form [(4byte address of arg1), (4byte address of arg2), ...]
        for (int i=0; i<argc; i++) {
            // get 4byte address to this argument
            byte[] argPoint = new byte[4];
            readVirtualMemory(argv+i*4, argPoint);
            
            // read string argument at pointer from above
            args[i] = readVirtualMemoryString(Lib.bytesToInt(argPoint,0), 256);
        }
        UserProcess child = new UserProcess();
	childList.add(child);
        String processName = readVirtualMemoryString(name,256);
        boolean ret = child.execute(processName, args);
        if(!ret) return -1;
        return child.processID;
    }

    /**
     * Handle the exit() system call. 
     * Clears the file table, sets exit type to "exit" argument, 
     * wakes parent if it joined, and finishes thread.
     * If this is the last process to exit, halt the machine
     */
    private int handleExit(int exit) {
        /* To Do: clear file table 
           store "exit" as this processes exit status
           call V() on this processes join semaphore in case anyone is waiting */
        unloadSections();
	for (int i = 2; i < fileTable.length; i++) {
	    if (fileTable[i] != null) {
		fileTable[i].close();
	    }
	}
	exitStatus = exit;
	joinSem.V();
        // Done 
        if (processID==0)
            Machine.halt();
        KThread.finish();
        return exit;
    }

    /**
     * Handle the join() system call. 
     * Puts this process to sleep waiting on process "pid"
     * returns exit status of process slept on
     */
    private int handleJoin(int pid) {
        /* To Do: check that the given pid is in fact this process's 
           child, otherwise return -1
           call P() on the child process's join semaphore
           return the exit status of the CHILD process */
        for (UserProcess child : childList) {
	    if (child.processID == pid) {
		child.joinSem.P();
		return child.exitStatus;
	    }
	}
	return -1;
    }

    /**
     * Handle the creat() system call.
     * Opens file, and clears it if it already exists.
     */
    private int handleCreate(int name) {
	//System.out.println("CREATE: " + name);
	String filename = readVirtualMemoryString(name,256);
	// catch -1? here
	OpenFile theFile = Machine.stubFileSystem().open(filename, true);
	if (theFile != null) {
	    for (int i = 2; i < fileTable.length; i++) {
		if (fileTable[i] == null) {
		    fileTable[i] = theFile;
		    return i;
		}
	    }
	}
	return -1;	
    }

    /**
     * Handle the open() system call.
     * Tries to open file.
     * If it doesn't exist, returns -1.
     */
    private int handleOpen(int name) {
	//System.out.println("OPEN: " + name);
	String filename = readVirtualMemoryString(name,256);
	OpenFile theFile = Machine.stubFileSystem().open(filename, false);
	if (theFile != null) {
	    for (int i = 2; i < fileTable.length; i++) {
		if (fileTable[i] == null) {
		    fileTable[i] = theFile;
		    return i;
		}
	    }
	}
	return -1;	
    }

    /**
     * Handle the close() system call.
     * Closes file it it exists.
     * Replaces file with null in the fileTable.
     */
    private int handleClose(int fileDescriptor){
	//System.out.println("CLOSE: " + fileDescriptor);
	String tempName;
        OpenFile file = fileTable[fileDescriptor];
	if (file != null){
	    fileTable[fileDescriptor].close();
	    fileTable[fileDescriptor] = null;
	    return fileDescriptor;
	}
        
        return -1;

    }

    /**
     * Handle the read() system call.
     * Reads file into buffer.
     * Returns size of what was read.
     */
    private int handleRead(int fileDescriptor, int buffer, int size){
	OpenFile file = fileTable[fileDescriptor];
        if (file == null) return -1;

        byte[] buff = new byte[size];
        int sizeRead; 
        sizeRead = file.read(buff, 0, size);

        writeVirtualMemory(buffer, buff);

        return sizeRead;
    }

    /**
     * Handle the write() system call.
     * Reads file from buffer.
     * Writes it through a new buffer to the new file.
     * Returns the size of what was written.
     */
    private int handleWrite(int fileDescriptor, int buffer, int size){
	OpenFile file = fileTable[fileDescriptor];
        if (file == null) return -1;

        byte[] buff = new byte[size];
        readVirtualMemory(buffer, buff);
        int sizeWritten;
        sizeWritten = file.write(buff, 0, size);
        return sizeWritten;
    }

    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
        case syscallExec:
            return handleExec(a0, a1, a2);
        case syscallExit:
            return handleExit(a0);
        case syscallJoin: 
            return handleJoin(a0);
	case syscallCreate:
    	    return handleCreate(a0);
    	case syscallOpen:
    	    return handleOpen(a0);
        case syscallClose:
            return handleClose(a0);
        case syscallRead:
            return handleRead(a0, a1, a2);
        case syscallWrite:
            return handleWrite(a0, a1, a2);

	    // To Do: add the rest of your syscall handlers here
	    // Don't forget, if the code in a given case does not return,
	    // you must add a break; statement or the next case will 
	    // also be executed, and the next, and so on until a return
	    // or a break;


	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    protected int processID;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    public int exitStatus;
    private Semaphore joinSem = new Semaphore(0);
    
    private LinkedList<UserProcess> childList = new LinkedList<UserProcess>();

    /** The file table */
    protected OpenFile[] fileTable;
}
