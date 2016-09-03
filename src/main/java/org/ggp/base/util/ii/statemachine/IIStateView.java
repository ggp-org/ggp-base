package org.ggp.base.util.ii.statemachine;

import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.Role;

public interface IIStateView {
    Set<GdlSentence> getContents();

    Role getRole();
}
