package apps.kiosk;

import java.applet.Applet;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import apps.common.ConsolePanel;
import apps.common.NativeUI;
import apps.kiosk.games.*;
import apps.kiosk.server.KioskGameServer;
import apps.kiosk.templates.CommonGraphics;

import player.GamePlayer;

import server.event.ServerConnectionErrorEvent;
import server.event.ServerIllegalMoveEvent;
import server.event.ServerTimeoutEvent;
import util.gdl.grammar.Gdl;
import util.kif.KifReader;
import util.match.Match;
import util.observer.Event;
import util.observer.Observer;

/**
 * AppletKiosk is a version of the standard Kiosk that can run in an applet.
 * This is the first step towards having a fully web-based GGP infrastructure.
 * Future steps should be more focused on Javascript/HTML/CSS.
 * 
 * TODO: Reconcile this with the Kiosk codebase. It's almost entirely copied,
 * but there are a couple of subtle differences in how resources are loaded and
 * these need to be refactored into a more coherent infrastructure.
 * 
 * @author Sam
 */
@SuppressWarnings("serial")
public final class AppletKiosk extends Applet implements ActionListener, Observer
{
    public void init() {
        setName("Gaming WebKiosk");
        setBackground(managerPanel.getBackground());
        
        // This is necessary for loading resources from the
        // JAR file associated with this applet.
        CommonGraphics.loadFrom = this;
        
        NativeUI.setNativeUI();
    }

    private final JPanel gamePanel, managerPanel;

    private final JTextField playClockTextField;
    private final JTextField startClockTextField;
    
    private final JButton runButton;
    private final JList selectedGame;
    private final JCheckBox flipRoles;

    private final JPanel theGUIPanel;
    
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
            return gameName.compareTo(((AvailableGame)o).gameName);
        }
    }
    
    private final JTextField computerAddress;

    private void addToSet(Set<AvailableGame> theSet, Class<?> availableCanvas) {
    	try {
    		GameCanvas theCanvas = (GameCanvas) availableCanvas.newInstance();
    		theSet.add(new AvailableGame(theCanvas.getGameName(), theCanvas.getGameKIF(), availableCanvas));
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public AppletKiosk()
    {
        this.setLayout(new GridBagLayout());

        SortedSet<AvailableGame> theAvailableGames = new TreeSet<AvailableGame>();
        addToSet(theAvailableGames, BattleCanvas.class);
        addToSet(theAvailableGames, BiddingTicTacToeCanvas.class);
        addToSet(theAvailableGames, BlockerCanvas.class);
        addToSet(theAvailableGames, BreakthroughCanvas.class);
        addToSet(theAvailableGames, BreakthroughHolesCanvas.class);
        addToSet(theAvailableGames, BreakthroughSmallCanvas.class);
        addToSet(theAvailableGames, CephalopodCanvas.class);
        addToSet(theAvailableGames, CheckersCanvas.class);
        addToSet(theAvailableGames, CheckersSmallCanvas.class);
        addToSet(theAvailableGames, CheckersTinyCanvas.class);
        //addToSet(theAvailableGames, ChessCanvas.class);
        addToSet(theAvailableGames, ChickenTicTacToeCanvas.class);
        addToSet(theAvailableGames, ConnectFiveCanvas.class);
        addToSet(theAvailableGames, ConnectFourCanvas.class);
        addToSet(theAvailableGames, FFACanvas.class);
        addToSet(theAvailableGames, GoldenRectangleCanvas.class);
        addToSet(theAvailableGames, KnightFightCanvas.class);
        addToSet(theAvailableGames, KnightthroughCanvas.class);
        addToSet(theAvailableGames, NumberTicTacToeCanvas.class);
        addToSet(theAvailableGames, PawnWhoppingCanvas.class);
        addToSet(theAvailableGames, PentagoCanvas.class);
        //addToSet(theAvailableGames, QyshinsuCanvas.class);
        addToSet(theAvailableGames, TicTacToeCanvas.class);
        addToSet(theAvailableGames, TTCC4Canvas.class);
        addToSet(theAvailableGames, TTCC4SmallCanvas.class);
        addToSet(theAvailableGames, TTCCanvas.class);
        addToSet(theAvailableGames, TTTxNineCanvas.class);
        
        flipRoles = new JCheckBox("Flip roles?");
        
        selectedGame = new JList(theAvailableGames.toArray());
        selectedGame.setSelectedIndex(0);
        selectedGame.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane selectedGamePane = new JScrollPane(selectedGame);
        
        computerAddress = new JTextField("184.73.38.228:9147");
        
        runButton = new JButton("Run!");
        runButton.addActionListener(this);

        startClockTextField = new JTextField("30");
        playClockTextField = new JTextField("10");
        managerPanel = new JPanel(new GridBagLayout());
        
        //runButton.setEnabled(false);
        startClockTextField.setColumns(15);
        playClockTextField.setColumns(15);
        
        int nRowCount = 1;
        managerPanel.setBorder(new TitledBorder("Kiosk Control"));
        managerPanel.add(new JLabel("Opponent:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(computerAddress, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Start Clock:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(startClockTextField, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Play Clock:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(playClockTextField, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(flipRoles, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Game:"), new GridBagConstraints(0, nRowCount, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(selectedGamePane, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 5.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new ConsolePanel(), new GridBagConstraints(0, nRowCount++, 2, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(runButton, new GridBagConstraints(1, nRowCount++, 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

        gamePanel = new JPanel(new GridBagLayout());
        gamePanel.setBorder(new TitledBorder("Game Kiosk"));

        theGUIPanel = new JPanel();
        theGUIPanel.setBackground(getBackground());
        gamePanel.add(theGUIPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));

        this.add(managerPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
        this.add(gamePanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));                
        
        // Start up the gamers!
        try {
            theHumanGamer = new KioskGamer(theGUIPanel);
            humanGamePlayer = new GamePlayer(3333, theHumanGamer);
            humanGamePlayer.start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }         
    
    private KioskGamer theHumanGamer;
    private GamePlayer humanGamePlayer;
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == runButton) {
            try {
                AvailableGame theGame = (AvailableGame) (selectedGame.getSelectedValue());
                String kifFile = theGame.kifFile;

                // TODO: Clean this up, so it's more general.
                //String gameDirectory = ProjectConfiguration.gameRulesheetsPath;
                String resourceName = "/games/rulesheets/" + kifFile + ".kif";
                InputStream ruleStream = getClass().getResourceAsStream(resourceName);
                if(ruleStream == null) System.err.println("ruleStream is NULL for: " + resourceName);
                List<Gdl> description = KifReader.readStream(ruleStream);

                String matchId = "kiosk." + kifFile + "-" + System.currentTimeMillis();
                int startClock = Integer.valueOf(startClockTextField.getText());
                int playClock = Integer.valueOf(playClockTextField.getText());
                Match match = new Match(matchId, startClock, playClock, description);

                GameCanvas theCanvas = theGame.getCanvas();
                theCanvas.setBackground(getBackground());
                theHumanGamer.setCanvas(theCanvas);

                List<String> hosts = new ArrayList<String>();
                List<Integer> ports = new ArrayList<Integer>();
                List<String> playerNames = new ArrayList<String>();

                if(!flipRoles.isSelected()) {
                    hosts.add("127.0.0.1");
                    ports.add(humanGamePlayer.getGamerPort());
                    playerNames.add("Human");                                   
                }                    
                
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
                
                if(flipRoles.isSelected()) {
                    hosts.add("127.0.0.1");
                    ports.add(humanGamePlayer.getGamerPort());
                    playerNames.add("Human");                                   
                }                
                
                KioskGameServer kioskServer = new KioskGameServer(match, hosts, ports, playerNames, (flipRoles.isSelected()? 1 : 0));
                kioskServer.addObserver(theHumanGamer);
                kioskServer.addObserver(this);
                kioskServer.start();
            } catch(Exception ex) {                
                ex.printStackTrace();
            }
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