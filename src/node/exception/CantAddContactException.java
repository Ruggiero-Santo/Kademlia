package node.exception;
@SuppressWarnings("serial")
public class CantAddContactException extends RuntimeException {
	public CantAddContactException(String message) {
		super(message);
	}
}
