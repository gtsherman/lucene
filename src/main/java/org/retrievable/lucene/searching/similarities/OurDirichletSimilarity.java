package org.retrievable.lucene.searching.similarities;

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
		return (float)Math.log(pr);
	}

}
