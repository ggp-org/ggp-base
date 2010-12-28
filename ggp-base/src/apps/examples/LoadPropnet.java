package apps.examples;

import java.util.List;

import util.game.GameRepository;
import util.gdl.grammar.Gdl;
import util.propnet.architecture.PropNet;
import util.propnet.factory.CachedPropNetFactory;

/**
 * This is an example program that loads a PropNet and displays its size.
 * 
 * If you are using this to load a large cached propnet (such as checkers
 * or Zhadu) the following command line arguments for the VM are recommended:
 * 
 *   "-mx500m -Xss20m -server"
 *   
 * Of course, increasing the values in those arguments can't hurt, but they
 * ensure that the propnet generation/loading code has enough memory and stack
 * space that it can operate effectively.
 * 
 * @author Sam Schreiber
 */
public class LoadPropnet {
    public static void main(String[] args) {
        List<Gdl> description = GameRepository.getDefaultRepository().getGame("conn4").getRules();
        
        PropNet theNetwork = CachedPropNetFactory.create(description);
        System.out.println("- - - - - - - - - -");
        System.out.println("Network size: " + theNetwork.getComponents().size());
    }
}