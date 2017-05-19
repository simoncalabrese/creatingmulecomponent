package creatingmulecomponent;

public class ParallelForEachException extends Exception {

	private static final long serialVersionUID = -8817059489050724678L;

	public ParallelForEachException() {
		super();
	}
	
	public ParallelForEachException(final Exception e) {
		super(e);
	}

	public ParallelForEachException(final String customMsg) {
		super(customMsg);
	}
}
