package indexer;

public class IndexerParameters {
	
	public int minFreqThreshold;
	
	public int maxFreqThreshold;
	
	public boolean isStemmingOn;

	public IndexerParameters(boolean isStemmingOn, int minFreqThreshold, int maxFreqThreshold) {
		this.isStemmingOn = isStemmingOn;
		this.minFreqThreshold = minFreqThreshold;
		this.maxFreqThreshold = maxFreqThreshold;
	}
}
