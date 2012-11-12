package ex2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

public class Main
{
	private static String START_URL = "http://www.udacity.com/cs101x/index.html";
	private static String GOOGLE = "http://google.de";
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws URISyntaxException 
	 */
	public static void main(String[] args) throws IOException, ParseException, URISyntaxException
	{
		WebCrawler crawler = new WebCrawler();
		crawler.crawl(new URL(START_URL));

		QueryEngine qe = new QueryEngine();
		List<Document> results = qe.searchIndex("learn*");

		printSearchResults(results);
	}
	
	private static void printSearchResults (List<Document> results)
	{
		for (Document doc : results)
		{
			System.out.println("===============================");
			System.out.println(doc.get("url"));
			System.out.println();
			System.out.println(doc.get("short_body"));
		}
	}

}
