package virtual_net;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import node.Contact;
import node.Contact.ID;
import node.Node;

public class Internet {
	private HashMap<ID, Node> allHost;
	private int collisions = 0;
	
	public Internet() {
		allHost = new HashMap<ID, Node>();
	}
	
	/**
	 * Registers the node within the network, the network also communicates the
	 * contact of its bootstrap node, this is a node taken randomly between the
	 * nodes within the network.
	 *  
	 * @return Contact bootstrap node
	 * @throws CollisionException 
	 */
	public Contact connect(Node node) throws CollisionException  {
		
		if(allHost.containsKey(node.getID())) {
			collisions++;
			throw new CollisionException();
		}
		
		Contact res = null;
		if(allHost.size() != 0) {
			ID[] temp = allHost.keySet().toArray(new ID[0]);
			res = allHost.get(temp[(int) (Math.random()*temp.length)]).getContact();
		}
		allHost.put(node.getID(), node);
		return res;
	}

	/**
	 * Function that simulates a PING CPR on the recipient node by the sender node.
	 * 
	 * @param sender Node sending the request
	 * @param recipient Contact you want to send the request to
	 * @return True if the knot is alive and therefore is responding. False if 
	 *  the node is not registered to the network or for some reason says it is not responding.
	 */
	public boolean sendPING(Node sender, Contact recipient) {
		Node node_recipient = allHost.get(recipient.getID());
		if(node_recipient == null) return false;
		return node_recipient.PING(sender.getContact());
	}

	/**
	 * Function that simulates a FIND_NODE RCP on the recipient node by the sender node.
	 * 
	 * @param sender Node sending the request
	 * @param id ID that sender is looking for
	 * @param recipient Contact you want to send the request to
	 * @return Array of contacts, the list of contacts that the recipient knows
	 * close to the specified id
	 */
	public Contact[] sendFIND_NODE(Node sender, ID id, Contact recipient) {
		Node node_recipient = allHost.get(recipient.getID());
		if(node_recipient == null) return null;
		return node_recipient.FIND_NODE(sender.getContact(), id);
	}

	/**
	 * Creates a file in the specified path and name containing all network edges.
	 * Each edge is encoded with Departure_ID;ID_Destination.
 	 * The file may have any exemptions but will be written as a normal text file.
	 * 
	 * @param filename path/name of the file in which to save the network edges
	 */
	public void saveNetwork(String filename) {
		try {
			FileWriter file = new FileWriter(new File("C-"+collisions+"_"+filename));

			for(ID key : allHost.keySet()) 
				file.write(allHost.get(key).getCvsEdge()+"\n");
			
			file.flush();
			file.close();
		} catch(IOException e) {e.printStackTrace();}
	}
	
	// Mainly useless function (random IP)
	public short[] myIP() {
		short[] ip = new short[4];
		for(int i = 0; i < 4 ; i++)
			ip[i] = (short) (Math.random()*255);
		return ip;
	}
}
