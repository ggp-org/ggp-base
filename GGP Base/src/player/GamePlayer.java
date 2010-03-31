package player;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import player.event.PlayerDroppedPacketEvent;
import player.event.PlayerReceivedMessageEvent;
import player.event.PlayerSentMessageEvent;
import player.gamer.Gamer;
import player.request.factory.RequestFactory;
import player.request.grammar.Request;
import util.http.HttpReader;
import util.http.HttpWriter;
import util.logging.GamerLogger;
import util.observer.Event;
import util.observer.Observer;
import util.observer.Subject;

public final class GamePlayer extends Thread implements Subject
{
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
        
        this.gamer = gamer;
    }

	public void addObserver(Observer observer)
	{
		observers.add(observer);
	}

	public void notifyObservers(Event event)
	{
		for (Observer observer : observers)
		{
			observer.observe(event);
		}
	}

	@Override
	public void run()
	{
		while (!isInterrupted())
		{
			try
			{
				Socket connection = listener.accept();
				String in = HttpReader.readAsServer(connection);
				notifyObservers(new PlayerReceivedMessageEvent(in));
				GamerLogger.log("GamePlayer", "[Received at " + System.currentTimeMillis() + "] " + in, GamerLogger.LOG_LEVEL_DATA_DUMP);

				Request request = new RequestFactory().create(gamer, in);
				String out = request.process(System.currentTimeMillis());
				
				HttpWriter.writeAsServer(connection, out);
				connection.close();
				notifyObservers(new PlayerSentMessageEvent(out));
				GamerLogger.log("GamePlayer", "[Sent at " + System.currentTimeMillis() + "] " + out, GamerLogger.LOG_LEVEL_DATA_DUMP);
			}
			catch (Exception e)
			{
				notifyObservers(new PlayerDroppedPacketEvent());
			}
		}
	}
}