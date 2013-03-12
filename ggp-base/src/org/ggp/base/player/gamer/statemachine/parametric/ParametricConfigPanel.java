package org.ggp.base.player.gamer.statemachine.parametric;

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

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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

import org.ggp.base.apps.player.config.ConfigPanel;

import external.JSON.JSONException;
import external.JSON.JSONObject;

class ParametricConfigPanel extends ConfigPanel implements ActionListener, DocumentListener, ChangeListener {
	private static final long serialVersionUID = 1L;
	
	private JSONObject params;
	private boolean params_dirty;

	final JButton saveButton;
	final JTextField style;
	final JComboBox strategy;
	final JComboBox stateMachine;
	final JCheckBox cacheStateMachine;
	final JSpinner maxPlys;
	final JPanel rightPanel;
	public ParametricConfigPanel() {
		super(new GridBagLayout());		
		JPanel leftPanel = new JPanel(new GridBagLayout());
		leftPanel.setBorder(new TitledBorder("Major Parameters"));
		rightPanel = new JPanel(new GridBagLayout());
		rightPanel.setBorder(new TitledBorder("Minor Parameters"));

		strategy = new JComboBox(new String[] { "Noop", "Legal", "Random", "Puzzle", "Minimax", "SearchLight", "Heuristic", "Monte Carlo" });
		stateMachine = new JComboBox(new String[] { "Prover" });
		cacheStateMachine = new JCheckBox();
		maxPlys = new JSpinner(new SpinnerNumberModel(5,1,100,1));

		style = new JTextField();
		style.setColumns(20);
		
		saveButton = new JButton(saveButtonMethod());
	    saveButton.setEnabled(false);
		
		int nRow = 0;
		leftPanel.add(new JLabel("Strategy"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(strategy, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(new JLabel("State Machine"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(stateMachine, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));		
		//leftPanel.add(new JLabel("Style"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		//leftPanel.add(styleField, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(saveButton, new GridBagConstraints(1, nRow++, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));		
		layoutRightPanel();
		
		add(leftPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		add(rightPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));		
		
		loadParamsJSON();
		setUIfromJSON();
		
		strategy.addActionListener(this);
		stateMachine.addActionListener(this);
		cacheStateMachine.addActionListener(this);
		maxPlys.addChangeListener(this);
		style.getDocument().addDocumentListener(this);		
	}
	
	private void layoutRightPanel() {
		int nRow = 0;
		rightPanel.removeAll();
		rightPanel.add(new JLabel("State machine cache?"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		rightPanel.add(cacheStateMachine, new GridBagConstraints(1, nRow++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		if (strategy.getSelectedItem().toString().equals("Heuristic")) {
			rightPanel.add(new JLabel("Max plys?"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
			rightPanel.add(maxPlys, new GridBagConstraints(1, nRow++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
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
		JSONObject newParams = getJSONfromUI();
		if (!newParams.toString().equals(params.toString())) {
			params_dirty = true;
			params = newParams;				
		}
		saveButton.setEnabled(params_dirty);
	}
	
	JSONObject getJSONfromUI() {
		JSONObject newParams = new JSONObject();
		try {
			if (!style.getText().isEmpty()) {
				newParams.put("style", style.getText());
			}
			newParams.put("strategy", strategy.getSelectedItem().toString());
			newParams.put("stateMachine", stateMachine.getSelectedItem().toString());
			newParams.put("cacheStateMachine", cacheStateMachine.isSelected());
			newParams.put("maxPlys", maxPlys.getModel().getValue());
		} catch (JSONException je) {
			je.printStackTrace();
		}
		return newParams;
	}
	
	void setUIfromJSON() {
		try {
			if (params.has("style")) {
				style.setText(params.getString("style"));					
			}
			if (params.has("strategy")) {
				strategy.setSelectedItem(params.getString("strategy"));
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
		} catch (JSONException je) {
			je.printStackTrace();
		}
	}
	
	void loadParamsJSON() {
		params = new JSONObject();
		String paramsFilename = System.getProperty("user.home") + "/.ggp-gamer-params.json";
		File paramsFile = new File(paramsFilename);
		if (!paramsFile.exists()) {				
			return;
		}
		try {
			String line;
			StringBuilder pdata = new StringBuilder();
			FileInputStream fis = new FileInputStream(paramsFilename);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			try {
				while ((line = br.readLine()) != null) {
					pdata.append(line);
				}
			} finally {
				br.close();
			}
			params = new JSONObject(pdata.toString());
			params_dirty = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void saveParamsJSON() {
		try {
			String paramsFilename = System.getProperty("user.home") + "/.ggp-gamer-params.json";
			File paramsFile = new File(paramsFilename);
			if (!paramsFile.exists()) {				
				paramsFile.createNewFile();
			}
			FileWriter fw = new FileWriter(paramsFile);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(params.toString());
			bw.close();
			params_dirty = false;
			saveButton.setEnabled(false);
		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}
	
	AbstractAction saveButtonMethod() {
		return new AbstractAction("Save") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent evt) {
				saveParamsJSON();
			}
		};
	}
}