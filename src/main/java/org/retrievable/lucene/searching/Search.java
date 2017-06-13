package org.retrievable.lucene.searching;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.retrievable.lucene.searching.support.Hits;
import org.retrievable.lucene.searching.support.Hits.Hit;

import edu.gslis.lucene.main.config.QueryConfig;

public class Search {

	public Hits search(IndexReader index, QueryConfig queryConfig) throws IOException, org.apache.lucene.queryparser.classic.ParseException {
		Hits finalHits = new Hits();
		for (LeafReaderContext subindex : index.getContext().leaves()) {
			Hits hits = search(index, subindex, queryConfig);
			finalHits.combineHits(hits);
		}
		
		for (Hit hit : finalHits) {
			double score = 0.0;
			for (String term : queryConfig.getText().trim().split(" ")) {
				double termScore = hit.getTermScore(term);
				if (termScore == 0.0) {
					int docLength = (Integer) index.document(hit.id).getField("length").numericValue();
					
					System.err.println("Term score of " + term + " is 0 in " + hit.id + ": rescoring");
					termScore = score(0, docLength, collectionProb(new Term("text", term), index));
				}
				score += termScore;
			}
			hit.score = score;
		}
		
		return finalHits;
	}
	
	public Hits search(IndexReader index, LeafReaderContext subindex, QueryConfig queryConfig) throws IOException {
		Hits rawHits = new Hits();
		System.err.println(queryConfig.getText());
		for (String termString : queryConfig.getText().trim().split(" ")) {
			Term term = new Term("text", termString);
			
			double collectionProb = collectionProb(term, index);
			
			PostingsEnum postings = subindex.reader().postings(term);
			if (postings == null) {
				System.err.println("No postings for " + term.text());
				continue;
			}
			while (postings.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
				int docLength = (Integer) index.document(postings.docID()).getField("length").numericValue();
				int freq = postings.freq();

				System.err.println("Scoring term: " + termString);
				double score = score(freq, docLength, collectionProb);
				rawHits.setTermScore(termString, score, postings.docID());
			}
		}
		return rawHits;
	}
	
	public double collectionProb(Term term, IndexReader index) throws IOException {
		System.err.println("Collection stats for term: " + term.text());
		System.err.println(index.totalTermFreq(term));
		System.err.println(index.getSumTotalTermFreq("text"));
		return (index.totalTermFreq(term) + 1.0) / (index.getSumTotalTermFreq("text") + 1.0);
	}
	
	// Dirichlet score
	public double score(int freq, double docLength, double collectionScore) throws IOException {
		double mu = 2500;

		System.err.println("Freq: " + freq);
		System.err.println("DLen: " + docLength);
		System.err.println("ColSc: " + collectionScore);
		
		double pr = (freq + 
				mu * collectionScore) / 
				(docLength + mu);

		return Math.log(pr);
	}
	
}
