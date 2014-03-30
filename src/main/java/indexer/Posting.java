package indexer;

public class Posting {

	private int documentId;
	private int position;

	public Posting(int documentId, int position) {
		this.documentId = documentId;
		this.position = position;
	}

	public int getDocumentId() {
		return documentId;
	}

	public void setDocumentId(int documentId) {
		this.documentId = documentId;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

}
