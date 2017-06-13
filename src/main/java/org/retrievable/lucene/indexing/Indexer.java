package org.retrievable.lucene.indexing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Indexer {

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("i", "index", true, "Location to create index");
		options.addOption("d", "data", true, "Location of raw data to be indexed");
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cl = parser.parse(options, args);
			

			String indexPath = "./index";
			if (cl.hasOption('i')) {
				indexPath = cl.getOptionValue('i');
			}
			
			String dataPath = cl.getOptionValue('d');
			
			index(indexPath, dataPath);
		} catch (ParseException e) {
			System.err.println("Error parsing command line arguments. Unable to proceed.");
			System.exit(-1);
		}
	}
	
	public static void index(String indexPath, String docsPath) {
		try {
			Directory dir = FSDirectory.open(Paths.get(indexPath));
			Analyzer analyzer = new StandardAnalyzer();

			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setOpenMode(OpenMode.CREATE);
			
			IndexWriter writer = new IndexWriter(dir, iwc);
			for (File file : (new File(docsPath)).listFiles()) {
				parseDocs(file.getAbsolutePath(), writer);				
			}
			writer.close();
		} catch (IOException e) {
			System.err.println("Unable to open index location. Exiting.");
			System.exit(-1);
		}
	}
	
	public static void parseDocs(String trecTextFile, IndexWriter writer) throws IOException {
		FieldType storeTermVectors = new FieldType(TextField.TYPE_STORED);
		storeTermVectors.setStoreTermVectors(true);

		String data = new String(Files.readAllBytes(Paths.get(trecTextFile)));
		data = data.replaceAll("(&(?!amp;))", "&amp;" ); 
		
        // Add a root element
        String xml = "<?xml version=\"1.1\"?><root>" + data + "</root>";

        try {
			DocumentBuilderFactory factory =  DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			org.w3c.dom.Document xmlDoc = builder.parse(new ByteArrayInputStream(xml.getBytes()));

			NodeList docs = xmlDoc.getElementsByTagName("DOC");
			for (int i=0; i<docs.getLength(); i++) {
				Element doc = (Element) docs.item(i);

				Document luceneDoc = new Document();

				/*NodeList fields = doc.getChildNodes();
				for (int j = 0; j < fields.getLength(); j++) {
					Node field = fields.item(j);
					String fieldName = field.getNodeName().toLowerCase();
					String fieldContents = field.getTextContent();
					
					luceneDoc.add(new TextField(fieldName, fieldContents, Field.Store.YES));
				}*/
				Node docno = doc.getElementsByTagName("DOCNO").item(0);
				luceneDoc.add(new StringField("docno", docno.getTextContent().trim(), Field.Store.YES));				
				
				Node text = doc.getElementsByTagName("TEXT").item(0);
				luceneDoc.add(new Field("text", text.getTextContent().trim(), storeTermVectors));
				
				Analyzer analyzer = writer.getAnalyzer();
				TokenStream tokens = analyzer.tokenStream("text", text.getTextContent());
				tokens.reset();                            
	            int docLength = 0;                            
	            while (tokens.incrementToken()) {
	                docLength++;
	            }
	            tokens.end();
	            tokens.close();
	            
				luceneDoc.add(new StoredField("length", docLength));
				
				writer.addDocument(luceneDoc);
			}
		} catch (ParserConfigurationException e) {
			System.err.println("Error parsing documents.");
			System.exit(-1);
		} catch (SAXException e) {
			System.err.println("Error parsing file: " + trecTextFile + ". Continuing.");
			System.err.println(xml);
			System.exit(-1);
		}
	}

}
