package node;

import java.util.LinkedHashSet;

import node.Contact.ID;
import node.exception.CantAddContactException;
import node.exception.SizeBucketsCanBeSetOnlyOneTimeException;
import node.exception.contact.SizeIDCanBeSetOnlyOneTimeException;

/**
 * Table containing all the contacts that are known to the Node owner of the
 * teballa.
 *
 * @see Contact
 * @see Node
 */
class RoutingTable {

	private class KBucket {
		private LinkedHashSet<Contact> bucket;

		/**
		 * [KBucket description]
		 * 
		 * @param size bucket size.
		 */
		public KBucket() {
			super();
			bucket = new LinkedHashSet<Contact>();
		}

		/**
		 * Adds the contact specified according to Kademlia's specifications
		 * to the bucket.
 		 * The contact will not be added if the oldest contact is still in the
		 * net and the bucket is already full.
		 *
		 * @param contact Object of the contact to be inserted in the bucket
		 */
		public void add(Contact contact) {
			if(bucket.contains(contact)) {
				bucket.remove(contact);
				bucket.add(contact);
			} else if(bucket.size()+1 < sizeBucket)
				bucket.add(contact);
			else {
				Contact last = bucket.iterator().next();
				bucket.remove(last);
				if(owner.isAlive(last))
					bucket.add(last);
				else
					bucket.add(contact);
			}
		}
		
		/**
		 * Returns the entire list of contacts in the bucket.
 		 * The list is sorted by the last to most recently contacted contact.
		 * 
		 * @return lost of contcts.
		 */
		public Contact[] getContacts() {
			return bucket.toArray(new Contact[0]);
		}

		/**
		 * Check if there is the contact in the bucket.
		 * 
		 * @param contact Contact to check
		 * @return true if it's in the bucket; false otherwise
		 */
		public boolean contains(Contact contact) {
			return bucket.contains(contact);
		}
		
		@Override
		public String toString() {
			String str = "";
			for(Contact c : bucket) str += c+",";
			return "KBucket size("+bucket.size()+")[" + str + "]";
		}
	}

	/**
	 * Table length i.e. the number of buckets contained in the routing table.
 	 * It is also the length (number of bits) identifying the nodes.
	 */
	private static int lengthTable = -1;
	// Maximum number of contacts each bucket can contain.
	private static int sizeBucket = 20; // default Value.
	// Node owner of the list.
	private Node owner;
	// list of buckets on the rooting table
	private KBucket[] table;

	public static void setSizeBucket(int sizeBucket) throws SizeBucketsCanBeSetOnlyOneTimeException {
		if(RoutingTable.sizeBucket == 20)
			RoutingTable.sizeBucket = sizeBucket;
		else if (sizeBucket != RoutingTable.sizeBucket)
			throw new SizeBucketsCanBeSetOnlyOneTimeException();
		
	}	
	public static int getSizeBucket() {
		return sizeBucket;
	}
	
	/**
	 *
	 * @param lengthTable number of buckets that will be contained in the tebella
	 * @throws SizeIDCanBeSetOnlyOneTimeException When you change the value a second time
	 */
	public static void setLengthTable(int lengthTable) throws SizeIDCanBeSetOnlyOneTimeException {
		if(RoutingTable.lengthTable == -1)
			RoutingTable.lengthTable = lengthTable;
		else if (lengthTable != RoutingTable.lengthTable)
			throw new SizeIDCanBeSetOnlyOneTimeException();
	}
	public static int getLengthTable() {
		return lengthTable;
	}

	/**
	 * [RoutingTable description]
	 * 
	 * @param owner	Node owner of the routing table.
	 */
	public RoutingTable(Node owner) {
		super();
		this.owner = owner;
		table = new KBucket[lengthTable];
	}

	/**
	 * Aggiunge il conttatto specificato all'interno della tabela di routing.
	 * Il contatto viene inserito all'interno del bucket corrispondente al
	 * numero di bit in comune tra l'ID del nodo che voglio aggiungere e il
	 * mio ID(owner).
	 *
	 * @param contact Contact Information
	 * @throws CantAddContactException You cannot enter the contact if it is
	 * null or it's the owner.
	 */
	public void addContact(Contact contact) throws CantAddContactException {
		if(contact == null)
			throw new CantAddContactException("Can't add Null contact");
		if(owner.getContact().equals(contact)) return;

		int bucketIndex = bucketIndexFromDistance(ID.xorD(owner.getID(), contact.getID()));
		if(table[bucketIndex] == null) //Instanzio il bucket se null
			table[bucketIndex] = new KBucket();
		
		table[bucketIndex].add(contact);
	}

	/**
	 * Return the list of contacts (containing maximum sizeBucket) in the table
	 * next to the input Id.
	 * 
	 * @param id Id of the contact to which the output contacts are to be close
	 * @return List of contacts I know near the given id.
	 */
	public Contact[] getContacts(ID id) {
		int bucketIndex = bucketIndexFromDistance(ID.xorD(owner.getID(), id));
		LinkedHashSet<Contact> result = new LinkedHashSet<Contact>();
		
		int offset = 0;
		int tempBucketIndex, beforeTempBucketIndex = -1;
		do {
			tempBucketIndex = (bucketIndex + offset) % lengthTable;
			if(tempBucketIndex < 0)
				tempBucketIndex += lengthTable;
			
			//Add all the contacts in the bucket (max sizeBucket)
			if(table[tempBucketIndex] != null) {
				Contact[] temp = table[tempBucketIndex].getContacts();
				for(Contact c : Contact.sort(temp, id))
					if(!result.contains(c)) {
						result.add(c);
						if(result.size() >= sizeBucket) break;
					}
			}
			
			offset *= -1;
			if(offset <= 0)
				offset--;
			
			//Stop if I've checked all the buckets.
			if(beforeTempBucketIndex == tempBucketIndex) break;
			else beforeTempBucketIndex = tempBucketIndex;
		} while(result.size() < sizeBucket);
				
		return result.toArray(new Contact[0]);
	}

	/**
	 * Returns the bucket corresponding to the distance obtained between two id.
	 * The calculation of the bucket is nothing more than the logarithm (lower
	 * integer part) of the distance if it has been calculated with the
	 * Contact.xorD().
	 *
	 * @param distance Previously calculated distance between the owner ID and
	 * the id you are referring to.
	 * @return bucket index.
	 */
	private static int bucketIndexFromDistance(ID distance) {
		return distance.log2();
	}
	
	public boolean contains(Contact contact) {
		if(contact == null)
			return false;
		if(contact.equals(owner.getContact()))
			return true;
		int bucketIndex = bucketIndexFromDistance(ID.xorD(this.owner.getID(), contact.getID()));
		if(table[bucketIndex] == null)
			return false;
		else 
			return table[bucketIndex].contains(contact);
	}
	
	@Override
	public String toString() {
		String str = "\n";
		for(int i = 0; i < lengthTable; i++)
			if(table[i] != null)
				str += "\t"+i+"->"+table[i]+"\n";
		return "RoutingTable [" + str + "]";
	}
	public String getCvsEdge() {
		String str = "";
		for (KBucket kBucket : table)
			if(kBucket != null) 
				for(Contact contact : kBucket.getContacts())
					str += owner.getID()+";"+contact.getID()+"\n";
		return str;
	}
	
	public Contact getClosest() {
		for(KBucket b : table)
			if(b != null)
				return Contact.sort(b.getContacts(), owner.getID())[0];
		return null;
	}
}
