package org.ggp.base.apps.tiltyard;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.ggp.base.util.http.HttpReader;
import org.ggp.base.util.http.HttpWriter;
import org.ggp.base.util.loader.RemoteResourceLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import external.JSON.JSONArray;
import external.JSON.JSONObject;

public class TiltyardRequestFarmTest extends Assert {
	@Before
	public void setUp() {
		new RequestFarmLoopThread().start();
	}

	@Test
	public void testThroughput() {
    	new ResponderLoopThread(2000).start();
    	new ReceiverLoopThread("OK").start();
    	runTestingLoop();
	}

	/* TODO(schreib): Get all of these working at the same time.

	@Test
	public void testConnectionError() {
    	new ReceiverLoopThread("CE").start();
    	runTestingLoop();
	}

    @Test
	public void testTimeout() {
    	new ResponderLoopThread(4000).start();
    	new ReceiverLoopThread("TO").start();
    	runTestingLoop();
	}

	*/

	static long doMath(long a) {
		return a/2+3;
	}

    // Connections are run asynchronously in their own threads.
    class ResponderThread extends Thread {
    	private Socket conn;
    	private int sleepTime;

    	public ResponderThread(Socket connection, int sleepTime) {
    		conn = connection;
    		this.sleepTime = sleepTime;
    	}

        @Override
        public void run() {
            try {
                String line = HttpReader.readAsServer(conn);
                Thread.sleep(sleepTime);
                HttpWriter.writeAsServer(conn, "" + doMath(Long.parseLong(line)));
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    AtomicInteger nSuccesses = new AtomicInteger(0);

    // Connections are run asynchronously in their own threads.
    class ReceiverThread extends Thread {
    	private Socket conn;
    	private String response;

    	public ReceiverThread(Socket connection, String expectedResponse) {
    		conn = connection;
    		response = expectedResponse;
    	}

        @Override
        public void run() {
            try {
                String line = HttpReader.readAsServer(conn);
                HttpWriter.writeAsServer(conn, "cool");
                conn.close();
                JSONObject batchResponseJSON = new JSONObject(line);
                assertTrue(batchResponseJSON.has("responses"));
                assertEquals(2, batchResponseJSON.getJSONArray("responses").length());
                JSONObject responseJSON = batchResponseJSON.getJSONArray("responses").getJSONObject(0);
                assertEquals(response, responseJSON.getString("responseType"));
                if (responseJSON.getString("responseType").equals("OK")) {
                    long original = Long.parseLong(new JSONObject(responseJSON.getString("originalRequest")).getString("requestContent"));
                	long response = Long.parseLong(responseJSON.getString("response"));
                	assertEquals(response, doMath(original));
                }
                nSuccesses.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class ResponderLoopThread extends Thread {
    	private int sleepTime;
    	public ResponderLoopThread(int sleepTime) {
    		this.sleepTime = sleepTime;
    	}

        @Override
        public void run() {
            try {
            	@SuppressWarnings("resource")
				ServerSocket listener = new ServerSocket(12345);
                while (true) {
                    try {
                        Socket connection = listener.accept();
                        new ResponderThread(connection, sleepTime).start();
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class ReceiverLoopThread extends Thread {
    	private String response;
    	public ReceiverLoopThread(String expectResponse) {
    		response = expectResponse;
    	}

        @Override
        public void run() {
            try {
            	@SuppressWarnings("resource")
				ServerSocket listener = new ServerSocket(12346);
                while (true) {
                    try {
                        Socket connection = listener.accept();
                        new ReceiverThread(connection, response).start();
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class RequestFarmLoopThread extends Thread {
        @Override
        public void run() {
            try {
            	TiltyardRequestFarm.testMode = true;
            	TiltyardRequestFarm.main(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void runTestingLoop() {
    	try {
	    	Random r = new Random();
    		JSONObject theBatchRequest = new JSONObject();
    		JSONArray theRequests = new JSONArray();
    		JSONObject theRequest = new JSONObject();
    		theRequest.put("targetPort", 12345);
    		theRequest.put("targetHost", "127.0.0.1");
    		theRequest.put("timeoutClock", 3000);
    		theRequest.put("forPlayerName", "");
    		theRequests.put(theRequest);
    		theRequests.put(theRequest);
    		theBatchRequest.put("requests", theRequests);
    		theBatchRequest.put("callbackURL", "http://127.0.0.1:12346");

	    	int nRequests = 0;
	    	while (true) {
	    		theRequest.put("requestContent", "" + r.nextLong());
	    		theRequest.put("timestamp", System.currentTimeMillis());
	    		assertEquals("okay", RemoteResourceLoader.postRawWithTimeout("http://127.0.0.1:" + TiltyardRequestFarm.SERVER_PORT, theBatchRequest.toString(), 5000));
	    		nRequests++;
	    		Thread.sleep(10);
	    		int curNumSuccesses = nSuccesses.get();
	    		System.out.println("Successes so far: " + curNumSuccesses + " vs Requests: " + nRequests);
	    		if (curNumSuccesses > 3000) {
	    			assertTrue(nRequests > curNumSuccesses);
	    			break;
	    		}
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
}