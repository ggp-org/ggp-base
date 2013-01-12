package org.ggp.base.util.gdl.scrambler;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

import org.ggp.base.util.crypto.BaseHashing;
import org.ggp.base.util.files.FileUtils;
import org.ggp.base.util.gdl.factory.GdlFactory;
import org.ggp.base.util.gdl.factory.exceptions.GdlFormatException;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;

import external.Base64Coder.Base64Coder;


public class MappingGdlScrambler implements GdlScrambler {	
	private Map<String,String> scrambleMapping;	
	private Map<String,String> unscrambleMapping;
	private Stack<String> prepopulatedFakeWords;
	private Random random;
	
	public MappingGdlScrambler() {
		random = new Random();
		scrambleMapping = new HashMap<String,String>();
		unscrambleMapping = new HashMap<String,String>();
		
		prepopulatedFakeWords = new Stack<String>();
		String wordListFile = FileUtils.readFileAsString(new File("src/org/ggp/base/util/gdl/scrambler/WordList"));
		if (wordListFile != null && !wordListFile.isEmpty()) {
			String[] words = wordListFile.split("[\n\r]");
			for (String word : words) {
				prepopulatedFakeWords.push(word);
			}
			Collections.shuffle(prepopulatedFakeWords);
		}		
	}
	
	private class ScramblingRenderer extends GdlRenderer {
		@Override
		protected String renderConstant(GdlConstant constant) {
			return scrambleWord(constant.getValue());
		}
		@Override
		protected String renderVariable(GdlVariable variable) {
			return scrambleWord(variable.toString());
		}		
	}
	private class UnscramblingRenderer extends GdlRenderer {
		@Override
		protected String renderConstant(GdlConstant constant) {
			return unscrambleWord(constant.getValue());
		}
		@Override
		protected String renderVariable(GdlVariable variable) {
			return unscrambleWord(variable.toString());
		}		
	}	
	
	@Override
	public String scramble(Gdl x) {
		return new ScramblingRenderer().renderGdl(x);
	}
	
	@Override
	public Gdl unscramble(String x) throws SymbolFormatException, GdlFormatException {
		return GdlFactory.create(new UnscramblingRenderer().renderGdl(GdlFactory.create(x)));
	}
	
	@Override
	public boolean scrambles() {
		return true;
	}
	
	private String scrambleWord(String realWord) {
		if (!shouldMap(realWord)) {
			return realWord;
		}
		if (!scrambleMapping.containsKey(realWord)) {
			String fakeWord = generateNewRandomWord();
			if (realWord.startsWith("?")) {
				fakeWord = "?" + fakeWord;
			}
			scrambleMapping.put(realWord, fakeWord);
			unscrambleMapping.put(fakeWord, realWord);
		}
		return scrambleMapping.get(realWord);
	}
	
	private String unscrambleWord(String fakeWord) {
		if (!shouldMap(fakeWord)) {
			return fakeWord;
		}
		fakeWord = fakeWord.toLowerCase();
		if (!unscrambleMapping.containsKey(fakeWord)) {
			throw new RuntimeException("Could not find scramble mapping for: " + fakeWord);
		}
		return unscrambleMapping.get(fakeWord);
	}

	private String generateNewRandomWord() {
		String word = null;		
		do {
			word = generateRandomWord().toLowerCase();
		} while (unscrambleMapping.containsKey(word));
		return word;
	}
	
	private String generateRandomWord() {
		if (prepopulatedFakeWords.isEmpty()) {
			return Base64Coder.encodeString(BaseHashing.computeSHA1Hash("" + System.currentTimeMillis() + random.nextLong())).replace('+', 'a').replace('/', 'b').replace('=','c').toLowerCase();
		} else {
			return prepopulatedFakeWords.pop();
		}
	}
	
	// TODO(schreib): Factor this out so that the keyword list can be shared
	// between the GdlPool and the MappingGdlScrambler without causing problems
	// for projects that import this project to depend on it.
	private static final HashSet<String> keywords = new HashSet<String>(Arrays.asList(
    		new String[] {"init","true","next","role","does","goal","legal","terminal","base","input"}));
	private static boolean shouldMap(String token) {
		if (keywords.contains(token.toLowerCase())) {
			return false;
		}
		try {
			Integer.parseInt(token);
			return false;
		} catch (NumberFormatException e) {
			;
		}
		return true;
	}
}