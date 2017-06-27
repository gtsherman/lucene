package org.retrievable.lucene.searching.expansion;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.retrievable.lucene.searching.Searcher;

import edu.gslis.lucene.main.config.QueryConfig;
import edu.gslis.textrepresentation.FeatureVector;

public class Rocchio {
	
	private double alpha;
	private double beta;
	private double k1;
	private double b;
	
	/**
	 * Default parameter values taken from:
	 * https://nlp.stanford.edu/IR-book/html/htmledition/the-rocchio71-algorithm-1.html
	 */
	public Rocchio() {
		this(1.0, 0.75);
	}
	
	public Rocchio(double alpha, double beta) {
		this(alpha, beta, 1.2, 0.75);
	}
	
	public Rocchio(double alpha, double beta, double k1, double b) {
		this.alpha = alpha;
		this.beta = beta;
		this.k1 = k1;
		this.b = b;
	}
	
	public void expandQuery(IndexSearcher index, QueryConfig query, int fbDocs, int fbTerms) throws IOException {
		ScoreDoc[] initialResults = Searcher.search(index, query, fbDocs);
		
		FeatureVector summedTermVec = new FeatureVector(null);
		
		for (ScoreDoc doc : initialResults) {
			StringBuffer docText = new StringBuffer();
			for (String chunk : index.doc(doc.doc).getValues("text")) {
				docText.append(chunk);
			}

			// Get the document tokens and add to the doc vector
			FeatureVector docVec = new FeatureVector(null);
			parseText(docText.toString(), docVec);
			
			// Compute the BM25 weights
			computeBM25Weights(index, docVec, summedTermVec);
		}
		
		// Multiply the summed term vector by beta / |Dr|
		FeatureVector relDocTermVec = new FeatureVector(null);
		for (String term : summedTermVec) {
			relDocTermVec.addTerm(term, summedTermVec.getFeatureWeight(term) * beta / fbDocs);
		}
		
		// Create a query vector and scale by alpha
		FeatureVector rawQueryVec = new FeatureVector(null);
		parseText(query.getText(), rawQueryVec);
		
		FeatureVector summedQueryVec = new FeatureVector(null);
		computeBM25Weights(index, rawQueryVec, summedQueryVec);
		
		FeatureVector queryTermVec = new FeatureVector(null);
		for (String term : rawQueryVec) {
			queryTermVec.addTerm(term, summedQueryVec.getFeatureWeight(term) * alpha);
		}
		
		// Combine query and rel doc vectors
		for (String term : queryTermVec) {
			relDocTermVec.addTerm(term, queryTermVec.getFeatureWeight(term));
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
	
	private void computeBM25Weights(IndexSearcher index, FeatureVector docVec, FeatureVector summedTermVec) throws IOException {
		for (String term : docVec) {
			int docCount = index.getIndexReader().numDocs();
			int docOccur = index.getIndexReader().docFreq(new Term("text", term));
			double avgDocLen = index.getIndexReader().getSumTotalTermFreq("text") / docCount;
			
			double idf = Math.log( (docCount + 1) / (docOccur + 0.5) ); // following Indri
			double tf = docVec.getFeatureWeight(term);
			
			double weight = (idf * k1 * tf) / (tf + k1 * (1 - b + b * docVec.getLength() / avgDocLen));
			summedTermVec.addTerm(term, weight);
		}
	}

}
