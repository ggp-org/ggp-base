package org.ggp.base.validator;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.ggp.base.util.Pair;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.game.TestGameRepository;
import org.ggp.base.util.gdl.grammar.Gdl;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.alloyggp.griddle.GdlProblem;
import net.alloyggp.griddle.validator.Validator;
import net.alloyggp.griddle.validator.Validators;

public class StaticValidator implements GameValidator {
    @Override
    public List<ValidatorWarning> checkValidity(Game theGame) throws ValidatorException {
        return validateDescription(theGame.getRules());
    }

    public static List<ValidatorWarning> validateDescription(List<Gdl> description) throws ValidatorException {
        Validator validator = Validators.getStandardValidator();
        String gameString = Joiner.on(' ').join(description);
        Set<GdlProblem> problems = validator.findProblems(gameString);

        if (containsErrors(problems)) {
            throw new ValidatorException(getErrorExplanations(problems));
        }

        return toValidatorWarnings(problems);
    }

    private static boolean containsErrors(Set<GdlProblem> problems) {
        for (GdlProblem problem : problems) {
            if (problem.isError()) {
                return true;
            }
        }
        return false;
    }

    private static String getErrorExplanations(Set<GdlProblem> problems) {
        Set<GdlProblem> errors = Sets.filter(problems, new Predicate<GdlProblem>() {
            @Override
            public boolean apply(GdlProblem input) {
                return input.isError();
            }
        });
        SortedSet<GdlProblem> sortedErrors = new TreeSet<GdlProblem>(new Comparator<GdlProblem>() {
            @Override
            public int compare(GdlProblem o1, GdlProblem o2) {
                int comparison = Integer.compare(o1.getPosition().getStart(), o2.getPosition().getStart());
                if (comparison != 0) {
                    return comparison;
                }
                return o1.getMessage().compareTo(o2.getMessage());
            }
        });
        sortedErrors.addAll(errors);
        StringBuilder sb = new StringBuilder();
        for (GdlProblem problem : sortedErrors) {
            sb.append(problem.getMessage()).append("\n");
        }
        return sb.toString();
    }

    private static List<ValidatorWarning> toValidatorWarnings(Set<GdlProblem> problems) {
        List<Pair<Integer, ValidatorWarning>> sortableWarnings = Lists.newArrayList();
        for (GdlProblem problem : problems) {
            sortableWarnings.add(Pair.of(problem.getPosition().getStart(), new ValidatorWarning(problem.getMessage())));
        }
        Collections.sort(sortableWarnings, new Comparator<Pair<Integer, ValidatorWarning>>() {
            @Override
            public int compare(Pair<Integer, ValidatorWarning> o1, Pair<Integer, ValidatorWarning> o2) {
                int comparison = o1.left.compareTo(o2.left);
                if (comparison != 0) {
                    return comparison;
                }
                return o1.right.toString().compareTo(o2.right.toString());
            }
        });
        List<ValidatorWarning> warnings = Lists.newArrayList();
        for (Pair<Integer, ValidatorWarning> warningPair : sortableWarnings) {
            warnings.add(warningPair.right);
        }
        return warnings;
    }

    //These are test cases for smooth handling of errors that often
    //appear in rulesheets. They are intentionally invalid.
    private static final ImmutableSet<String> GAME_KEY_BLACKLIST =
            ImmutableSet.of(
                    "test_case_3b",
                    "test_case_3e",
                    "test_case_3f",
                    "test_clean_not_distinct");

    /**
     * Tries to test most of the rulesheets in the games directory. This should
     * be run when developing a new game to spot errors.
     */
    public static void main(String[] args) {
        GameRepository testGameRepo = new TestGameRepository();

        for(String gameKey : testGameRepo.getGameKeys()) {
            if (GAME_KEY_BLACKLIST.contains(gameKey)) {
                continue;
            }

            System.out.println("Testing " + gameKey);
            try {
                List<ValidatorWarning> warnings = new StaticValidator().checkValidity(testGameRepo.getGame(gameKey));
                if (!warnings.isEmpty()) {
                    System.out.println("Warnings for " + gameKey + ": " + warnings);
                }
            } catch (ValidatorException e) {
                e.printStackTrace();
                //Draw attention to the error
                return;
            }
        }
    }
}
