package org.ggp.base.util.presence;

import java.io.IOException;

import org.ggp.base.server.request.RequestBuilder;
import org.ggp.base.util.http.HttpRequest;

import external.JSON.JSONException;
import external.JSON.JSONObject;

public class PlayerPresence {
	final private String host;
	final private int port;
	private String name;
	private String status;
	private long statusTime;
	
	PlayerPresence(String host, int port) {
		this.host = host;
		this.port = port;
		this.name = null;
		this.status = null;
		this.statusTime = 0;
	}
	
	public void updateInfo() {
		JSONObject info;
		String newName, newStatus;
		try {
			info = new JSONObject(HttpRequest.issueRequest(host, port, "", RequestBuilder.getInfoRequest(), 1000));
			newName = info.getString("name");
			newStatus = info.getString("status");
		} catch (JSONException je) {
			newName = null;
			newStatus = "error";			
		} catch (IOException e) {
			newName = null;
			newStatus = "error";
		}
		synchronized(this) {
			name = newName;
			status = newStatus;
			statusTime = System.currentTimeMillis();
		}
	}
	
	public synchronized String getName() {
		return name;
	}	
	
	public synchronized String getStatus() {
		return status;
	}
	
	public synchronized long getStatusAge() {
		return System.currentTimeMillis() - statusTime;
	}
	
	public String getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
}