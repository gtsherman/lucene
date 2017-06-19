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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.retrievable.lucene.searching.expansion.Rocchio;
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
		options.addOption("n", "count", true, "Number of search results to return");
		options.addOption("e", "expand", false, "Whether to expand the query");
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cl = parser.parse(options, args);
			
			String indexPath = "./index";
			if (cl.hasOption('i')) {
				indexPath = cl.getOptionValue('i');
			}
			
			int numResults = 1000;
			if (cl.hasOption('n')) {
				numResults = Integer.parseInt(cl.getOptionValue('n'));
			}
			
			String queryPath = cl.getOptionValue('q');
			List<QueryConfig> queries = readQueries(queryPath);
			
			boolean expandQueries = false;
			if (cl.hasOption('e')) {
				expandQueries = true;
			}
			Rocchio expander = new Rocchio();
			
			try {
				IndexSearcher index = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(indexPath))));
				index.setSimilarity(new BM25Similarity(1.2f, 0.75f));
				for (QueryConfig query : queries) {
					if (expandQueries) {
						expander.expandQuery(index, query, 20, 20);
					}
					
					ScoreDoc[] results = search(index, query, numResults);
					
					for (int i = 0; i < results.length; i++) {
						ScoreDoc result = results[i];
						System.out.println(query.getNumber() + " Q0 " + index.doc(result.doc).getField("docno").stringValue() + " " + (i + 1) + " "  + result.score + " test");
					}
				}
			} catch (IOException e) {
				System.err.println("Index cannot be read.");
				e.printStackTrace(System.err);
				System.exit(-1);
			}

		} catch (ParseException e) {
			System.err.println("Error parsing command line arguments. Unable to proceed.");
			System.exit(-1);
		}
	}
	
	public static List<QueryConfig> readQueries(String queryPath) {
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
		return queries;
	}
	
	public static ScoreDoc[] search(IndexSearcher index, QueryConfig queryConfig, int numResults) throws IOException {
		Query query;
		try {
			QueryParser queryParser = new QueryParser("text", new StandardAnalyzer());
			query = queryParser.parse(queryConfig.getText());
		} catch (org.apache.lucene.queryparser.classic.ParseException e) {
			System.err.println("Query cannot be parsed: " + queryConfig.getText());
			return null;
		}
		
		return index.search(query, numResults).scoreDocs;
	}

}
