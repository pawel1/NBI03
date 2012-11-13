package ex2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class WebCrawler
{
	private static final String HTML_A_TAG_PATTERN = "(?i)<a([^>]+)>(.+?)</a>";
	private static final String HTML_A_HREF_TAG_PATTERN = "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";
	private static final String BODY_PATTERN = "(?i)<body>((.|\n)*?)</body>";
	private static final String TITLE_PATTERN = "(?i)<title>((.|\n)*?)</title>";
	private Pattern patternATag;
	private Pattern patternLink;
	private Pattern patternBody;
	private Pattern patternTitle;
	private ArrayList<URL> urlList;
	private int listIndex;
	private IndexWriter indexWriter;
	private Directory dir;
	private Analyzer anal;
	private static Version version = Version.LUCENE_40;

	/**
	 * creates an instance of the WebCrawler, which enables you to crawl and
	 * index websites
	 */
	public WebCrawler()
	{
		this.patternATag = Pattern.compile(HTML_A_TAG_PATTERN);
		this.patternLink = Pattern.compile(HTML_A_HREF_TAG_PATTERN);
		this.patternBody = Pattern.compile(BODY_PATTERN);
		this.patternTitle = Pattern.compile(TITLE_PATTERN);
		this.urlList = new ArrayList<URL>();
		this.listIndex = 1;

		// init index:
		anal = new StandardAnalyzer(version);
		IndexWriterConfig conf = new IndexWriterConfig(version, anal);
		try
		{
			File workpath = new File(".");
			dir = FSDirectory.open(new File(workpath.getAbsolutePath()
					+ "\\index"));
			workpath.delete();

			this.indexWriter = new IndexWriter(dir, conf);
			// clear index from last runs
			this.indexWriter.deleteAll();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * @param url the URL, where you want to start the recursive crawling 
	 * algorithm. While crawling an index will be created at the sources rootpath
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void crawl (URL url) throws IOException, URISyntaxException
	{
		startCrawling(url);
		optimizeAndCloseWriter();
	}
	
	private void startCrawling(URL url) throws IOException, URISyntaxException
	{
		if (urlList.isEmpty())
		{
			urlList.add(url);
		}
		
		URI currentUri = url.toURI();
		
		URLConnection connection = url.openConnection();
		InputStream is = connection.getInputStream();

		String content = convertStreamToString(is);
		//System.out.println(content);

		Matcher bodyMatcher = patternBody.matcher(content);
		Matcher titleMatcher = patternTitle.matcher(content);
		Matcher aTagMatcher = patternATag.matcher(content);

		String fullBody = bodyMatcher.find() ? bodyMatcher.group() : "";
		String body = fullBody.replaceAll("<[^>]+>", " ");
		body = body.replaceAll("\\s+", " ").trim();
		
		String title = titleMatcher.find() ? titleMatcher.group() : "";

		Document doc = new Document();
		doc.add(new TextField("full_body", fullBody.trim(), Store.YES));
		doc.add(new TextField("title", title.trim(), Store.YES));
		doc.add(new TextField("url", url.toString(), Store.YES));
		doc.add(new TextField("short_body", body, Store.YES));
		indexWriter.addDocument(doc);
		//System.out.println(doc.toString());
		while (aTagMatcher.find())
		{
			String aTag = aTagMatcher.group(1);
			Matcher hrefMatcher = patternLink.matcher(aTag);
			// System.out.println(aTag);
			while (hrefMatcher.find())
			{
				String href = hrefMatcher.group();
				href = href.replaceAll("href=", "").trim();
				// System.out.println(href);
				href = href.replace("\"", "");
				href = href.replaceAll(" ", "%20");
				URI foundURI = new URI(href).normalize();
				
				if (!foundURI.isAbsolute())
				{
					foundURI = currentUri.resolve(foundURI);
				}
				
				/*
				if (href.startsWith("/") || href.startsWith("#"))
				{
					href = url.getProtocol().trim()+"://"+url.getAuthority().trim()+href.trim();
				}
				*/
				
				URL finalURL = foundURI.toURL();
				if (!urlList.contains(finalURL))
				{
					urlList.add(finalURL);
				}
			}
		}
		// recursive call of crawler, picks next unvisited url inline:
		if (listIndex < urlList.size())
		{
			startCrawling(urlList.get(listIndex++));
		}
	}

	public List<URL> getURLs()
	{
		return this.urlList;
	}
	
	private void optimizeAndCloseWriter() throws IOException
	{
		this.indexWriter.commit();
		this.indexWriter.close();
	}

	/**
	 * @param is the stream to read from
	 * @return the stream's content as one String. Used to read HTML documents
	 */
	private static String convertStreamToString(InputStream is)
	{
		Scanner s = new Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

}
