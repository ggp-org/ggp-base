package org.ggp.base.apps.benchmark;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.loader.RemoteResourceLoader;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;

import external.JSON.JSONArray;
import external.JSON.JSONException;
import external.JSON.JSONObject;

/**
 * ImportSanchoCases reads through benchmark test cases for Sancho
 * and converts them into cases for the PlayerTester.
 *
 * @author Sam Schreiber
 */
public class ImportSanchoCases {
    public static final String[] urlsForSanchoCases = new String[] {
            "https://raw.githubusercontent.com/SteveDraper/ggp-base/develop/data/tests/suites/Tiltyard.29f58e264f8e5b8a9a190b8ae64a1fccf2892c20.0.49.json",
            "https://raw.githubusercontent.com/SteveDraper/ggp-base/develop/data/tests/suites/Tiltyard.50de9881e8d992dae28bf268365a78e55b01f33b.1.11.json",
            "https://raw.githubusercontent.com/SteveDraper/ggp-base/develop/data/tests/suites/Tiltyard.6b34ffb9c48407f9db29c3fcf68063a097c7aec9.1.8.json",
            "https://raw.githubusercontent.com/SteveDraper/ggp-base/develop/data/tests/suites/Tiltyard.71ed3b17f057c511ef4ed5d3d1c0d4030fcdb27b.0.25.json",
            "https://raw.githubusercontent.com/SteveDraper/ggp-base/develop/data/tests/suites/Tiltyard.8d472e9d2c354b614c0b97a78e849afa1f206396.0.25.json",
            "https://raw.githubusercontent.com/SteveDraper/ggp-base/develop/data/tests/suites/Tiltyard.8d472e9d2c354b614c0b97a78e849afa1f206396.0.7.json",
            "https://raw.githubusercontent.com/SteveDraper/ggp-base/develop/data/tests/suites/Tiltyard.a36059b9e9ef556a1523256d0945a5f472a7edee.1.18.json",
            "https://raw.githubusercontent.com/SteveDraper/ggp-base/develop/data/tests/suites/Tiltyard.aef8791cf27fd51b06863606a3a1e02edc156c68.0.3.json",
            "https://raw.githubusercontent.com/SteveDraper/ggp-base/develop/data/tests/suites/Tiltyard.b7d98c11f2da12899a97c1e846938a8d345e6b31.0.7.json",
            "https://raw.githubusercontent.com/SteveDraper/ggp-base/develop/data/tests/suites/Tiltyard.d125166ef6c8af6c731f4be0f50a950891b7a649.3.36.json",
    };

    public static void main(String[] args) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, JSONException, IOException {
        for (String caseURL : urlsForSanchoCases) {
            try {
                convertSanchoTestCases(caseURL);
            } catch (Exception e) {
                System.err.println("Caught exception when processing " + caseURL);
                e.printStackTrace();
            }
        }
    }

    public static void convertSanchoTestCases(String testCasesURL) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, JSONException, IOException, SymbolFormatException {
        JSONObject theCases = RemoteResourceLoader.loadJSON(testCasesURL);
        JSONArray theCasesArr = theCases.getJSONArray("cases");
        for (int i = 0; i < theCasesArr.length(); i++) {
            JSONObject theCase = theCasesArr.getJSONObject(i);

            String theCaseID = theCase.getString("case");
            String[] extractedData = theCaseID.replaceAll("(.*)Tiltyard (.*), player (.*), move (.*)", "$2 $3 $4").split(" ");
            String theMatchID = extractedData[0];
            int thePlayerID = Integer.parseInt(extractedData[1]);
            int theMoveID = Integer.parseInt(extractedData[2]);
            String repoURL = theCase.getString("repo");
            String gameKey = theCase.getString("game");
            @SuppressWarnings("unused")
            int startClock = Integer.parseInt(theCase.getString("start"));
            @SuppressWarnings("unused")
            int playClock = Integer.parseInt(theCase.getString("play"));
            int limitMove = theCase.getInt("limit");
            JSONObject theCheck = theCase.getJSONObject("check");
            int checkRole = Integer.parseInt(theCheck.getString("player"));

            JSONObject realMatchData = RemoteResourceLoader.loadJSON("http://matches.ggp.org/matches/" + theMatchID + "/");
            String theState = realMatchData.getJSONArray("states").getString(theMoveID-1);

            boolean flipAcceptableMoves = false;
            if (theCheck.getString("acceptable").startsWith("!:")) {
                flipAcceptableMoves = true;
                theCheck.put("acceptable", theCheck.getString("acceptable").substring(2));
            }
            Set<String> acceptableMoves = new HashSet<String>(Arrays.asList(theCheck.getString("acceptable").split(",")));
            {
                Set<String> acceptableMovesClean = new HashSet<String>();
                for (String move : acceptableMoves) {
                    acceptableMovesClean.add("( " + move + " )");
                }
                acceptableMoves = acceptableMovesClean;
            }
            if (flipAcceptableMoves) {
                StateMachine theMachine = new ProverStateMachine();
                theMachine.initialize(PlayerTester.getMediasResGame(gameKey, theState).getRules());
                List<Move> legalMoves = theMachine.getLegalMoves(theMachine.getInitialState(), theMachine.getRoles().get(thePlayerID));
                Set<String> newAcceptableMoves = new HashSet<String>();
                for (Move move : legalMoves) {
                    if (!acceptableMoves.contains(move.toString())) newAcceptableMoves.add(move.toString());
                }
                if (acceptableMoves.size() + newAcceptableMoves.size() != legalMoves.size()) throw new RuntimeException("Acceptable move size mismatch: " + Arrays.deepToString(legalMoves.toArray(new Move[]{})) + " != " + Arrays.toString(acceptableMoves.toArray()) + " + " + Arrays.toString(newAcceptableMoves.toArray()));
                acceptableMoves = newAcceptableMoves;
            }

            if (thePlayerID != checkRole) throw new RuntimeException("Unexpected role check: " + thePlayerID + " vs " + checkRole);
            if (theMoveID != limitMove) throw new RuntimeException("Start move != limit move: " + theMoveID + " vs " + limitMove);
            if (!repoURL.equals("games.ggp.org/base")) throw new RuntimeException("Using non-base repo: " + repoURL);


            StringBuilder theGoodMoves = new StringBuilder("new String[] { ");
            for (String move : acceptableMoves) {
                theGoodMoves.append("\"" + move + "\", ");
            }
            theGoodMoves.append("}");

            System.out.println("new PlayerTester.TestCase(\"Sancho\", \"" + gameKey + "\", " + thePlayerID + ", 15, 5, \"" + theState + "\", " + theGoodMoves +"),");
        }
    }
}