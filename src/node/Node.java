package node;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import node.Contact.ID;
import node.exception.SizeBucketsCanBeSetOnlyOneTimeException;
import node.exception.contact.InvalidIPException;
import node.exception.contact.InvalidPortException;
import node.exception.contact.SizeIDCanBeSetOnlyOneTimeException;
import virtual_net.CollisionException;
import virtual_net.Internet;

public class Node {
	private static int alfa = 3;

	private Internet connection;
	private Contact me;
	private RoutingTable table;
	
	
	public static void setSizeID(int sizeID) throws SizeIDCanBeSetOnlyOneTimeException {
		RoutingTable.setLengthTable(sizeID);
		Contact.setSizeID(sizeID);
	}

	public static void setSizeBuckets(int sizeBucket) throws SizeBucketsCanBeSetOnlyOneTimeException {
		RoutingTable.setSizeBucket(sizeBucket);
	}
	
	public ID getID() {
		return me.getID();
	}
	
	public Contact getContact() {
		return me;
	}
	
	public Node(Internet connection)  {
		super();
		
		this.connection = connection;

		Contact myBoot = null;
		do {
			try {
				this.me = new Contact(connection.myIP(), (int) (Math.random()*64331) + 1024);
				myBoot = connection.connect(this);
			} catch (CollisionException e) {
				this.me = null;
			} catch (InvalidPortException | InvalidIPException e) {e.printStackTrace();}
		} while(this.me == null);
		
		table = new RoutingTable(this);
		if (myBoot != null) {
			table.addContact(myBoot);
			join();
		}
	}
	
	/**
	 * Find yourself to know your successor.
	 * That is, you look for who before you managed the space that you should
	 * manage, then you have to look for a random ID away from you.
	 */
	private void join() {
		lookup(me.getID());
		lookup(table.getClosest().getID().getNewFarID());
	}

	boolean isAlive(Contact recipient) {
		return connection.sendPING(this, recipient);
	}	
	
	/**
	 * Sends the FIND_NODE request to a series of parallel "alpha" nodes that
	 * should be close to the node that i looking for (taken from my table) to 
	 * receive their lists of nodes close to the requested id.
	 * Once I receive the lists merge them by ordering them by distance, even
	 * with my previous one, to request again FIND_NODE to new closest node 
	 * (the current list.)

	 * @param id ID on which to perform the lookup
	 * @return 
	 */
	private Contact[] lookup(ID id) {
		HashSet<Contact> alreadyContacted = new HashSet<Contact>();
		
		LinkedHashSet<Contact> kClosest = new LinkedHashSet<Contact>(Arrays.asList(table.getContacts(id)));
		LinkedHashSet<Contact> toContact = new LinkedHashSet<Contact>(kClosest);
		
		HashSet<Contact> toMerge = new HashSet<Contact>();
		do {
			//I run the FIND_NODE on the first alpha nodes in parallel  
			toMerge = toContact.stream().limit(alfa)
			.map(x -> {
				alreadyContacted.add(x); 
				return connection.sendFIND_NODE(this, id, x);
				})
			.reduce(toMerge, (x,y) -> { x.addAll(Arrays.asList(y)); return x;}, (x,y) -> {x.addAll(y); return x;});
			
			//I remove the Node that i've already tied. 
			toContact.removeAll(alreadyContacted);
			toMerge.remove(me);
			toMerge.removeAll(toContact);
			toMerge.removeAll(alreadyContacted);
			
			//Adding the new contacts that i've discovered
			toMerge.forEach(x -> table.addContact(x));
			
			//Updating the list of nodes to be contacted
			toContact = merge(toContact, toMerge, id);
		} while(endIteration(kClosest, toContact, id));
		
		return kClosest.toArray(new Contact[0]);
	}
	/**
	 * Checking if the lookup process is over.
	 * Is over when checking the new contacts i discovered that, with the
	 * current iteration (toContact set), there are no closer contacts.
	 *  	
	 * @param kClosest List of the closest known contacts so far
	 * @param toContact	list of new contacts
	 * @param id id compared to those contacts must be close to each other
	 * @return True if the kClostest list has been modified so if there were
	 *	closer contacts; False otherwise.
	 */
	private boolean endIteration(Set<Contact> kClosest, Set<Contact> toContact, ID id) {
		//If the list of nodes to contact is empty I have concluded
		if(toContact.size() == 0) return false;

		if(kClosest.containsAll(toContact)) return false;
		kClosest = merge(kClosest, toContact, id);
		
		return true;
	}
	
	/**
	 * Combine the two sets of contacts and create a third one sorted according
	 * to the input id, the resulting set will be as large as a routing table bucket.
	 *
	 * @param set1 first set
	 * @param set2 second set
	 * @param id reference id for sorting
	 * @return a set ordered according to the id.
	 */
	private static LinkedHashSet<Contact> merge(Set<Contact> set1, Set<Contact> set2, ID id) {
		//Merging the two into a linked list
		LinkedList<Contact> result = new LinkedList<Contact>(set1);
		result.addAll(set2);
		//Order the elements of the two lists according to the distance between the contact and the id
		Collections.sort(result, new Comparator<Contact>() {
		    @Override
			public int compare(Contact a, Contact b) {
		    	return ID.compare(ID.xorD(a.getID(), id), ID.xorD(b.getID(), id));
			}
		});
		//just take the first k elements
		result.removeIf(x -> result.indexOf(x) > (RoutingTable.getSizeBucket()-1));
		return new LinkedHashSet<Contact>(result);
	}
	
	/**
	 * Receive the request to find and search in my table the k closest to the requested id
	 * 
	 * @param sender who sent me the request
	 * @param id id that the contacts must be close
	 * @return list of k contacts close to the id
	 */
	public Contact[] FIND_NODE(Contact sender, ID id) {
		Contact[] res = table.getContacts(id);
		table.addContact(sender);
		return res;
	}
	/**
	 * Receive Ping's request to answer if I'm alive and can answer
	 * 
	 * @param sender who made the request
	 * @return True
	 */
	public boolean PING(Contact sender) {
		table.addContact(sender);
		return true;
	}
	
	@Override
	public String toString() {
		return "Node [" + me + "]";
	}
	
	/**
	 * Returns a string containing all the edges that depart from this knot.
	 * A single edge is formatted with myID;targetID (useful for exam of graph)
	 * 
	 */
	public String getCvsEdge() {
		return table.getCvsEdge();
	}
}
