package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

public class Connection extends OpenFile{

	public int srcLink;
	public int srcPort;
	public int dstLink;
	public int dstPort;
	public int curSeqNum;
	
	public Connection(int srcLink, int scrPort, int dstLink, int dstPort){
		super(null, "Connection");
		this.srcLink = srcLink;
		this.srcPort = srcPort;
		this.dstLink = dstLink;
		this.dstPort = dstPort;
		this.curSeqNum = 0;
	}

	public int read(byte[] buffer, int offset, int length) {
		int numBytesRead = 0;
		NetMessage message = NetKernel.postOffice.receive(srcPort);
		if (message == null) {
			return 0;
		}
		for(int i = 0; i < length; i ++) {
			buffer[i] = message.contents[i + offset];
			numBytesRead++;
		}
		
		return numBytesRead;

	}
}