
public interface StoreConnector {
	
	String connect();
	void acceptAllCerts();
	String parseStoreVersion(String htmlString);

}
