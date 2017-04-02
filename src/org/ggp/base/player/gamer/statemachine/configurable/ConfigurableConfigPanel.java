package org.ggp.base.player.gamer.statemachine.configurable;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.ggp.base.apps.player.config.ConfigPanel;

import external.JSON.JSONException;
import external.JSON.JSONObject;

class ConfigurableConfigPanel extends ConfigPanel implements ActionListener, DocumentListener, ChangeListener {
	private static final long serialVersionUID = 1L;
	
	private File associatedFile;	
	final JTextField associatedFileField;	
	
	private JSONObject params;
	private String savedParams;

	final JButton loadButton;
	final JButton saveAsButton;
	final JButton saveButton;
	final JTextField name;
	final JComboBox strategy;
	final JComboBox metagameStrategy;
	final JComboBox stateMachine;
	final JCheckBox cacheStateMachine;
	final JSpinner maxPlys;
	final JSpinner heuristicFocus;
	final JSpinner heuristicMobility;
	final JSpinner heuristicOpponentFocus;
	final JSpinner heuristicOpponentMobility;
	final JSpinner mcDecayRate;
	final JPanel rightPanel;
	public ConfigurableConfigPanel() {
		super(new GridBagLayout());		
		JPanel leftPanel = new JPanel(new GridBagLayout());
		leftPanel.setBorder(new TitledBorder("Major Parameters"));
		rightPanel = new JPanel(new GridBagLayout());
		rightPanel.setBorder(new TitledBorder("Minor Parameters"));

		strategy = new JComboBox(new String[] {"Noop", "Legal", "Random", "Puzzle", "Minimax", "Heuristic", "Monte Carlo"});
		metagameStrategy = new JComboBox(new String[]{"None", "Random Exploration"});
		stateMachine = new JComboBox(new String[] { "Prover" });
		cacheStateMachine = new JCheckBox();
		maxPlys = new JSpinner(new SpinnerNumberModel(1,1,100,1));		
		heuristicFocus = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
		heuristicMobility = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
		heuristicOpponentFocus = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
		heuristicOpponentMobility = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
		mcDecayRate = new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));

		name = new JTextField();
		name.setColumns(20);
		name.setText("Player #" + new Random().nextInt(100000));
		
		loadButton = new JButton(loadButtonMethod());
		saveButton = new JButton(saveButtonMethod());
		saveAsButton = new JButton(saveAsButtonMethod());
		
		associatedFileField = new JTextField();
		associatedFileField.setEnabled(false);
		
		JPanel buttons = new JPanel();		
		buttons.add(loadButton);
		buttons.add(saveButton);
		buttons.add(saveAsButton);
		
		int nRow = 0;
		leftPanel.add(new JLabel("Name"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(name, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));		
		leftPanel.add(new JLabel("Gaming Strategy"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(strategy, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(new JLabel("Metagame Strategy"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(metagameStrategy, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(new JLabel("State Machine"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(stateMachine, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));		
		leftPanel.add(buttons, new GridBagConstraints(1, nRow++, 2, 1, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
		leftPanel.add(associatedFileField, new GridBagConstraints(0, nRow, 2, 1, 1.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 0, 0));
		layoutRightPanel();
		
		add(leftPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		add(rightPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));		
		
		params = new JSONObject();
		syncJSONtoUI();
		
		strategy.addActionListener(this);
		metagameStrategy.addActionListener(this);
		stateMachine.addActionListener(this);
		cacheStateMachine.addActionListener(this);
		maxPlys.addChangeListener(this);
		heuristicFocus.addChangeListener(this);
		heuristicMobility.addChangeListener(this);
		heuristicOpponentFocus.addChangeListener(this);
		heuristicOpponentMobility.addChangeListener(this);
		mcDecayRate.addChangeListener(this);
		name.getDocument().addDocumentListener(this);		
	}
	
	private void layoutRightPanel() {
		int nRow = 0;
		rightPanel.removeAll();
		rightPanel.add(new JLabel("State machine cache?"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		rightPanel.add(cacheStateMachine, new GridBagConstraints(1, nRow++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		if (strategy.getSelectedItem().toString().equals("Heuristic")) {
			rightPanel.add(new JLabel("Max plys?"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
			rightPanel.add(maxPlys, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));			
			rightPanel.add(new JLabel("Focus Heuristic Weight"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
			rightPanel.add(heuristicFocus, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
			rightPanel.add(new JLabel("Mobility Heuristic Weight"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
			rightPanel.add(heuristicMobility, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));			
			rightPanel.add(new JLabel("Opponent Focus Heuristic Weight"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
			rightPanel.add(heuristicOpponentFocus, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));			
			rightPanel.add(new JLabel("Opponent Mobility Heuristic Weight"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
			rightPanel.add(heuristicOpponentMobility, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));			
		}
		if (strategy.getSelectedItem().toString().equals("Monte Carlo")) {
			rightPanel.add(new JLabel("Goal Decay Rate"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
			rightPanel.add(mcDecayRate, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));			
		}
		rightPanel.add(new JLabel(), new GridBagConstraints(2, nRow++, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		rightPanel.repaint();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getParameter(String name, T defaultValue) {
		try {
			if (params.has(name)) {
				return (T)params.get(name);
			} else {
				return defaultValue;
			}
		} catch (JSONException je) {
			return defaultValue;
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == strategy) {
			layoutRightPanel();
		}
		syncJSONtoUI();
	}		
	@Override
	public void changedUpdate(DocumentEvent e) {
		syncJSONtoUI();
	}
	@Override
	public void insertUpdate(DocumentEvent e) {
		syncJSONtoUI();
	}
	@Override
	public void removeUpdate(DocumentEvent e) {
		syncJSONtoUI();
	}
	@Override
	public void stateChanged(ChangeEvent arg0) {
		syncJSONtoUI();
	}
	
	void syncJSONtoUI() {
		if (settingUI) return;		
		params = getJSONfromUI();		
		saveButton.setEnabled(savedParams == null || !params.toString().equals(savedParams));		
	}
	
	JSONObject getJSONfromUI() {
		JSONObject newParams = new JSONObject();
		try {
			if (!name.getText().isEmpty()) {
				newParams.put("name", name.getText());
			}
			newParams.put("strategy", strategy.getSelectedItem().toString());
			newParams.put("metagameStrategy", metagameStrategy.getSelectedItem().toString());
			newParams.put("stateMachine", stateMachine.getSelectedItem().toString());
			newParams.put("cacheStateMachine", cacheStateMachine.isSelected());
			newParams.put("maxPlys", maxPlys.getModel().getValue());
			newParams.put("heuristicFocus", heuristicFocus.getModel().getValue());
			newParams.put("heuristicMobility", heuristicMobility.getModel().getValue());
			newParams.put("heuristicOpponentFocus", heuristicOpponentFocus.getModel().getValue());
			newParams.put("heuristicOpponentMobility", heuristicOpponentMobility.getModel().getValue());
			newParams.put("mcDecayRate", mcDecayRate.getModel().getValue());
		} catch (JSONException je) {
			je.printStackTrace();
		}
		return newParams;
	}
	
	private boolean settingUI = false;
	void setUIfromJSON() {
		settingUI = true;
		try {			
			if (params.has("name")) {
				name.setText(params.getString("name"));					
			}
			if (params.has("strategy")) {
				strategy.setSelectedItem(params.getString("strategy"));
			}
			if (params.has("metagameStrategy")) {
				metagameStrategy.setSelectedItem(params.getString("metagameStrategy"));
			}			
			if (params.has("stateMachine")) {
				stateMachine.setSelectedItem(params.getString("stateMachine"));
			}
			if (params.has("cacheStateMachine")) {
				cacheStateMachine.setSelected(params.getBoolean("cacheStateMachine"));
			}
			if (params.has("maxPlys")) {
				maxPlys.getModel().setValue(params.getInt("maxPlys"));
			}
			if (params.has("heuristicFocus")) {
				heuristicFocus.getModel().setValue(params.getInt("heuristicFocus"));
			}
			if (params.has("heuristicMobility")) {
				heuristicMobility.getModel().setValue(params.getInt("heuristicMobility"));
			}
			if (params.has("heuristicOpponentFocus")) {
				heuristicOpponentFocus.getModel().setValue(params.getInt("heuristicOpponentFocus"));
			}
			if (params.has("heuristicOpponentMobility")) {
				heuristicOpponentMobility.getModel().setValue(params.getInt("heuristicOpponentMobility"));
			}
			if (params.has("mcDecayRate")) {
				mcDecayRate.getModel().setValue(params.getInt("mcDecayRate"));
			}			
		} catch (JSONException je) {
			je.printStackTrace();
		} finally {
			settingUI = false;
		}
	}
	
	void loadParamsJSON(File fromFile) {
		if (!fromFile.exists()) {				
			return;
		}
		associatedFile = fromFile;
		associatedFileField.setText(associatedFile.getPath());
		params = new JSONObject();
		try {
			String line;
			StringBuilder pdata = new StringBuilder();
			FileInputStream fis = new FileInputStream(associatedFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			try {
				while ((line = br.readLine()) != null) {
					pdata.append(line);
				}
			} finally {
				br.close();
			}
			params = new JSONObject(pdata.toString());
			savedParams = params.toString();
			setUIfromJSON();
			syncJSONtoUI();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void saveParamsJSON(boolean saveAs) {
		try {
			if (saveAs || associatedFile == null) {
				final JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new PlayerFilter());
				int returnVal = fc.showSaveDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null) {
					File toFile = fc.getSelectedFile();
					if (toFile.getName().contains(".")) {
						associatedFile = new File(toFile.getParentFile(), toFile.getName().substring(0,toFile.getName().lastIndexOf(".")) + ".player");
					} else {
						associatedFile = new File(toFile.getParentFile(), toFile.getName() + ".player");
					}
					associatedFileField.setText(associatedFile.getPath());
				} else {
					return;
				}
			}
			FileWriter fw = new FileWriter(associatedFile);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(params.toString());
			bw.close();
			savedParams = params.toString();
			syncJSONtoUI();
		} catch (IOException ie) {
			ie.printStackTrace();
		}		
	}
	
	AbstractAction saveButtonMethod() {
		return new AbstractAction("Save") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent evt) {
				saveParamsJSON(false);
			}
		};
	}
	AbstractAction saveAsButtonMethod() {
		return new AbstractAction("Save As") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent evt) {
				saveParamsJSON(true);
			}
		};
	}	
	AbstractAction loadButtonMethod() {
		return new AbstractAction("Load") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent evt) {
				final JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new PlayerFilter());
				int returnVal = fc.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null) {
					loadParamsJSON(fc.getSelectedFile());
				}
			}
		};
	}
	
	static class PlayerFilter extends FileFilter {
	    public boolean accept(File f) {
	        if (f.isDirectory()) return true;
	        return f.getName().endsWith(".player");
	    }
	    public String getDescription() {
	        return "GGP Players (*.player)";
	    }
	}	
}