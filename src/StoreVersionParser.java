import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class StoreVersionParser {
	
	private static final String PLATFORM_ANDROID = "Android";
	private static final String PLATFORM_IOS = "iOS";
	
	private static final String ANDROID_INPUT_PATH = "input/android_apps.txt";
	private static final String IOS_INPUT_PATH = "input/ios_apps.txt";
	
	private static final String ANDROID_OUTPUT_PATH = "output/android_store_versions.txt";
	private static final String IOS_OUTPUT_PATH = "output/ios_store_versions.txt";
	private static final String HISTORY_OUTPUT_PATH = "output/versions_history.txt";

	
	private static JSONArray androidInputArray;
	private static JSONArray iosInputArray;
	private static JSONObject historyJsonMap;
	
	private static final String JSON_KEY_STORE_ID = "store_id";
	private static final String JSON_KEY_APP_NAME = "app_name";
	private static final String JSON_KEY_APP_BRAIN_PATH_NAME = "appbrain_path_name";
	
	public static void main(String[] args) {
		
		String historyString = readFileContent(HISTORY_OUTPUT_PATH);
		
		historyJsonMap = (historyString.isEmpty() || historyString == null)? new JSONObject(): new JSONObject(historyString);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				String androidInputString = readFileContent(ANDROID_INPUT_PATH);
				androidInputArray = new JSONArray(androidInputString);
				getStoreVersion(PLATFORM_ANDROID, ANDROID_OUTPUT_PATH, androidInputArray);
			}
		}).start();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				String iosInputString = readFileContent(IOS_INPUT_PATH);
				iosInputArray = new JSONArray(iosInputString);
				getStoreVersion(PLATFORM_IOS, IOS_OUTPUT_PATH, iosInputArray);
				
			}
		}).start();
		
	}
	
	private static void getStoreVersion(String platform, String outputFilePath, JSONArray inputJsonArray) {
		int index = 0;
		String allVersions = "";
		JSONObject jsonOutputObject = new JSONObject();
		while(index < inputJsonArray.length()) {
			JSONObject jsonInputObject = inputJsonArray.getJSONObject(index);
			String identifier = jsonInputObject.getString(JSON_KEY_STORE_ID);
			String appName = jsonInputObject.getString(JSON_KEY_APP_NAME);
			String appbrainPathName = (jsonInputObject.has(JSON_KEY_APP_BRAIN_PATH_NAME))? jsonInputObject.getString(JSON_KEY_APP_BRAIN_PATH_NAME):null;
			System.out.println("Getting "+platform+" version for app "+ appName);
			StoreConnector storeConnector = (platform.equals(PLATFORM_ANDROID))? new GoogleStoreConnector(identifier, appName, appbrainPathName): new AppleStoreConnector(identifier);
			String version = storeConnector.connect();
			if(version != null) {
			jsonOutputObject.put(appName, version);
			appendVersionToHistory(appName, version, platform, historyJsonMap);
			allVersions +=  version + " (" + appName + ")" + "\n";
			index++;
			}
		}
		
		writeToFile(outputFilePath, jsonOutputObject.toString());	
		
		writeToFile(HISTORY_OUTPUT_PATH, historyJsonMap.toString());
		
		System.out.println("\nAll "+platform+" versions\n"+allVersions);
	}
	
	
	private static void writeToFile(String filePath, String text) {
		try {	
		File file = new File(filePath); 
		file.createNewFile();
	    BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
	    writer.write(text);
	    writer.flush();
	    writer.close();
		}catch(Exception e) {
			System.out.println("Write exception: "+e.getMessage());
		}
	}
	
	private static String readFileContent(String filePath) {
		File file = new File(filePath); 
		try {
			file.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] bytes = null;
		try {
			bytes = Files.readAllBytes(Paths.get(filePath));
		} catch (IOException e) {
			bytes = null;
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}
	
	private static void appendVersionToHistory(String appName, String version, String platform, JSONObject historyJsonMap) {
		if(!historyJsonMap.has(appName)) {
			historyJsonMap.put(appName, new JSONObject());
		}
		if(historyJsonMap.getJSONObject(appName).has(platform.toLowerCase())) {
			if(!historyJsonMap.getJSONObject(appName).getString(platform.toLowerCase()).contains(version)) {
			version = historyJsonMap.getJSONObject(appName).getString(platform.toLowerCase())+","+version;
			}else {
				return;
			}
		}
		historyJsonMap.getJSONObject(appName).put(platform.toLowerCase(), version);
	}
	
	

}
