package org.retrievable.lucene.searching.similarities;

import java.util.List;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.LMSimilarity;

public class OurDirichletSimilarity extends LMSimilarity {
	
	private final float mu;
	
	public OurDirichletSimilarity(float mu) {
		this.mu = mu;
	}
	
	public OurDirichletSimilarity() {
		this(2500);
	}
	
	public OurDirichletSimilarity(CollectionModel collectionModel, float mu) {
		super(collectionModel);
		this.mu = mu;
	}
	
	public float getMu() {
		return this.mu;
	}

	@Override
	public String getName() {
		return "Our Dirichlet (" + getMu() + ")";
	}

	@Override
	protected float score(BasicStats stats, float freq, float docLen) {
		double pr = (freq + 
				getMu() * ((LMStats)stats).getCollectionProbability()) / 
				(docLen + getMu());
		return stats.getBoost() * (float)Math.log(pr);
	}

	@Override
	protected void explain(List<Explanation> subs, BasicStats stats, int doc,
	    float freq, float docLen) {
	  if (stats.getBoost() != 1.0f) {
	    subs.add(Explanation.match(stats.getBoost(), "boost"));
	  }

	  subs.add(Explanation.match(mu, "mu"));
	  Explanation weightExpl = Explanation.match(
	      (float)Math.log(1 + freq /
	      (mu * ((LMStats)stats).getCollectionProbability())),
	      "term weight");
	  subs.add(weightExpl);
	  subs.add(Explanation.match(docLen, "doc length"));
	  subs.add(Explanation.match(
	      (float)Math.log(mu / (docLen + mu)), "document norm"));
	  super.explain(subs, stats, doc, freq, docLen);
	}
}
