package org.ggp.base.apps.kiosk;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import org.ggp.base.player.GamePlayer;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.player.gamer.exception.AbortingException;
import org.ggp.base.server.GameServer;
import org.ggp.base.server.event.ServerConnectionErrorEvent;
import org.ggp.base.server.event.ServerIllegalMoveEvent;
import org.ggp.base.server.event.ServerTimeoutEvent;
import org.ggp.base.util.game.CloudGameRepository;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.reflection.ProjectSearcher;
import org.ggp.base.util.symbol.grammar.SymbolPool;
import org.ggp.base.util.ui.*;

/**
 * Kiosk is a program for running two-player human-vs-computer matches
 * with clean visualizations and intuitive human interfaces. Originally
 * designed for running matches against players implemented using the
 * standard Java stack, it can also connect to remote players as need be.
 * 
 * @author Sam
 */
@SuppressWarnings("serial")
public final class Kiosk extends JPanel implements ActionListener, ItemListener, Observer
{
    public static final String remotePlayerString = "[REMOTE PLAYER]";
    
    private static void createAndShowGUI(Kiosk serverPanel)
    {
        JFrame frame = new JFrame("Gaming Kiosk");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(serverPanel);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args)
    {
        NativeUI.setNativeUI();
        final Kiosk serverPanel = new Kiosk();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(serverPanel);
            }
        });
    }

    private final JPanel managerPanel;

    private final JTextField playClockTextField;
    private final JTextField startClockTextField;
    
    private final JButton runButton;
    private final JList selectedGame;
    private final JCheckBox flipRoles;
    
    private final PublishButton publishButton;

    private final JPanel theGUIPanel;
        
    private final JComboBox playerComboBox;
    private List<Class<?>> gamers = null;
    private final JTextField computerAddress;

    private final GameRepository theRepository;
    
    public Kiosk()
    {        
        super(new GridBagLayout());   
        setPreferredSize(new Dimension(1050, 900));

        NativeUI.setNativeUI();
        GamerLogger.setFileToDisplay("GamePlayer");
        
        SortedSet<AvailableGame> theAvailableGames = new TreeSet<AvailableGame>();
        List<Class<?>> theAvailableCanvasList = ProjectSearcher.getAllClassesThatAre(GameCanvas.class);
        for(Class<?> availableCanvas : theAvailableCanvasList) {
            try {
                GameCanvas theCanvas = (GameCanvas) availableCanvas.newInstance();                
                theAvailableGames.add(new AvailableGame(theCanvas.getGameName(), theCanvas.getGameKey(), availableCanvas));
            } catch(Exception e) {
                ;
            }
        }
        
        flipRoles = new JCheckBox("Flip roles?");
        
        selectedGame = new JList(theAvailableGames.toArray());
        selectedGame.setSelectedIndex(0);
        selectedGame.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane selectedGamePane = new JScrollPane(selectedGame);

        computerAddress = new JTextField("player.ggp.org:80");        
        playerComboBox = new JComboBox();
        playerComboBox.addItemListener(this);

        gamers = ProjectSearcher.getAllClassesThatAre(Gamer.class);
        List<Class<?>> gamersCopy = new ArrayList<Class<?>>(gamers);            
        for(Class<?> gamer : gamersCopy)
        {
            try {
                Gamer g = (Gamer) gamer.newInstance();
                if (!g.isComputerPlayer()) {
                	throw new Exception("Kiosk only considers computer players");
                }
                playerComboBox.addItem(g.getName());
            } catch(Exception ex) {
                gamers.remove(gamer);
            }
        }
        playerComboBox.setSelectedItem("Random");
        playerComboBox.addItem(remotePlayerString);        
        
        runButton = new JButton("Run!");
        runButton.addActionListener(this);

        publishButton = new PublishButton("Publish online!");
        
        startClockTextField = new JTextField("30");
        playClockTextField = new JTextField("10");
        managerPanel = new JPanel(new GridBagLayout());
        
        startClockTextField.setColumns(15);
        playClockTextField.setColumns(15);

        int nRowCount = 1;
        managerPanel.setBorder(new TitledBorder("Kiosk Control"));
        managerPanel.add(new JLabel("Opponent:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(playerComboBox, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(computerAddress, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Start Clock:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(startClockTextField, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Play Clock:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(playClockTextField, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(flipRoles, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Game:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(selectedGamePane, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 5.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Publishing:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(publishButton, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        //managerPanel.add(new ConsolePanel(), new GridBagConstraints(0, nRowCount++, 2, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));        
        managerPanel.add(runButton, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

        JPanel gamePanel = new JPanel(new GridBagLayout());
        gamePanel.setBorder(new TitledBorder("Game Kiosk"));

        theGUIPanel = new JPanel();
        gamePanel.add(theGUIPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));

        this.add(managerPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
        this.add(gamePanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
        
        // Start up the gamers!
        try {
            theHumanGamer = new KioskGamer(theGUIPanel);
            theHumanPlayer = new GamePlayer(DEFAULT_HUMAN_PORT, theHumanGamer);
            theHumanPlayer.start();
        } catch(Exception e) {
            e.printStackTrace();
        }

        // This is where we get the rulesheets from. Each game has a corresponding
        // game (with rulesheet) stored on this repository server. Changing this is
        // likely to break things unless you know what you're doing.
        theRepository = new CloudGameRepository("http://games.ggp.org/base/");        
    }
    
    class AvailableGame implements Comparable<AvailableGame> {
        private String gameName, kifFile;
        private Class<?> theCanvasClass;
        
        public AvailableGame(String gameName, String kifFile, Class<?> theCanvasClass) {
            this.gameName = gameName;
            this.kifFile = kifFile;
            this.theCanvasClass = theCanvasClass;
        }
        
        public String toString() {
            return gameName;
        }
        
        public GameCanvas getCanvas() {
            try {
                return (GameCanvas)theCanvasClass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public int compareTo(AvailableGame o) {
            return gameName.compareTo(o.gameName);
        }
    }    
    
    private GamePlayer theComputerPlayer = null;
    private GamePlayer theHumanPlayer = null;
    private KioskGamer theHumanGamer;
    
    private GameServer kioskServer = null;
    
    private static final int DEFAULT_HUMAN_PORT = 3333;
    private static final int DEFAULT_COMPUTER_PORT = 3334;
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == runButton) {
            if (kioskServer != null && !kioskServer.getMatch().isCompleted()) {
            	// When a match is started while another match is still running,
            	// abort the ongoing match so that the new one can start. This
            	// also directly tells the human gamer to abort, since otherwise
            	// it will wait indefinitely for the human to submit their last
            	// move before it processes the abort message from the server,
            	// since messages are processed serially, and the human gamer
            	// has no time limits for processing play messages.
            	kioskServer.abort();
            	kioskServer = null;
            	try {
            		theHumanGamer.abort();
            	} catch (AbortingException ae) {
            		ae.printStackTrace();
            	}
            	System.gc();
            }
            
            try {
                GdlPool.drainPool();
                SymbolPool.drainPool();
                
                AvailableGame theGame = (AvailableGame) (selectedGame.getSelectedValue());
                Game game = theRepository.getGame(theGame.kifFile);
                
                if (game == null) {
                	JOptionPane.showMessageDialog(this, "Cannot load game data for " + theGame.kifFile, "Error", JOptionPane.ERROR_MESSAGE);
                	return;                	
                }
                
                String matchId = "kiosk." + theGame.kifFile + "-" + System.currentTimeMillis();
                int startClock = Integer.valueOf(startClockTextField.getText());
                int playClock = Integer.valueOf(playClockTextField.getText());
                Match match = new Match(matchId, -1, startClock, playClock, game);
                theHumanGamer.setCanvas(theGame.getCanvas());

                // Stop old player if it's not the right type
                String computerPlayerName = (String) playerComboBox.getSelectedItem();
                if(theComputerPlayer != null && !theComputerPlayer.getGamer().getName().equals(computerPlayerName)) {
                    theComputerPlayer.interrupt();
                    Thread.sleep(100);
                    theComputerPlayer = null;
                }

                // Start a new player if necessary
                if(theComputerPlayer == null) {
                    Gamer gamer = null;                    
                    if(!playerComboBox.getSelectedItem().equals(remotePlayerString)) {
                        Class<?> gamerClass = gamers.get(playerComboBox.getSelectedIndex());
                        try {
                            gamer = (Gamer) gamerClass.newInstance();
                        } catch(Exception ex) { throw new RuntimeException(ex); }
                        theComputerPlayer = new GamePlayer(DEFAULT_COMPUTER_PORT, gamer);
                        theComputerPlayer.start();
                        System.out.println("Kiosk has started a gamer named " + theComputerPlayer.getGamer().getName() + ".");                        
                    }
                }

                List<String> hosts = new ArrayList<String>();
                List<Integer> ports = new ArrayList<Integer>();
                List<String> playerNames = new ArrayList<String>();

                if(!flipRoles.isSelected()) {
                    hosts.add("127.0.0.1");
                    ports.add(theHumanPlayer.getGamerPort());
                    playerNames.add("Human");                                   
                }
                
                if(playerComboBox.getSelectedItem().equals(remotePlayerString)) {
                    try {
                        String[] splitAddress = computerAddress.getText().split(":");
                        String hostname = splitAddress[0];
                        int port = Integer.parseInt(splitAddress[1]);
                        
                        hosts.add(hostname);
                        ports.add(port);                    
                        playerNames.add("Computer");                    
                    } catch(Exception ex) {
                        ex.printStackTrace();
                        return;
                    }                    
                } else {
                    hosts.add("127.0.0.1");
                    ports.add(theComputerPlayer.getGamerPort());                    
                    playerNames.add("Computer");
                }

                if(flipRoles.isSelected()) {
                    hosts.add("127.0.0.1");
                    ports.add(theHumanPlayer.getGamerPort());
                    playerNames.add("Human");                                   
                }
                
                match.setPlayerNamesFromHost(playerNames);
                
                GamerLogger.startFileLogging(match, "kiosk");
                kioskServer = new GameServer(match, hosts, ports);
                kioskServer.givePlayerUnlimitedTime((flipRoles.isSelected()? 1 : 0));
                kioskServer.addObserver(theHumanGamer);
                kioskServer.addObserver(this);
                kioskServer.start();
                
                publishButton.setServer(kioskServer);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    @Override
    public void itemStateChanged(ItemEvent e) {
        if(e.getSource() == playerComboBox) {
            if(playerComboBox.getSelectedItem().equals(remotePlayerString)) {
                computerAddress.setVisible(true);
            } else {
                computerAddress.setVisible(false);
            }
            validate();
        }        
    }

    @Override
    public void observe(Event event) {
        if(event instanceof ServerIllegalMoveEvent) {
            ServerIllegalMoveEvent x = (ServerIllegalMoveEvent)event;
            System.err.println("Got illegal move [" + x.getMove() + "] by role [" + x.getRole() + "].");
        } else if (event instanceof ServerTimeoutEvent) {
            ServerTimeoutEvent x = (ServerTimeoutEvent)event;
            System.err.println("Timeout when communicating with role [" + x.getRole() + "].");            
        } else if (event instanceof ServerConnectionErrorEvent) {
            ServerConnectionErrorEvent x = (ServerConnectionErrorEvent)event;
            System.err.println("Connection error when communicating with role [" + x.getRole() + "].");            
        }
    }    
}