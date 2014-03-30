package indexer;

public class DocumentData {

	private String name;
	private Integer documentId;
	private String classAssignment;

	public DocumentData(String name, Integer documentId, String classAssignment) {
		this.setName(name);
		this.setDocumentId(documentId);
		this.setClassAssignment(classAssignment);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getDocumentId() {
		return documentId;
	}

	public void setDocumentId(Integer documentId) {
		this.documentId = documentId;
	}

	public String getClassAssignment() {
		return classAssignment;
	}

	public void setClassAssignment(String classAssignment) {
		this.classAssignment = classAssignment;
	}
}
