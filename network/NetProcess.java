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

    //public PostOffice postOffice = new PostOffice();
        private static final int syscallConnect = 11, syscallAccept = 12;
    
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

    /**
    * Attempt to initiate a new connection to the specified port on the specified
    * remote host, and return a new file descriptor referring to the connection.
    * connect() does not give up if the remote host does not respond immediately.
    *
    * Returns the new file descriptor, or -1 if an error occurred.
    */
    private int handleConnect(int host, int port){
        int srcLink = Machine.networkLink().getLinkAddress();

        int srcPort = NetKernel.postOffice.findAvailablePort();
               
       Connection connection = new Connection(host, port, srcLink, srcPort);
        int i;
        for(i = 2; i < fileTable.length; i ++)
        {
            if (fileTable[i] == null) {
            fileTable[i] = connection;
            break;
            }

        }
        try {
            NetMessage message = new NetMessage(host, port, srcLink, srcPort, 1, 0, new byte[0]);
            NetKernel.postOffice.send(message);
        } catch (MalformedPacketException e) {
            System.out.println("Malformed packet exception");
            Lib.assertNotReached();
            return -1;
        }

        System.out.println("acknowledge");
        NetMessage acknowledgement = NetKernel.postOffice.receive(srcPort);
        System.out.println("Acknowledge " + acknowledgement);

        return i;
    }

    /**
     * Attempt to accept a single connection on the specified local port and return
    * a file descriptor referring to the connection.
    *
    *
    * If no connection requests are pending, returns -1 immediately.
    *
    * In either case, accept() returns without waiting for a remote host.
    *     
    * Returns a new file descriptor referring to the connection, or -1 if an error
    * occurred.
    */
    private int handleAccept(int port){

    NetMessage message = NetKernel.postOffice.receive(port);
    if (message == null) {
        return -1;
    }

    int dstLink  = message.packet.srcLink;
    int srcLink = Machine.networkLink().getLinkAddress();
    int dstPort = message.srcPort;
    Connection connection = new Connection(dstLink, dstPort, srcLink, port);
    NetKernel.postOffice.markPortAsUsed(port);
    int i;
    for(i = 2; i < fileTable.length; i ++)
    {
            if (fileTable[i] == null) {
            fileTable[i] = connection;
            break;
        }
       
    }
    try {
            NetMessage acknowledgement = new NetMessage(dstLink, dstPort, srcLink, port,  3, 0, new byte[0]);
            NetKernel.postOffice.send(acknowledgement);
    } catch(MalformedPacketException e) {
            System.out.println("malformed packed exception");
            Lib.assertNotReached();
            return -1;
        }

        return i;
        

    }
}

