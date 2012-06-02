package nachos.userprog;

import nachos.network.*;
import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.Arrays;
import java.lang.Math;

//connection extends openfile
public class Connection extends OpenFile{

	public int srcLink;
	public int srcPort;
	public int dstLink;
	public int dstPort;
	public int curSeqNum;
	
	public Connection(int dstLink, int dstPort, int srcLink, int srcPort){
		//params are the source link, source port, destination link, destination port
		super(null, "Connection");
		this.srcLink = srcLink;
		this.srcPort = srcPort;
		this.dstLink = dstLink;
		this.dstPort = dstPort;
		this.curSeqNum = 0;
	}


	public int read(byte[] buffer, int offset, int length) {
		//reads the packet into a buffer
		NetMessage message = NetKernel.postOffice.receive(srcPort);
		if (message == null) {
			return 0;
		}
		
		curSeqNum++;
		int numBytesRead = Math.min(length, message.contents.length);
		System.arraycopy(message.contents, 0, buffer, offset, numBytesRead);

		return numBytesRead;

	}

	public int write(byte[] buffer, int offset, int length) {
		//writes a packet from a buffer
		int numBytesWritten = 0;
		try{
			NetMessage message = new NetMessage(dstLink, dstPort, srcLink, srcPort, 0, curSeqNum, new byte[length]);
			for (int i = offset; i < length + offset; i++) {
				message.contents[i-offset] = buffer[i];
				numBytesWritten++;
			}
			NetKernel.postOffice.send(message);
			return numBytesWritten;
		} 
		catch (MalformedPacketException e) {
			return -1;
		}
	}
}
