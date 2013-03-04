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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.ggp.base.apps.player.config.ConfigPanel;

import external.JSON.JSONException;
import external.JSON.JSONObject;

class ParametricConfigPanel extends ConfigPanel implements ActionListener, DocumentListener {
	private static final long serialVersionUID = 1L;
	
	private JSONObject params;
	private boolean params_dirty;

	final JButton saveButton;
	final JTextField styleField;
	final JComboBox strategyField;
	public ParametricConfigPanel() {
		super(new GridBagLayout());		
		JPanel leftPanel = new JPanel(new GridBagLayout());
		leftPanel.setBorder(new TitledBorder("Major Parameters"));
		JPanel rightPanel = new JPanel(new GridBagLayout());
		rightPanel.setBorder(new TitledBorder("Minor Parameters"));

		strategyField = new JComboBox(new String[] { "Noop", "Legal", "Random", "Puzzle", "Minimax", "SearchLight", "Heuristic", "Monte Carlo" });

		styleField = new JTextField();
		styleField.setColumns(20);
		
		saveButton = new JButton(saveButtonMethod());
	    saveButton.setEnabled(false);
		
		int nRow = 0;
		leftPanel.add(new JLabel("Strategy"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(strategyField, new GridBagConstraints(1, nRow++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(new JLabel("Style"), new GridBagConstraints(0, nRow, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(styleField, new GridBagConstraints(1, nRow++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 5, 5));
		leftPanel.add(saveButton, new GridBagConstraints(1, nRow++, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5));

		add(leftPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
		add(rightPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));		
		
		loadParamsJSON();
		setUIfromJSON();
		
		strategyField.addActionListener(this);
		styleField.getDocument().addDocumentListener(this);		
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
			if (!styleField.getText().isEmpty()) {
				newParams.put("style", styleField.getText());
			}
			newParams.put("strategy", strategyField.getSelectedItem().toString());
		} catch (JSONException je) {
			je.printStackTrace();
		}
		return newParams;
	}
	
	void setUIfromJSON() {
		try {
			if (params.has("style")) {
				styleField.setText(params.getString("style"));					
			}
			if (params.has("strategy")) {
				strategyField.setSelectedItem(params.getString("strategy"));
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