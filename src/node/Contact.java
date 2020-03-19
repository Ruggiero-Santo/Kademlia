package node;

import static com.theromus.utils.HexUtils.convertBytesToString;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import node.exception.contact.InvalidIPException;
import node.exception.contact.InvalidPortException;
import node.exception.contact.MustSetSizeIDException;
import node.exception.contact.SizeIDCanBeSetOnlyOneTimeException;

import com.theromus.sha.Keccak;
import com.theromus.sha.Parameters;
import com.theromus.exception.CantSetCustomHashLen;
import com.theromus.exception.MustBeSetTypeHashException;
import com.theromus.exception.NotValidHashLenException;

public class Contact {
	
	public static class ID {

		public ID(byte[] hash) {
			this.id = hash;
		}

		private byte[] id;
		
		/**
		 * You calculate a new random id away from your id. 
		 * @return New far ID computed
		 */
		public ID getNewFarID() {
			byte[] farID = new byte[id.length];
			int resto = 0;
			for(int i = 0; i < id.length; i++) {
				byte add = (byte) ((Math.random() * 155));
				int temp = id[i]+add+resto;
				farID[i] = (byte) (temp & 0xFF);
				resto = (i >> 8 ) & 0x1;
			}
			byte del = (byte) ((id.length*8) - sizeID);
			farID[0] = (byte) ((farID[0] & 0xFF) % Math.pow(2, 8-del));
			return new ID(farID);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(id);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof ID)) {
				return false;
			}
			ID other = (ID) obj;
			return Arrays.equals(id, other.id);
		}
		
		public static ID xorD(ID a, ID b) {
			byte[] out = new byte[a.id.length];
			for (int i = 0; i < a.id.length; i++) 
		        out[i] = (byte) (a.id[i] ^ b.id[i]);
		    return new ID(out);
		}
		
		public static int compare(ID a, ID b) {
			int res = 0;
			if(a.id.length != b.id.length) 
				if(a.id.length > b.id.length)
					return 1;
				else
					return -1;
			for(int i = 0; i < a.id.length && res == 0; i++)
				if(a.id[i] != b.id[i])
					return (a.id[i] & 0xFF) - (b.id[i] & 0xFF); 
			return res;
		}
		
		@Override
		public String toString() {
			return convertBytesToString(id);	
		}

		public int log2() {
			//Value calculated in case the number of bytes of the id is not multiple of bytes
			int siz = sizeID + (id.length * 8 - sizeID);
			
			int res = 0;
			for (int i = 0; i < id.length; i++)
				//Calculate the logarithm(b=2) of the first byte other than zero and then actually calculate the bucket.
				if(id[i] != 0) {
					int currLog = (int) Math.floor(Math.log(id[i])/Math.log(2));
					res = siz - (i * 8) - (8 - currLog);
					break;
				}
			
			return res;
		}
	}
	
	private static int sizeID = -1;
	private ID id;
	private byte[] ip;
	private short port;
	
	public static void setSizeID(int sizeID) throws SizeIDCanBeSetOnlyOneTimeException {
		if (Contact.sizeID == -1)
			Contact.sizeID = sizeID;
		else if (sizeID != Contact.sizeID)
			throw new SizeIDCanBeSetOnlyOneTimeException();
	}
	public int getSizeID() {
		return sizeID;
	}

	public ID getID() {
		return id;
	}

	public byte[] getIP() {
		return ip;
	}
    private void setIP(short[] ip) throws InvalidIPException {
    	this.ip = new byte[4];
    	for(int i = 0; i < 4; i++) {
    		if(ip[i]< 0 || ip[i] > 255)
				throw new InvalidIPException();
			else
				this.ip[i] = (byte) ip[i];
    	}
    }

    public short getPort() {
        return port;
    }
	private void setPort(int port) throws InvalidPortException {
		if(port< 0 || port > 65535)
			throw new InvalidPortException();
		else
			this.port = (short) port;
	}

	public Contact(short[] ip, int port) throws InvalidPortException, InvalidIPException {
		super();
		if(sizeID == -1) 
			throw new MustSetSizeIDException();
		
		setIP(ip);
		setPort(port);
        computeID();
	}
	
	private void computeID() {
    	byte[] temp = new byte[6];
		for(int i  = 0; i < 4 ; i++)
			temp[i] = ip[i];
		temp[4] = (byte) (port >> 8);
		temp[5] = (byte) port;
		
		Keccak sha;
		try {
			sha = new Keccak(Parameters.SHAKE256, sizeID);
			this.id = new ID (sha.getHash(temp));
		} catch (NotValidHashLenException | CantSetCustomHashLen | MustBeSetTypeHashException e) {
			e.printStackTrace();
		}
    }
	
	public static Contact[] sort(Contact[] contacts, ID id) {
		LinkedList<Contact> res = new LinkedList<Contact>(Arrays.asList(contacts));
		sort(res, id);
		return res.toArray(new Contact[0]);
	}
	
	public static int compareFormID(Contact a, Contact b, ID id) {
		return ID.compare(ID.xorD(a.getID(), id), ID.xorD(b.getID(), id));
	}

	public static void sort(LinkedList<Contact> list, ID id) {
		//Pre-compute the xor.
		ID[] tempL = new ID[list.size()];
		for(int i = 0; i < tempL.length; i++)
			tempL[i] = ID.xorD(id, list.get(i).getID());
		
		ID tempXOR;
		int n = list.size();
		for (int i = 0; i < n-1; i++)
			for (int j = 0; j < n-i-1; j++)
				if (ID.compare(tempL[j], tempL[j+1]) > 0) {
					Collections.swap(list, j, j+1);
					
					tempXOR = tempL[j];
					tempL[j] = tempL[j+1];
					tempL[j+1] = tempXOR;
				}
	}
	
	@Override
	public String toString() {
		String sIP = "";
		for(byte v : ip) 
			sIP += (v & 0xFF) +".";
		sIP = sIP.substring(0, sIP.length() - 1);
		return "Contact (ID=" + id + ", IP=" + sIP + ", Port=" + (port & 0xFFFF) + ")";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;  
		if (obj instanceof Contact) {  
			Contact other = (Contact) obj;
			return id.equals(other.id);
		}  
		return false;
	}
}