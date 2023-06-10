import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.json.JSONObject;

public class AppleStoreConnector implements StoreConnector{
	
	private final String BASE_URL_APP_STORE = "https://itunes.apple.com/lookup?bundleId=";
	private String storeUrl = BASE_URL_APP_STORE;
	
	private final String jsonKeyResults = "results";
	private final String jsonKeyVersion = "version";
	
	private String identifier;
	public AppleStoreConnector(String identifier) {
		this.identifier = identifier;
	}
	
	public String connect(){
		acceptAllCerts();
		String storeVersion = "";
		try {
		URL url = new URL(storeUrl+identifier);
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.connect();

		if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			storeVersion = parseStoreVersion(response.toString());
			
		} else {
			connect(); // try forever
			System.out.println("GET request did not work.");
		}
		
		}catch(Exception e) {
			connect(); // try forever
			System.out.println("Exception: "+ e.getMessage());
		}

		return storeVersion;
	}
	
	public void acceptAllCerts() {
		TrustManager[] trustAllCerts = new TrustManager[]{
			    new X509TrustManager() {
			        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			            return null;
			        }
			        public void checkClientTrusted(
			            java.security.cert.X509Certificate[] certs, String authType) {
			        }
			        public void checkServerTrusted(
			            java.security.cert.X509Certificate[] certs, String authType) {
			        }
			    }
			};

			try {
			    SSLContext sc = SSLContext.getInstance("SSL");
			    sc.init(null, trustAllCerts, new java.security.SecureRandom());
			    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			} catch (Exception e) {
			}
	}
	
	// extracting version from json
	public String parseStoreVersion(String jsonString) {
		JSONObject jsonObject = new JSONObject(jsonString);
		String version = jsonObject.getJSONArray(jsonKeyResults).getJSONObject(0).getString(jsonKeyVersion); 
		System.out.println(version);
		return version;
	}

}
