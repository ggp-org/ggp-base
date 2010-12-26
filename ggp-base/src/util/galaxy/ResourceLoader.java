package util.galaxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import external.JSON.JSONArray;
import external.JSON.JSONObject;

public class ResourceLoader {
    public static JSONObject loadJSON(String theURL) throws IOException {
        try {
            return new JSONObject(loadRaw(theURL));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
    
    public static JSONArray loadJSONArray(String theURL) throws IOException {
        try {
            return new JSONObject("{ 'z': " + loadRaw(theURL) + "}").getJSONArray("z");
        } catch (Exception e) {
            throw new IOException(e);
        }                
    }
    
    public static String loadRaw(String theURL) throws IOException {
        URL url = new URL(theURL);
        URLConnection urlConnection = url.openConnection();                
        if (urlConnection.getContentLength() == 0)
            throw new IOException("Could not load URL: " + theURL);
        StringBuilder theJSON = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        do {
            String nextLine = br.readLine();
            if (nextLine == null) break;
            theJSON.append(nextLine);
        } while (true);
        return theJSON.toString();
    }
    
    public static InputStream openInputStreamToURL(String theURL) throws IOException {
        URL url = new URL(theURL);
        URLConnection urlConnection = url.openConnection();                
        if (urlConnection.getContentLength() == 0)
            throw new IOException("Could not load URL: " + theURL);
        return urlConnection.getInputStream();
    }
}