package org.ggp.base.player;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.ggp.base.player.event.PlayerDroppedPacketEvent;
import org.ggp.base.player.event.PlayerReceivedMessageEvent;
import org.ggp.base.player.event.PlayerSentMessageEvent;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.player.gamer.statemachine.random.RandomGamer;
import org.ggp.base.player.request.factory.RequestFactory;
import org.ggp.base.player.request.grammar.Request;
import org.ggp.base.util.http.HttpReader;
import org.ggp.base.util.http.HttpWriter;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.observer.Subject;


public final class GamePlayer extends Thread implements Subject
{
    private final int port;
    private final Gamer gamer;
    private ServerSocket listener;
    private final List<Observer> observers;

    public GamePlayer(int port, Gamer gamer) throws IOException
    {
        observers = new ArrayList<Observer>();
        listener = null;

        while(listener == null) {
            try {
                listener = new ServerSocket(port);
            } catch (IOException ex) {
                listener = null;
                port++;
                System.err.println("Failed to start gamer on port: " + (port-1) + " trying port " + port);
            }
        }

        this.port = port;
        this.gamer = gamer;
    }

	@Override
	public void addObserver(Observer observer)
	{
		observers.add(observer);
	}

	@Override
	public void notifyObservers(Event event)
	{
		for (Observer observer : observers)
		{
			observer.observe(event);
		}
	}

	public final int getGamerPort() {
	    return port;
	}

	public final Gamer getGamer() {
	    return gamer;
	}

	public void shutdown() {
		try {
			listener.close();
			listener = null;
		} catch (IOException e) {
			;
		}
	}

	@Override
	public void run()
	{
		while (listener != null) {
			try {
				Socket connection = listener.accept();
				String in = HttpReader.readAsServer(connection);
				if (in.length() == 0) {
				    throw new IOException("Empty message received.");
				}

				notifyObservers(new PlayerReceivedMessageEvent(in));
				GamerLogger.log("GamePlayer", "[Received at " + System.currentTimeMillis() + "] " + in, GamerLogger.LOG_LEVEL_DATA_DUMP);

				Request request = new RequestFactory().create(gamer, in);
				String out = request.process(System.currentTimeMillis());

				HttpWriter.writeAsServer(connection, out);
				connection.close();
				notifyObservers(new PlayerSentMessageEvent(out));
				GamerLogger.log("GamePlayer", "[Sent at " + System.currentTimeMillis() + "] " + out, GamerLogger.LOG_LEVEL_DATA_DUMP);
			} catch (Exception e) {
				GamerLogger.log("GamePlayer", "[Dropped data at " + System.currentTimeMillis() + "] Due to " + e, GamerLogger.LOG_LEVEL_DATA_DUMP);
				notifyObservers(new PlayerDroppedPacketEvent());
			}
		}
	}

	// Simple main function that starts a RandomGamer on a specified port.
	// It might make sense to factor this out into a separate app sometime,
	// so that the GamePlayer class doesn't have to import RandomGamer.
	public static void main(String[] args)
	{
		if (args.length != 1) {
			System.err.println("Usage: GamePlayer <port>");
			System.exit(1);
		}

		try {
			GamePlayer player = new GamePlayer(Integer.valueOf(args[0]), new RandomGamer());
			player.run();
		} catch (NumberFormatException e) {
			System.err.println("Illegal port number: " + args[0]);
			e.printStackTrace();
			System.exit(2);
		} catch (IOException e) {
			System.err.println("IO Exception: " + e);
			e.printStackTrace();
			System.exit(3);
		}
	}
}