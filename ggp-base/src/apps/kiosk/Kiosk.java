package apps.kiosk;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import apps.common.NativeUI;
import apps.kiosk.server.KioskGameServer;

import player.GamePlayer;
import player.gamer.Gamer;

import util.configuration.ProjectConfiguration;
import util.gdl.grammar.Gdl;
import util.kif.KifReader;
import util.logging.GamerLogger;
import util.match.Match;
import util.reflection.ProjectSearcher;

@SuppressWarnings("serial")
public final class Kiosk extends JPanel implements ActionListener
{    
    private static void createAndShowGUI(Kiosk serverPanel)
    {
        JFrame frame = new JFrame("Gaming Kiosk");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setPreferredSize(new Dimension(1200, 900));
        frame.getContentPane().add(serverPanel);

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args)
    {
        NativeUI.setNativeUI();
    
        final Kiosk serverPanel = new Kiosk();
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {

            public void run()
            {
                createAndShowGUI(serverPanel);
            }
        });
    }

    private final JPanel managerPanel;

    private final JTextField playClockTextField;
    private final JTextField startClockTextField;
    
    private final JButton runButton;
    private final JComboBox selectedGame;

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
    
    private final JComboBox playerComboBox;
    private List<Class<?>> gamers = ProjectSearcher.getAllClassesThatAre(Gamer.class);
    
    public Kiosk()
    {
        super(new GridBagLayout());

        SortedSet<AvailableGame> theAvailableGames = new TreeSet<AvailableGame>();
        List<Class<?>> theAvailableCanvasList = ProjectSearcher.getAllClassesThatAre(GameCanvas.class);
        for(Class<?> availableCanvas : theAvailableCanvasList) {
            try {
                GameCanvas theCanvas = (GameCanvas) availableCanvas.newInstance();                
                theAvailableGames.add(new AvailableGame(theCanvas.getGameName(), theCanvas.getGameKIF(), availableCanvas));
            } catch(Exception e) {
                ;
            }
        }
        
        selectedGame = new JComboBox();
        for(AvailableGame theGame : theAvailableGames)
            selectedGame.addItem(theGame);

        playerComboBox = new JComboBox();
        List<Class<?>> gamersCopy = new ArrayList<Class<?>>(gamers);
        for(Class<?> gamer : gamersCopy)
        {
            Gamer g;
            try {
                g = (Gamer) gamer.newInstance();
                playerComboBox.addItem(g.getName());
            } catch(Exception ex) {
                gamers.remove(gamer);
            }
        }        
        
        runButton = new JButton("Run!");
        runButton.addActionListener(this);

        startClockTextField = new JTextField("30");
        playClockTextField = new JTextField("10");
        managerPanel = new JPanel(new GridBagLayout());
        
        //runButton.setEnabled(false);
        startClockTextField.setColumns(15);
        playClockTextField.setColumns(15);

        managerPanel.setBorder(new TitledBorder("Kiosk Control"));
        managerPanel.add(new JLabel("Game:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(selectedGame, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));        
        managerPanel.add(new JLabel("Opponent:"), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(playerComboBox, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Start Clock:"), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(startClockTextField, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(new JLabel("Play Clock:"), new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(playClockTextField, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
        managerPanel.add(runButton, new GridBagConstraints(1, 5, 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

        JPanel gamePanel = new JPanel(new GridBagLayout());
        gamePanel.setBorder(new TitledBorder("Game Kiosk"));

        theGUIPanel = new JPanel();
        gamePanel.add(theGUIPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));

        this.add(managerPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
        this.add(gamePanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
        
        // Start up the gamers!
        try {
            theHumanGamer = new KioskGamer(theGUIPanel);
            GamePlayer humanPlayer = new GamePlayer(HUMAN_PORT, theHumanGamer);
            humanPlayer.start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private GamePlayer theComputerPlayer = null;
    private KioskGamer theHumanGamer;
    private final static int HUMAN_PORT = 9184;
    private final static int COMPUTER_PORT = 9185;
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == runButton) {
            try {
                AvailableGame theGame = (AvailableGame)selectedGame.getSelectedItem();
                String kifFile = theGame.kifFile;
                
                String gameDirectory = ProjectConfiguration.gameRulesheetsPath;
                List<Gdl> description = KifReader.read(gameDirectory + kifFile + ".kif");
                
                String matchId = "kiosk." + kifFile + "-" + System.currentTimeMillis();
                int startClock = Integer.valueOf(startClockTextField.getText());
                int playClock = Integer.valueOf(playClockTextField.getText());
                Match match = new Match(matchId, startClock, playClock, description);
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
                    Class<?> gamerClass = gamers.get(playerComboBox.getSelectedIndex());
                    try {
                        gamer = (Gamer) gamerClass.newInstance();
                    } catch(Exception ex) { throw new RuntimeException(ex); }
                    theComputerPlayer = new GamePlayer(COMPUTER_PORT, gamer);
                    theComputerPlayer.start();
                }
                
                List<String> hosts = new ArrayList<String>();
                List<Integer> ports = new ArrayList<Integer>();
                List<String> playerNames = new ArrayList<String>();
                
                hosts.add("127.0.0.1");
                ports.add(HUMAN_PORT);
                playerNames.add("Human");               
                
                hosts.add("127.0.0.1");
                ports.add(theComputerPlayer.getGamerPort());                    
                playerNames.add("Computer");
                
                GamerLogger.startFileLogging(match, "kiosk");
                KioskGameServer kioskServer = new KioskGameServer(match, hosts, ports, playerNames, 0);
                kioskServer.addObserver(theHumanGamer);
                kioskServer.start();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}