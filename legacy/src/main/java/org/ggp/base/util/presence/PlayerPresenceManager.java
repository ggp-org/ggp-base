package org.ggp.base.util.presence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.observer.Subject;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

public class PlayerPresenceManager implements Subject {
	private Map<String,PlayerPresence> monitoredPlayers;

	public class PlayerPresenceChanged extends Event {}
	public class PlayerPresenceAdded extends Event {}
	public class PlayerPresenceRemoved extends Event {}

	public static boolean isDifferent(String a, String b) {
		return !Objects.equals(a, b);
	}

	public static final int INFO_PING_PERIOD_IN_SECONDS = 1;
	class PresenceMonitor extends Thread {
		@Override
		public void run() {
			while(true) {
				try {
					Thread.sleep(INFO_PING_PERIOD_IN_SECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Set<String> keys = new HashSet<String>(monitoredPlayers.keySet());
				for (String key : keys) {
					PlayerPresence presence = monitoredPlayers.get(key);
					if (presence == null) continue;
					if (presence.getStatusAge() > INFO_PING_PERIOD_IN_SECONDS*1000) {
						String old_name = presence.getName();
						String old_status = presence.getStatus();
						presence.updateInfo();
						String new_name = presence.getName();
						String new_status = presence.getStatus();
						if (isDifferent(old_status, new_status)) {
							notifyObservers(new PlayerPresenceChanged());
						} else if (isDifferent(old_name, new_name)) {
							notifyObservers(new PlayerPresenceChanged());
						}
					}
				}
			}
		}
	}

	public PlayerPresenceManager() {
		monitoredPlayers = new HashMap<String,PlayerPresence>();
		loadPlayersJSON();
		if (monitoredPlayers.size() == 0) {
			try {
				// When starting from a blank slate, add some initial players to the
				// monitoring list just so that it's clear how it works.
				addPlayer("127.0.0.1:9147");
				addPlayer("127.0.0.1:9148");
			} catch (InvalidHostportException e) {
				;
			}
		}
		new PresenceMonitor().start();
	}

	@SuppressWarnings("serial")
	public class InvalidHostportException extends Exception {}

	private PlayerPresence addPlayerSilently(String hostport) throws InvalidHostportException {
		try {
			if (!monitoredPlayers.containsKey(hostport)) {
				String host = hostport.split(":")[0];
				int port = Integer.parseInt(hostport.split(":")[1]);
				PlayerPresence presence = new PlayerPresence(host, port);
				monitoredPlayers.put(hostport, presence);
				return presence;
			} else {
				return monitoredPlayers.get(hostport);
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new InvalidHostportException();
		} catch (NumberFormatException e) {
			throw new InvalidHostportException();
		}
	}

	public PlayerPresence addPlayer(String hostport) throws InvalidHostportException {
		PlayerPresence presence = addPlayerSilently(hostport);
		notifyObservers(new PlayerPresenceAdded());
		savePlayersJSON();
		return presence;
	}

	public void removePlayer(String hostport) {
		monitoredPlayers.remove(hostport);
		notifyObservers(new PlayerPresenceRemoved());
		savePlayersJSON();
	}

	public PlayerPresence getPresence(String hostport) {
		return monitoredPlayers.get(hostport);
	}

	public Set<String> getSortedPlayerNames() {
		return new TreeSet<String>(monitoredPlayers.keySet());
	}

	private Set<Observer> observers = new HashSet<Observer>();
	@Override
	public void addObserver(Observer observer) {
		observers.add(observer);
	}

	@Override
	public void notifyObservers(Event event) {
		for (Observer observer : observers) {
			observer.observe(event);
		}
	}

	private static final String playerListFilename = ".ggpserver-playerlist.json";
	private void savePlayersJSON() {
		try {
			JSONObject playerListJSON = new JSONObject();
			playerListJSON.put("hostports", monitoredPlayers.keySet());
			File file = new File(System.getProperty("user.home"), playerListFilename);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(playerListJSON.toString());
			bw.close();
		} catch (IOException ie) {
			ie.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void loadPlayersJSON() {
		try {
			String line;
			StringBuilder pdata = new StringBuilder();
			File file = new File(System.getProperty("user.home"), playerListFilename);
			if (!file.exists()) {
				return;
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
			try {
				while ((line = br.readLine()) != null) {
					pdata.append(line);
				}
			} finally {
				br.close();
			}
			JSONObject playerListJSON = new JSONObject(pdata.toString());
			if (playerListJSON.has("hostports")) {
				JSONArray theHostports = playerListJSON.getJSONArray("hostports");
				for (int i = 0; i < theHostports.length(); i++) {
					try {
						addPlayerSilently(theHostports.get(i).toString());
					} catch (InvalidHostportException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (IOException ie) {
			ie.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}