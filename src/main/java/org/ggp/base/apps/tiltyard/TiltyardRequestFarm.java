package org.ggp.base.apps.tiltyard;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.crypto.BaseCryptography.EncodedKeyPair;
import org.ggp.base.util.crypto.SignableJSON;
import org.ggp.base.util.files.FileUtils;
import org.ggp.base.util.http.HttpReader;
import org.ggp.base.util.http.HttpRequest;
import org.ggp.base.util.http.HttpWriter;
import org.ggp.base.util.loader.RemoteResourceLoader;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

/**
 * The Tiltyard Request Farm is a multi-threaded web server that opens network
 * connections, makes requests, and reports back responses on behalf of a remote
 * client. It serves as a backend for intermediary systems that, due to various
 * restrictions, cannot make long-lived HTTP connections themselves.
 *
 * This is the backend for the continuously-running online GGP.org Tiltyard,
 * which schedules matches between players around the world and aggregates stats
 * based on the outcome of those matches.
 *
 * SAMPLE INVOCATION (when running locally):
 *
 * ResourceLoader.load_raw('http://127.0.0.1:9124/' + escape(JSON.stringify({
 * "targetPort":9147,"targetHost":"0.player.ggp.org","timeoutClock":30000,
 * "forPlayerName":"Webplayer-0","callbackURL":"http://tiltyard.ggp.org/farm/",
 * "requestContent":"( play foo bar baz )"})))
 *
 * Tiltyard Request Farm will open up a network connection to the target, send
 * the request string, and wait for the response. Once the response arrives, it
 * will close the connection and call the callback, sending the response to the
 * remote client that issued the original request.
 *
 * You shouldn't be running this server unless you are bringing up an instance of the
 * online GGP.org Tiltyard or an equivalent service.
 *
 * @author Sam Schreiber
 */
public final class TiltyardRequestFarm
{
    public static final int SERVER_PORT = 9125;
    private static final String registrationURL = "http://tiltyard.ggp.org/backends/register/farm";

    private static final Object requestCountLock = new Object();
    private static int activeBatches = 0;
    private static int outgoingRequests = 0;
    private static int returningRequests = 0;
    private static int abandonedBatches = 0;
    static void printBatchStats() {
    	System.out.println(new Date().getTime() + " [" + new Date() + "]: now " + activeBatches + " active batches, with " + outgoingRequests + " requests outgoing, " + returningRequests + " returning; " + abandonedBatches + " batches abandoned.");
    }

    public static boolean testMode = false;

    static EncodedKeyPair getKeyPair(String keyPairString) {
    	if (keyPairString == null)
    		return null;
        try {
            return new EncodedKeyPair(keyPairString);
        } catch (JSONException e) {
            return null;
        }
    }
    public static final EncodedKeyPair theBackendKeys = getKeyPair(FileUtils.readFileAsString(new File("src/main/java/org/ggp/base/apps/tiltyard/BackendKeys.json")));
    public static String generateSignedPing() {
    	String zone = null;
   		try {
   			Map<String, String> metadataRequestProperties = new HashMap<String, String>();
   			metadataRequestProperties.put("Metadata-Flavor", "Google");
			zone = RemoteResourceLoader.loadRaw("http://metadata/computeMetadata/v1/instance/zone", 1, metadataRequestProperties);
		} catch (IOException e1) {
			// If we can't acquire the request farm zone, just silently drop it.
		}

        JSONObject thePing = new JSONObject();
        try {
        	if (zone != null) thePing.put("zone", zone);
            thePing.put("lastTimeBlock", (System.currentTimeMillis() / 3600000));
            thePing.put("nextTimeBlock", (System.currentTimeMillis() / 3600000)+1);
            SignableJSON.signJSON(thePing, theBackendKeys.thePublicKey, theBackendKeys.thePrivateKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return thePing.toString();
    }

    static class RunSingleRequestThread extends Thread {
    	String targetHost, requestContent, forPlayerName;
    	int targetPort, timeoutClock;
    	boolean fastReturn;
    	JSONObject myResponse;

    	public RunSingleRequestThread(JSONObject theJSON) throws JSONException {
    		myResponse = new JSONObject();
    		myResponse.put("originalRequest", theJSON);
            targetPort = theJSON.getInt("targetPort");
            targetHost = theJSON.getString("targetHost");
            timeoutClock = theJSON.getInt("timeoutClock");
            forPlayerName = theJSON.getString("forPlayerName");
            requestContent = theJSON.getString("requestContent");
            if (theJSON.has("fastReturn")) {
            	fastReturn = theJSON.getBoolean("fastReturn");
            } else {
            	fastReturn = true;
            }
    	}

    	@Override
    	public void run() {
        	synchronized (requestCountLock) {
        		outgoingRequests++;
        		printBatchStats();
        	}
            long startTime = System.currentTimeMillis();
            try {
                try {
                	String response = HttpRequest.issueRequest(targetHost, targetPort, forPlayerName, requestContent, timeoutClock);
                	response = response.replaceAll("\\P{InBasic_Latin}", "");
                	myResponse.put("response", response);
                	myResponse.put("responseType", "OK");
                } catch (SocketTimeoutException te) {
                	myResponse.put("responseType", "TO");
                } catch (IOException ie) {
                	myResponse.put("responseType", "CE");
                }
            } catch (JSONException je) {
            	throw new RuntimeException(je);
            }
        	synchronized (requestCountLock) {
        		outgoingRequests--;
        		printBatchStats();
        	}
            long timeSpent = System.currentTimeMillis() - startTime;
            if (!fastReturn && timeSpent < timeoutClock) {
            	try {
					Thread.sleep(timeoutClock - timeSpent);
				} catch (InterruptedException e) {
					;
				}
            }
    	}

    	public JSONObject getResponse() {
    		return myResponse;
    	}
    }

    // Connections are run asynchronously in their own threads.
    static class RunBatchRequestThread extends Thread {
    	String originalRequest, callbackURL;
    	Set<RunSingleRequestThread> theRequestThreads;
    	Set<String> activeRequests;

        public RunBatchRequestThread(Socket connection, Set<String> activeRequests) throws IOException, JSONException {
            String line = HttpReader.readAsServer(connection);
            System.out.println(new Date().getTime() + " [" + new Date() + "] received batch request: " + line);

            String response = null;
            if (line.equals("ping")) {
                response = generateSignedPing();
            } else {
                synchronized (activeRequests) {
                	if (activeRequests.contains(line)) {
                		System.out.println("Got duplicate request; ignoring.");
                		connection.close();
                		return;
                	} else {
                		activeRequests.add(line);
                	}
                	this.activeRequests = activeRequests;
                }

                JSONObject theBatchJSON = new JSONObject(line);
                JSONArray theRequests = theBatchJSON.getJSONArray("requests");
                theRequestThreads = new HashSet<RunSingleRequestThread>();
                for (int i = 0; i < theRequests.length(); i++) {
                	JSONObject aRequest = theRequests.getJSONObject(i);
                	RunSingleRequestThread aRequestThread = new RunSingleRequestThread(aRequest);
                	theRequestThreads.add(aRequestThread);
                }
                callbackURL = theBatchJSON.getString("callbackURL");

                originalRequest = line;
                response = "okay";
            }

            HttpWriter.writeAsServer(connection, response);
            connection.close();
        }

        @Override
        public void run() {
        	if (originalRequest == null)
        		return;

        	synchronized (requestCountLock) {
        		activeBatches++;
        		printBatchStats();
        	}

        	// Start running all of the requests in the batch parallel.
            for (RunSingleRequestThread aRequestThread : theRequestThreads) {
            	aRequestThread.start();
            }

            // Wait for all of the requests to finish; aggregate them into a batch response.
            JSONObject responseJSON = new JSONObject();
            JSONArray responses = new JSONArray();
            for (RunSingleRequestThread aRequestThread : theRequestThreads) {
            	try {
					aRequestThread.join();
					responses.put(aRequestThread.getResponse());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            }
            try {
                responseJSON.put("responses", responses);
                if (!testMode) {
                	SignableJSON.signJSON(responseJSON, theBackendKeys.thePublicKey, theBackendKeys.thePrivateKey);
                }
            } catch (JSONException je) {
            	je.printStackTrace();
            	synchronized (requestCountLock) {
            		abandonedBatches++;
            		activeBatches--;
            		printBatchStats();
            	}
                synchronized (activeRequests) {
                	activeRequests.remove(originalRequest);
                }
            	return;
            }

            // Send the batch response back to the callback URL.
        	synchronized (requestCountLock) {
        		returningRequests++;
        		printBatchStats();
        	}
            int nPostAttempts = 0;
            while (true) {
            	try {
            		RemoteResourceLoader.postRawWithTimeout(callbackURL, responseJSON.toString(), Integer.MAX_VALUE);
            		break;
            	} catch (IOException ie) {
            		nPostAttempts++;
            		try {
						Thread.sleep(nPostAttempts < 10 ? 1000 : 15000);
					} catch (InterruptedException e) {
						;
					}
            	}
            }
        	synchronized (requestCountLock) {
        		returningRequests--;
        		activeBatches--;
        		printBatchStats();
        		if (activeBatches == 0) {
        			System.gc();
        			System.out.println("Garbage collecting since there are no active batches.");
        		}
        	}
            synchronized (activeRequests) {
            	activeRequests.remove(originalRequest);
            }
        }
    }

    static class TiltyardRegistration extends Thread {
        @Override
        public void run() {
            // Send a registration ping to Tiltyard every five minutes.
            while (true) {
                try {
                    RemoteResourceLoader.postRawWithTimeout(registrationURL, generateSignedPing(), 2500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(5 * 60 * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
       }
    }

    @SuppressWarnings("resource")
	public static void main(String[] args) {
        ServerSocket listener = null;
        try {
             listener = new ServerSocket(SERVER_PORT);
        } catch (IOException e) {
            System.err.println("Could not open server on port " + SERVER_PORT + ": " + e);
            e.printStackTrace();
            return;
        }
        if (!testMode) {
	        if (theBackendKeys == null) {
	            System.err.println("Could not load cryptographic keys for signing request responses.");
	            return;
	        }
	        new TiltyardRegistration().start();
        }

        Set<String> activeRequests = new HashSet<String>();
        while (true) {
            try {
                Socket connection = listener.accept();
                RunBatchRequestThread handlerThread = new RunBatchRequestThread(connection, activeRequests);
                handlerThread.start();
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }
}
