package org.ggp.base.util.gdl.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class ImmutableSentenceDomainModel extends AbstractSentenceDomainModel {
    private final ImmutableMap<SentenceForm, SentenceFormDomain> domains;

    private ImmutableSentenceDomainModel(
            ImmutableSentenceFormModel formModel,
            ImmutableMap<SentenceForm, SentenceFormDomain> domains) {
        super(formModel);
        if (!formModel.getSentenceForms().equals(domains.keySet())) {
            throw new IllegalArgumentException();
        }
        this.domains = domains;
    }

    public static ImmutableSentenceDomainModel create(SentenceFormModel formModel,
            Map<SentenceForm, SentenceFormDomain> domains) {
        return new ImmutableSentenceDomainModel(ImmutableSentenceFormModel.copyOf(formModel),
                ImmutableMap.copyOf(domains));
    }

    public static ImmutableSentenceDomainModel copyUsingCartesianDomains(
            SentenceDomainModel otherModel) {
        if (otherModel instanceof ImmutableSentenceDomainModel) {
            return (ImmutableSentenceDomainModel) otherModel;
        }

        ImmutableMap.Builder<SentenceForm, SentenceFormDomain> domains = ImmutableMap.builder();
        for (SentenceForm form : otherModel.getSentenceForms()) {
            SentenceFormDomain otherDomain = otherModel.getDomain(form);
            List<Set<GdlConstant>> domainsForSlots = Lists.newArrayList();
            for (int i = 0; i < form.getTupleSize(); i++) {
                domainsForSlots.add(otherDomain.getDomainForSlot(i));
            }
            domains.put(form, CartesianSentenceFormDomain.create(form, domainsForSlots));
        }
        return new ImmutableSentenceDomainModel(ImmutableSentenceFormModel.copyOf(otherModel), domains.build());
    }

    @Override
    public SentenceFormDomain getDomain(SentenceForm form) {
        return domains.get(form);
    }
}
