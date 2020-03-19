package node.exception.contact;
@SuppressWarnings("serial")
/**
 * Eccezione lanciata nel momento in cui il valore viene modificato una seconda
 * volta a runtime con un valore differente.
 */
public class SizeIDCanBeSetOnlyOneTimeException extends RuntimeException {}
