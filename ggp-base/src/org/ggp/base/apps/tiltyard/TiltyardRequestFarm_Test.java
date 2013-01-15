package org.ggp.base.apps.tiltyard;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import org.ggp.base.util.loader.RemoteResourceLoader;
import org.ggp.base.util.http.HttpReader;
import org.ggp.base.util.http.HttpWriter;

import junit.framework.TestCase;

import external.JSON.JSONObject;

public class TiltyardRequestFarm_Test extends TestCase {
	public void testThroughput() {
		runThroughputTest();
	}
	
	static long doMath(long a) {
		return a/2+3;
	}
	
    // Connections are run asynchronously in their own threads.
    class ResponderThread extends Thread {
    	private Socket conn;
    	
    	public ResponderThread(Socket connection) {
    		conn = connection;
    	}
    	
        @Override
        public void run() {
            try {
                String line = HttpReader.readAsServer(conn);
                Thread.sleep(20000); // 20s
                HttpWriter.writeAsServer(conn, "" + doMath(Long.parseLong(line)));
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    Integer nSuccesses = new Integer(0);
    
    // Connections are run asynchronously in their own threads.
    class ReceiverThread extends Thread {
    	private Socket conn;
    	
    	public ReceiverThread(Socket connection) {
    		conn = connection;
    	}
    	
        @Override
        public void run() {
            try {
                String line = HttpReader.readAsServer(conn);
                HttpWriter.writeAsServer(conn, "cool");
                conn.close();
                JSONObject responseJSON = new JSONObject(line);
                assertEquals("OK", responseJSON.getString("responseType"));
                long original = Long.parseLong(new JSONObject(responseJSON.getString("originalRequest")).getString("requestContent"));
            	long response = Long.parseLong(responseJSON.getString("response"));
            	assertEquals(response, doMath(original));
            	synchronized (nSuccesses) {
            		nSuccesses++;
            	}
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    class ResponderLoopThread extends Thread {
        @Override
        public void run() {
            try {
            	ServerSocket listener = new ServerSocket(12345);
                while (true) {
                    try {
                        Socket connection = listener.accept();
                        new ResponderThread(connection).start();
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
        @Override
        public void run() {
            try {
            	ServerSocket listener = new ServerSocket(12346);
                while (true) {
                    try {
                        Socket connection = listener.accept();
                        new ReceiverThread(connection).start();
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
    
    public void runThroughputTest() {
    	try {
	    	new ResponderLoopThread().start();
	    	new ReceiverLoopThread().start();
	    	new RequestFarmLoopThread().start();
	    	
	    	Random r = new Random();
    		JSONObject theRequest = new JSONObject();
    		theRequest.put("targetPort", 12345);
    		theRequest.put("targetHost", "127.0.0.1");
    		theRequest.put("timeoutClock", 30000);
    		theRequest.put("callbackURL", "http://127.0.0.1:12346");
    		theRequest.put("forPlayerName", "");
	    	
	    	int nRequests = 0;
	    	while (true) {
	    		theRequest.put("requestContent", "" + r.nextLong());
	    		theRequest.put("timestamp", System.currentTimeMillis());
	    		assertEquals("okay", RemoteResourceLoader.postRawWithTimeout("http://127.0.0.1:" + TiltyardRequestFarm.SERVER_PORT, theRequest.toString(), 5000));
	    		nRequests++;
	    		Thread.sleep(10);
	    		synchronized (nSuccesses) {
	    			System.out.println("Successes so far: " + nSuccesses + " vs Requests: " + nRequests);
	    			if (nSuccesses > 10000) {
	    				assertTrue(nRequests > nSuccesses);
	    				break;
	    			}
	    		}
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
}