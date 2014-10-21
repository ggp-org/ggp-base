package org.ggp.base.util.presence;

import java.io.IOException;

import org.ggp.base.server.request.RequestBuilder;
import org.ggp.base.util.http.HttpRequest;

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
		InfoResponse info;
		try {
			String infoFull = HttpRequest.issueRequest(host, port, "", RequestBuilder.getInfoRequest(), 1000);
			info = InfoResponse.create(infoFull);
		} catch (IOException e) {
			info = new InfoResponse();
			info.setName(null);
			info.setStatus("error");
		}
		synchronized(this) {
			name = info.getName();
			status = info.getStatus();
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