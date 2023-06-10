import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class GoogleStoreConnector implements StoreConnector{
	
	private final String BASE_URL_GOOGLE_PLAY = "https://play.google.com/store/apps/details?id=";
	private final String BASE_URL_APP_BRAIN = "https://www.appbrain.com/app/";
	private String storeUrl;
	
	
	private final String emptyString = "";
	private final String googlePlayPrefix = "\\[\\[\\[\"";
	private final String googlePlaySuffix = "\"]]";
	private final String appBrainPrefix = "<meta itemprop=\"softwareVersion\" content=\"";
	private final String appBrainSuffix = "\">";
	
	
	private String versionPrefix;
	private String versinSuffix;
	
	private String identifier;
	private String appName;
	private String appbrainPathName;
	public GoogleStoreConnector(String identifier, String appName, String appbrainPathName) {
		this.identifier = identifier;
		this.appName = appName;
		this.appbrainPathName = appbrainPathName;
	}
	
	public String connect(){
		String url = BASE_URL_GOOGLE_PLAY+identifier;
		String appVersion = connect(url);
		if(appVersion == null || appVersion.isEmpty()) { // in case GooglePlay multi-versions
			url = (appbrainPathName == null || appbrainPathName.isEmpty())? BASE_URL_APP_BRAIN+appbrainPathName+"/"+identifier: BASE_URL_APP_BRAIN+appName.toLowerCase()+"/"+identifier;
			appVersion = connect(url);
		}
		return appVersion;
	}
	
	public String connect(String urlString){
		storeUrl = urlString;
		acceptAllCerts();
		String storeVersion = "";
		try {
		URL url = new URL(urlString);
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
			return storeVersion;
			
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
	
	
	// extracting version from html
	public String parseStoreVersion(String htmlString) {
		
		if(storeUrl.contains(BASE_URL_GOOGLE_PLAY)) {
			// GooglePlay
			// [[["version_here"]]
			versionPrefix = googlePlayPrefix;
			versinSuffix = googlePlaySuffix;
		}else {
			// AppBrain
			// <meta itemprop="softwareVersion" content="version_here">
			versionPrefix = appBrainPrefix;
			versinSuffix = appBrainSuffix;
		}
		
		
	        for(int i=3 ; i>=0 ; i--) {
	        	String regex = versionPrefix + getRegex(i) + versinSuffix;
	            Pattern versionPattern = Pattern.compile(regex);
	            Matcher versionMatcher = versionPattern.matcher(htmlString);
	            if(versionMatcher.find()) {
	            	return versionMatcher.group()
	                        .replaceAll(versionPrefix, emptyString)
	                        .replaceAll(versinSuffix, emptyString);
	            	}
	            }
		
        return "";
	}
	
	private String getRegex(int count) {
		// range from xx.xx.xx.xx.xx to xx.xx
		String regex = "\\d+\\.\\d+";
		while(count > 0) {
			regex+="\\.\\d+";
			count--;
		}
		return regex;
	}
	
}
