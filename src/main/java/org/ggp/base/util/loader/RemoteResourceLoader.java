package org.ggp.base.util.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

/**
 * RemoteResourceLoader loads remotely-stored resources. It can load resources
 * as raw strings, JSON objects, or JSON arrays.
 *
 * @author Sam
 */
public class RemoteResourceLoader {
    private RemoteResourceLoader() {
    }

    public static JSONObject loadJSON(String theURL) throws JSONException, IOException {
        return loadJSON(theURL, 1);
    }
    public static JSONObject loadJSON(String theURL, int nMaxAttempts) throws JSONException, IOException {
        return new JSONObject(loadRaw(theURL, nMaxAttempts));
    }

    public static JSONArray loadJSONArray(String theURL) throws JSONException, IOException {
        return loadJSONArray(theURL, 1);
    }
    public static JSONArray loadJSONArray(String theURL, int nMaxAttempts) throws JSONException, IOException {
        return new JSONArray(loadRaw(theURL, nMaxAttempts));
    }

    public static String loadRaw(String theURL) throws IOException {
        return loadRaw(theURL, 1);
    }
    public static String loadRaw(String theURL, int nMaxAttempts) throws IOException {
        return loadRaw(theURL, nMaxAttempts, null);
    }
    public static String loadRaw(String theURL, int nMaxAttempts, Map<String, String> requestProperties) throws IOException {
        int nAttempt = 0;
        while(true) {
            nAttempt++;
            try {
                URL url = new URL(theURL);
                URLConnection urlConnection = url.openConnection();
                urlConnection.setUseCaches(false);
                urlConnection.setDefaultUseCaches(false);
                urlConnection.addRequestProperty("Cache-Control", "no-cache,max-age=0");
                urlConnection.addRequestProperty("Pragma", "no-cache");
                if (requestProperties != null) {
                    for (String key : requestProperties.keySet()) {
                        urlConnection.addRequestProperty(key, requestProperties.get(key));
                    }
                }
                if (urlConnection.getContentLength() == 0)
                    throw new IOException("Could not load URL: " + theURL);
                StringBuilder theRawData = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                    do {
                        String nextLine = br.readLine();
                        if (nextLine == null) break;
                        theRawData.append(nextLine + "\n");
                    } while (true);
                }
                return theRawData.toString();
            } catch (IOException ie) {
                if (nAttempt >= nMaxAttempts) {
                    throw ie;
                }
            }
        }
    }

    public static String postRawWithTimeout(String theURL, String toPost, int nTimeout) throws IOException {
        URL url = new URL(theURL);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setConnectTimeout(nTimeout);
        OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
        out.write(toPost);
        out.close();

        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        StringBuilder decodedString = new StringBuilder();
        String decodedLine;
        while ((decodedLine = in.readLine()) != null) {
            decodedString.append(decodedLine);
        }
        in.close();
        return decodedString.toString();
    }
}