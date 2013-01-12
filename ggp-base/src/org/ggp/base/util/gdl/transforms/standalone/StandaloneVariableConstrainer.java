package org.ggp.base.util.gdl.transforms.standalone;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.ggp.base.util.files.FileUtils;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.transforms.VariableConstrainer;
import org.ggp.base.validator.StaticValidator;
import org.ggp.base.validator.exception.StaticValidatorException;


/**
 * The standalone version of VariableConstrainer can be run as its own
 * program. It takes a .kif file as input and generates a new .kif file
 * with the modified output. The new filename is (original name)_VARCONST.kif.
 * 
 * The new file is not intended to be particularly legible; it is
 * intended mainly for use by other programs.
 * 
 * @author Alex Landau
 *
 */
public class StandaloneVariableConstrainer {

	public static void main(String[] args) throws InterruptedException {
		if(args.length < 1 || !args[0].endsWith(".kif")) {
			System.out.println("Please enter the path of a .kif file as an argument.");
			return;
		}
		
		String filename = args[0];
		Game theGame = Game.createEphemeralGame(Game.preprocessRulesheet(FileUtils.readFileAsString(new File(filename))));
		List<Gdl> description = theGame.getRules();
		if (description == null || description.size() == 0) {
			System.err.println("Problem reading the file " + filename + " or parsing the GDL.");
			return;
		}
		
		try {
			StaticValidator.validateDescription(description);
		} catch (StaticValidatorException e) {
			System.err.println("GDL validation error: " + e.toString());
			return;
		}
		
		List<Gdl> transformedDescription = VariableConstrainer.replaceFunctionValuedVariables(description);

		String newFilename = filename.substring(0, filename.lastIndexOf(".kif")) + "_VARCONST.kif";
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(newFilename)));
			
			for(Gdl gdl : transformedDescription) {
				out.write(gdl.toString());
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			System.err.println("There was an error writing the translated GDL file " + newFilename + ".");
			e.printStackTrace();
		}
	}

}
