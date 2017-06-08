package org.retrievable.lucene.searching;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

public class Searcher {

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("i", "index", true, "Location to create index");
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cl = parser.parse(options, args);
			
			String stringQuery = "welfare reform";
			QueryParser queryParser = new QueryParser("text", new StandardAnalyzer());
			Query query = queryParser.parse(stringQuery);

			String indexPath = "./index";
			if (cl.hasOption('i')) {
				indexPath = cl.getOptionValue('i');
			}
			
			Similarity similarity = new LMDirichletSimilarity(2500);
			//Similarity similarity = new OurDirichletSimilarity(2500);
			
			try {
				IndexSearcher index = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(indexPath))));
				index.setSimilarity(similarity);
				search(index, query);
			} catch (IOException e) {
				System.err.println("Index cannot be read.");
				e.printStackTrace(System.err);
				System.exit(-1);
			}
			
		} catch (ParseException e) {
			System.err.println("Error parsing command line arguments. Unable to proceed.");
			System.exit(-1);
		} catch (org.apache.lucene.queryparser.classic.ParseException e) {
			System.err.println("Error parsing query. Unable to proceed.");
			System.exit(-1);
		}
	}
	
	public static void search(IndexSearcher index, Query query) throws IOException {
		TopDocs topdocs = index.search(query, 1000);
		ScoreDoc[] results = topdocs.scoreDocs;

		for (int i = 0; i < results.length; i++) {
			ScoreDoc result = results[i];
			System.out.println("103" + " Q0 " + index.doc(result.doc).getField("docno").stringValue() + " " + (i + 1) + " "  + result.score + " test");
		}

	}

}
