package nachos.network;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>VMProcess</tt> that supports networking syscalls.
 */
public class NetProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public NetProcess() {
    super();
    }

    PostOffice postOffice = new PostOffice();
    private static final int
    syscallConnect = 11,
    syscallAccept = 12;
    
    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>11</td><td><tt>int  connect(int host, int port);</tt></td></tr>
     * <tr><td>12</td><td><tt>int  accept(int port);</tt></td></tr>
     * </table>
     * 
     * @param   syscall the syscall number.
     * @param   a0  the first syscall argument.
     * @param   a1  the second syscall argument.
     * @param   a2  the third syscall argument.
     * @param   a3  the fourth syscall argument.
     * @return  the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
    switch (syscall) {
        case syscallAccept:
            return handleAccept(a0);
        case syscallConnect:
            return handleConnect(a0,a1);
    default:
        return super.handleSyscall(syscall, a0, a1, a2, a3);
    }
    }


    private int handleConnect(int host, int port){
        /*
        Yeah I don't fully understand about modifying the postOffice stuff,     
        but I'm pretty sure we just need to loop around our fileTable, find an 
        empty space, but our Connection object in it.  Then we do some sort of 
        NetMessage (I think) and then do fancy postOffice stuff with it, which 
        I don't fully understand :p  I'm guessing the "postOffice protocol" or 
        whatever probably has some send or receive function or getting ports or 
        something.  But yeah, maybe we can ask Sherri about that one.
        */
        int localID = Machine.networkLink().getLinkAddress();

        int srcPort = postOffice.findAvailablePort();
        
        //srcLink,srcPort, dstLink,dstPort
       
       Connection connection = new Connection(localID, srcPort, host, port);
        int i;
        for(i = 0; i < fileTable.length; i ++)
        {
            if (fileTable[i] == null) {
            fileTable[i] = connection;
            break;
            }

        }
        //int dstLink, int dstPort, int srcLink, int srcPort,
        //               int status, int seqNum, byte[] contents
        try {
        NetMessage message = new NetMessage(host, port, localID, srcPort, 1, connection.curSeqNum, new byte[0]);
        postOffice.send(message);
        } catch (MalformedPacketException e) {
            System.out.println("Malformed packet exception");
            Lib.assertNotReached();
            return -1;
        }
        return i;
    }

    private int handleAccept(int port){
        NetMessage message = postOffice.receive(port);
        if (message == null) {
            return -1;
        }
        int dstLink= message.packet.dstLink;
        int srcLink  = message.packet.srcLink;
        int dstPort = message.dstPort;
        int srcPort = message.srcPort;
        Connection connection = new Connection(srcLink, srcPort, dstLink, dstPort);
            int i;
        for(i = 0; i < fileTable.length; i ++)
        {
            if (fileTable[i] == null) {
            fileTable[i] = connection;
            break;
        }
        try {
            NetMessage acknowledgement = new NetMessage(dstLink, dstPort, srcLink, srcPort,  3, connection.curSeqNum, new byte[0]);
            postOffice.send(acknowledgement);
        } catch(MalformedPacketException e) {
            System.out.println("malformed packed exception");
            Lib.assertNotReached();
            return -1;
            }

        }
        return i;
    

    }
}

