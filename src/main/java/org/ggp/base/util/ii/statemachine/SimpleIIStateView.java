package org.ggp.base.util.ii.statemachine;

import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.Role;

import com.google.common.collect.ImmutableSet;

public class SimpleIIStateView implements IIStateView {
    private final ImmutableSet<GdlSentence> contents;
    private final Role role;

    public SimpleIIStateView(ImmutableSet<GdlSentence> contents, Role role) {
        this.contents = contents;
        this.role = role;
    }

    @Override
    public Set<GdlSentence> getContents() {
        return contents;
    }

    @Override
    public Role getRole() {
        return role;
    }
}
