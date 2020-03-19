import node.Node;
import node.exception.contact.InvalidIPException;
import node.exception.contact.InvalidPortException;
import node.exception.contact.MustSetSizeIDException;
import node.exception.contact.SizeIDCanBeSetOnlyOneTimeException;
import virtual_net.Internet;

public class Main {
	
	public static void main(String[] args) throws SizeIDCanBeSetOnlyOneTimeException, InvalidPortException, InvalidIPException, MustSetSizeIDException  {
		
		int m = Integer.parseInt(args[0]);//Number of bit Key
		int n = Integer.parseInt(args[1]);//Number of Node in the network
		int k = Integer.parseInt(args[2]);//Number of node in a single bucket
		
		Internet internetto = new Internet();
		System.out.println("M:"+m+" N:"+n+" K:"+k);
		System.out.println("Creo la rete Kademlia...");
		long time = System.nanoTime();
		
		// Set size for all node.
		Node.setSizeID(m);
		Node.setSizeBuckets(k);
		
		for(int i = 0; i < n; i++)
			new Node(internetto);

		time = System.nanoTime() - time;
		
		System.out.println((time / 1000000)+"millisec");
		System.out.println("Sto salvando la rete...");
		internetto.saveNetwork("m-"+m+"_n-"+n+"_k-"+k);
	}
}
