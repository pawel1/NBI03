import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;



public class WebCrawler
{
	private static final String HTML_A_TAG_PATTERN = "(?i)<a([^>]+)>(.+?)</a>";
	private static final String HTML_A_HREF_TAG_PATTERN = "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";
	private Pattern patternATag;
	private Pattern patternLink;
	private ArrayList<URL> urlList;
	private int listIndex; 
	private IndexWriter indexWriter;
	private DirectoryReader reader;
	private Directory dir;
	private Analyzer anal;
	private static Version version = Version.LUCENE_40;

	
	public WebCrawler() 
	{
		// TODO: improve pattern for URL retrieving
		// does not work on most links
		this.patternATag = Pattern.compile(HTML_A_TAG_PATTERN);
		this.patternLink = Pattern.compile(HTML_A_HREF_TAG_PATTERN);
		this.urlList = new ArrayList<URL>();
		this.listIndex = 0;
		
		// init index:
		anal = new StandardAnalyzer(version);
		IndexWriterConfig conf = new IndexWriterConfig(version, anal);
		try
		{
			File workpath = new File(".");
			dir = FSDirectory.open(new File(workpath.getAbsolutePath()+"\\index"));
			workpath.delete();		
			
			this.indexWriter = new IndexWriter(dir,conf);
			// clear index from last runs
			this.indexWriter.deleteAll();
		} 
		catch (IOException e){e.printStackTrace();}
	}
	
	private void crawl(URL url)
	{	
		try
		{
			URLConnection connection = url.openConnection();
			InputStream is = connection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			
			
			String line;
			while((line = br.readLine())!= null) 
			{
				Matcher resultSet = patternATag.matcher(line);
				while(resultSet.find())
				{
					String aTag = resultSet.group();
					resultSet = patternLink.matcher(aTag);
//					System.out.println(aTag);
					while(resultSet.find())
					{
						String href = resultSet.group();
						href = href.replaceAll("href=", "");
//						System.out.println(href);
						href = href.replace("\"", "");
//						System.out.println(href);
						
						URL finalURL = new URL(href);
						if(!urlList.contains(finalURL)) 
						{
							urlList.add(finalURL);
							Document doc = new Document();
							doc.add(new StoredField("url",href));
							doc.add(new StoredField("text","hi"));
//							System.out.println((doc.getBinaryValue("text")));
							System.out.println((doc.toString()));
							indexWriter.addDocument(doc);
						}
					}
				}
			}
			// rekursive call of crawler, picks next unvisited url inline:
			if(listIndex < urlList.size()-1) crawl(urlList.get(++listIndex));
		}
		catch (IOException e){e.printStackTrace();}	
	}
	
	public void searchIndex(String key) 
	{
		try
		{			
			reader = DirectoryReader.open(dir);
		} 
		catch (IOException e){e.printStackTrace();}
		
		
		IndexSearcher searcher = new IndexSearcher(reader);
		QueryParser parser = new QueryParser(Version.LUCENE_40, "text", anal);
		// TODO: quere doesnt get results
		try
		{
			Query query = parser.parse(key);
			System.out.println(searcher.search(query, 10).totalHits);
		    ScoreDoc[] hits = searcher.search(query, null, 100).scoreDocs;
		    System.out.println("hits: "+hits.length);
		    for (int i = 0; i < hits.length; i++) 
		    {
		    	System.out.println("jest!!!");
		        Document hitDoc = searcher.doc(hits[i].doc);
		        System.out.println(hitDoc.toString());
		    }
		    reader.close();
		    dir.close();
		} 
		catch (ParseException e){e.printStackTrace();}
		catch (IOException e){e.printStackTrace();}
		
	}
	
	public static void main(String[] args) 
	{
		WebCrawler crawler = new WebCrawler();
		try
		{
			crawler.crawl(new URL("http://www.udacity.com/cs101x/index.html"));
//			System.out.println(crawler.urlList.toString());
//			System.out.println(crawler.indexWriter.numDocs());
			crawler.indexWriter.commit();
			crawler.searchIndex("text:hi");
			crawler.indexWriter.close();

		} 
		catch (MalformedURLException e){e.printStackTrace();} 
		catch (IOException e){e.printStackTrace();
		}
	}

}
