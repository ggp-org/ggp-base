package org.ggp.base.apps.tourney;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.player.GamePlayer;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.util.match.Match;


/**
 * TourneyManager is the thread that runs a fixed number of matches
 * between predetermined players on a single game. All of the players
 * are started beforehand, and they stay on the same port through the
 * entire tournament.
 * 
 * @author Sam Schreiber
 */
public final class TourneyManager extends Thread
{
    private static final int DEFAULT_GAME_PORT = 9147;
    
    private List<Class<?>> thePlayers;
    private TourneyEventsPanel thePanel;
    private Match theMatchModel;    
    private int nRepetitions;
    private String gameName;

    public TourneyManager(List<Class<?>> thePlayers, Match theMatchModel, String gameName, int numReps, TourneyEventsPanel thePanel) {
        this.theMatchModel = theMatchModel;
        this.thePlayers = thePlayers;
        this.thePanel = thePanel;
        this.nRepetitions = numReps;
        this.gameName = gameName;
    }

    @Override
    public void run()
    {
        try {
            // Start all of the gamers
            int nextPortToTry = DEFAULT_GAME_PORT;
            int playerIndex = 0;
            List<String> hosts = new ArrayList<String>(thePlayers.size());
            List<String> names = new ArrayList<String>(thePlayers.size());
            List<Integer> ports = new ArrayList<Integer>(thePlayers.size());
            for (int i = 0; i < thePlayers.size(); i++) {
                Gamer gamer = null;
                Class<?> gamerClass = thePlayers.get(playerIndex++);
                try {
                    gamer = (Gamer) gamerClass.newInstance();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
    
                GamePlayer player = new GamePlayer(nextPortToTry, gamer);            
                player.start();
                
                hosts.add("localhost");
                names.add(player.getName());
                ports.add(new Integer(player.getGamerPort()));            
                System.out.println("Tourney successfully started " + gamer.getName() + " on port " + player.getGamerPort() + ".");
                
                nextPortToTry = player.getGamerPort() + 1;
            }

            // Run through the matches
            for (int nRound = 0; nRound < nRepetitions; nRound++) {                
                // Configure parameters...
                TourneyEvent theNextEvent = new TourneyEvent(gameName, theMatchModel, ports.size());
                thePanel.setCurrentEvent(theNextEvent);                
                theNextEvent.addObserver(thePanel);
                
                // ...and run the event!
                theNextEvent.runEvent(hosts, names, ports);

                // And get ready to repeat.
                Thread.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}