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
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;
import org.retrievable.lucene.searching.support.Hits;
import org.retrievable.lucene.searching.support.Hits.Hit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.gslis.lucene.main.config.QueryConfig;

public class ManualSearcher {

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
			
			try {
				DirectoryReader index = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			
				for (QueryConfig query : queries) {
					Hits searchHits = (new Search()).search(index, query);
					List<Hit> results = searchHits.getRankedHits();
					
					for (int i = 0; i < Math.min(results.size(), 1000); i++) {
						Hit result = results.get(i);
						System.out.println(query.getNumber() + " Q0 " + index.document(result.id).getField("docno").stringValue() + " " + (i + 1) + " "  + result.score + " test");
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
		} catch (org.apache.lucene.queryparser.classic.ParseException e) {
			System.err.println("Error parsing query. Unable to proceed.");
			System.exit(-1);
		}
	}

}
