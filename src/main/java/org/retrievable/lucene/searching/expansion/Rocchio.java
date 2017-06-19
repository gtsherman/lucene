package org.retrievable.lucene.searching.expansion;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.retrievable.lucene.searching.Searcher;
import org.retrievable.lucene.searching.support.FeatureVector;

import edu.gslis.lucene.main.config.QueryConfig;

public class Rocchio {
	
	private double alpha;
	private double beta;
	
	/**
	 * Default parameter values taken from:
	 * https://nlp.stanford.edu/IR-book/html/htmledition/the-rocchio71-algorithm-1.html
	 */
	public Rocchio() {
		this(1.0, 0.75);
	}
	
	public Rocchio(double alpha, double beta) {
		this.alpha = alpha;
		this.beta = beta;
	}
	
	public void expandQuery(IndexSearcher index, QueryConfig query, int fbDocs, int fbTerms) throws IOException {
		ScoreDoc[] initialResults = Searcher.search(index, query, fbDocs);
		
		FeatureVector summedTermVec = new FeatureVector(null);
		
		for (ScoreDoc doc : initialResults) {
			StringBuffer docText = new StringBuffer();
			for (String chunk : index.doc(doc.doc).getValues("text")) {
				docText.append(chunk);
			}

			// Get the document tokens and add to the summed term vector
			parseText(docText.toString(), summedTermVec);
		}
		
		// Multiply the summed term vector by beta / |Dr|
		FeatureVector relDocTermVec = new FeatureVector(null);
		for (String term : summedTermVec) {
			relDocTermVec.addTerm(term, summedTermVec.getFeatureWeight(term) * beta / fbDocs);
		}
		
		// Create a query vector and scale by alpha
		FeatureVector summedQueryVec = new FeatureVector(null);
		parseText(query.getText(), summedQueryVec);

		FeatureVector queryTermVec = new FeatureVector(null);
		for (String term : summedQueryVec) {
			queryTermVec.addTerm(term, summedQueryVec.getFeatureWeight(term) * alpha);
		}
		
		// Combine query and rel doc vectors
		for (String term : queryTermVec) {
			relDocTermVec.addTerm(term, relDocTermVec.getFeatureWeight(term));
		}
		
		// Get top terms
		relDocTermVec.clip(fbTerms);
		
		StringBuffer expandedQuery = new StringBuffer();
		for (String term : relDocTermVec) {
			expandedQuery.append(term + "^" + relDocTermVec.getFeatureWeight(term) + " ");
		}
		
		query.setText(expandedQuery.toString());
	}
	
	private void parseText(String text, FeatureVector vector) throws IOException {
		StandardAnalyzer analyzer = new StandardAnalyzer();
		TokenStream tokenStream = analyzer.tokenStream(null, text);
		CharTermAttribute tokens = tokenStream.addAttribute(CharTermAttribute.class);
		tokenStream.reset();
		while (tokenStream.incrementToken()) {
			String token = tokens.toString();
			vector.addTerm(token);
		}
		analyzer.close();	
	}

}
