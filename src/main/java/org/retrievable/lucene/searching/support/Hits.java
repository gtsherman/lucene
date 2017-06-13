package org.retrievable.lucene.searching.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.retrievable.lucene.searching.support.Hits.Hit;

public class Hits implements Iterable<Hit> {
	
	private Map<Integer, Hit> hits;
	
	public Hits() {
		this.hits = new HashMap<Integer, Hit>();
	}
	
	public void combineHits(Hits hits) {
		for (Integer hitId : hits.hits.keySet()) {
			Hit hit = hits.getHit(hitId);
			for (String term : hit.termScores.keySet()) {
				setTermScore(term, hit.getTermScore(term), hitId);
			}
		}
	}
	
	public void setTermScore(String term, double score, int doc) {
		if (!hits.containsKey(doc)) {
			hits.put(doc, new Hit(doc));
		}
		hits.get(doc).setTermScore(term, score);
	}
	
	public Hit getHit(int id) {
		return hits.containsKey(id) ? hits.get(id) : new Hit(id);
	}

	public Iterator<Hit> iterator() {
		return hits.values().iterator();
	}
	
	public List<Hit> getRankedHits() {
		List<Hit> rankedHits = new ArrayList<Hit>(hits.values());
		Collections.sort(rankedHits, new Comparator<Hit>() {
			public int compare(Hit h1, Hit h2) {
				if (h1.score > h2.score) {
					return -1;
				}
				if (h1.score < h2.score) {
					return 1;
				}
				return 0;
			}
		});
		return rankedHits;
	}
	
	public class Hit {
		
		public int id;
		public String docno;
		public double score;
		private Map<String, Double> termScores;
		
		public Hit(int id) {
			this.id = id;
			this.termScores = new HashMap<String, Double>();
		}
		
		public void setTermScore(String term, double score) {
			this.termScores.put(term, score);
		}
		
		public double getTermScore(String term) {
			return termScores.containsKey(term) ? termScores.get(term) : 0.0;
		}
	}

}