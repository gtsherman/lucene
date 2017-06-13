package org.retrievable.lucene.searching;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.retrievable.lucene.searching.similarities.OurDirichletSimilarity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.gslis.lucene.main.config.QueryConfig;

public class Searcher {

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("i", "index", true, "Location to create index");
		options.addOption("q", "queries", true, "File containing queries in Indri format");
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cl = parser.parse(options, args);
			
			String indexPath = "./index";
			if (cl.hasOption('i')) {
				indexPath = cl.getOptionValue('i');
			}
			
			String queryPath = cl.getOptionValue('q');
			List<QueryConfig> queries = new ArrayList<QueryConfig>();
			try {
	    	    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	            Document doc = docBuilder.parse (new File(queryPath));
	            NodeList queryNodes = doc.getElementsByTagName("query");
	            for (int i=0; i<queryNodes.getLength(); i++) {
	                Node node = queryNodes.item(i);
	                if(node.getNodeType() == Node.ELEMENT_NODE) {
	                    Element query = (Element)node;
	                    String number = query.getElementsByTagName("number").item(0).getFirstChild().getNodeValue();;
	                    String text = query.getElementsByTagName("text").item(0).getFirstChild().getNodeValue();
	                    text = text.replaceAll("\\\n", "");

	                    QueryConfig qquery = new QueryConfig();
	                    qquery.setNumber(number);
	                    qquery.setText(text);
	                    
	                    queries.add(qquery);
	                }
	            }
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
			
		//	Similarity similarity = new LMDirichletSimilarity(2500);
			Similarity similarity = new OurDirichletSimilarity(2500);
			
			try {
				IndexSearcher index = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(indexPath))));
				index.setSimilarity(similarity);
			
				for (QueryConfig query : queries) {

					search(index, query);
				}
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
	
	public static void search(IndexSearcher index, QueryConfig queryConfig) throws IOException, org.apache.lucene.queryparser.classic.ParseException {
		QueryParser queryParser = new QueryParser("text", new StandardAnalyzer());
		Query query = queryParser.parse(queryConfig.getText());
		
		TopDocs topdocs = index.search(query, 1000);
		ScoreDoc[] results = topdocs.scoreDocs;
		
		System.err.println("Query: " + query.toString());
		System.err.println("Query type: " + query.getClass().getName());

		Explanation explanation = index.explain(query, results[0].doc);
		System.err.println("Scoring " + queryConfig.getNumber() + " against doc " + index.doc(results[0].doc).getField("docno").stringValue());
		System.err.println(explanation);

		explanation = index.explain(query, results[1].doc);
		System.err.println("Scoring " + queryConfig.getNumber() + " against doc " + index.doc(results[1].doc).getField("docno").stringValue());
		System.err.println(explanation);

		for (int i = 0; i < results.length; i++) {
			ScoreDoc result = results[i];
			System.out.println(queryConfig.getNumber() + " Q0 " + index.doc(result.doc).getField("docno").stringValue() + " " + (i + 1) + " "  + result.score + " test");
		}

	}

}
