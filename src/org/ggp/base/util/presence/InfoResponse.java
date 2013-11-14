package org.ggp.base.util.presence;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.symbol.factory.SymbolFactory;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;
import org.ggp.base.util.symbol.grammar.Symbol;
import org.ggp.base.util.symbol.grammar.SymbolAtom;
import org.ggp.base.util.symbol.grammar.SymbolList;
import org.ggp.base.util.symbol.grammar.SymbolPool;

/**
 * Wherein we poorly reinvent JSON, so that we can keep INFO responses
 * consistent with the Symbol-based KIF format that the other GGP protocol
 * messages are in.
 *
 * @author schreib
 */

public class InfoResponse {
	private String name;
	private String status;
	private String species;

	public InfoResponse() {
		;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setSpecies(String species) {
		this.species = species;
	}

	public String getName() {
		return name;
	}

	public String getStatus() {
		return status;
	}

	public String getSpecies() {
		return species;
	}

	public InfoResponse(Symbol symbol) {
		if (symbol instanceof SymbolList) {
			SymbolList pairs = (SymbolList)symbol;
			for (int i = 0; i < pairs.size(); i++) {
				Symbol pairSymbol = pairs.get(i);
				if (pairSymbol instanceof SymbolList) {
					SymbolList pair = (SymbolList)pairSymbol;
					if (pair.size() < 2) continue;
					String key = pair.get(0).toString().toLowerCase();
					String value = "";
					for (int j = 1; j < pair.size(); j++) {
						value += pair.get(j).toString();
					}
					if (key.equals("name")) {
						name = value;
					} else if (key.equals("status")) {
						status = value;
					} else if (key.equals("species")) {
						species = value;
					}
				}
			}
		} else if (symbol instanceof SymbolAtom) {
			status = ((SymbolAtom) symbol).getValue();
		}
	}

	public static InfoResponse create(String original) {
		try {
			return new InfoResponse(SymbolFactory.create(original));
		} catch (SymbolFormatException e) {
			return new InfoResponse();
		}
	}

	private Symbol getKeyValueSymbol(String key, String value) {
		Symbol keySymbol = SymbolPool.getAtom(key);
		Symbol valueSymbol = SymbolPool.getAtom(value);
		return SymbolPool.getList(new Symbol[] {keySymbol, valueSymbol} );
	}

	public Symbol toSymbol() {
		List<Symbol> infoList = new ArrayList<Symbol>();
		if (name != null) {
			infoList.add(getKeyValueSymbol("name", name));
		}
		if (status != null) {
			infoList.add(getKeyValueSymbol("status", status));
		}
		if (species != null) {
			infoList.add(getKeyValueSymbol("species", species));
		}
		return SymbolPool.getList(infoList);
	}
}
