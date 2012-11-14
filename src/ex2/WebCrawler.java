package ex2;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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

import javax.swing.text.*;
import javax.swing.text.html.*;

public class WebCrawler
{
	private static final String BODY_PATTERN = "(?i)<body>((.|\n)*?)</body>";
	private static final String TITLE_PATTERN = "(?i)<title>((.|\n)*?)</title>";
	private static final int MAX_NUMBER_LINKS = 100;
	
	private Pattern patternBody;
	private Pattern patternTitle;
	private ArrayList<URL> urlList;
	private int listIndex;
	private IndexWriter indexWriter;
	private Directory dir;
	private Analyzer anal;
	private static Version version = Version.LUCENE_40;

	public WebCrawler()
	{
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

	public void crawl (URL url) throws IOException, URISyntaxException
	{
		try {
			startCrawling(url);
		} catch(Exception e) {
			//invalid link? ignore it
		}
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
		
		
		EditorKit kit = new HTMLEditorKit();
		javax.swing.text.Document doc2 = kit.createDefaultDocument();

		// The Document class does not yet 
		// handle charset's properly.
		doc2.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
		
		try {

			// Create a reader on the HTML content.
			Reader rd = getReader(currentUri.toASCIIString());

			// Parse the HTML.
			kit.read(rd, doc2, 0);

			ElementIterator it = new ElementIterator(doc2);
			javax.swing.text.Element elem;
			while ((elem = it.next()) != null) {
				// recursive call of crawler, picks next unvisited url inline:
				
				MutableAttributeSet s = (MutableAttributeSet)
				elem.getAttributes().getAttribute(HTML.Tag.A);
				
				//System.out.println(s);
				
				String link = currentUri.toASCIIString();
				
				if (s != null) {
					if ((link = (String) s.getAttribute(HTML.Attribute.HREF)) != null) {
						if (link.startsWith("//")) {
							link = url.getProtocol() + ":" + link;
						} else if (link.startsWith("/") || link.startsWith("#")) {
							link = url.getProtocol().trim()+"://" + url.getAuthority().trim() + link.trim();
						}
						
						URI foundURI = new URI(link).normalize();
						
						if (!foundURI.isAbsolute()) {
							foundURI = currentUri.resolve(foundURI);
						}
						
						try {
							URL finalURL = foundURI.toURL();
							
							System.out.println(finalURL.toString());
							
							if (!urlList.contains(finalURL))
							{
								urlList.add(finalURL);
							}
						} catch (Exception e) {
							//invalid url? ignore it
						}
					}
				}			
			}
			if (listIndex < urlList.size() && urlList.size() < MAX_NUMBER_LINKS)
			{
				startCrawling(urlList.get(listIndex++));
			}
		} catch (Exception e) {
			//e.printStackTrace();
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

	private static String convertStreamToString(InputStream is)
	{
		Scanner s = new Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}
	

	private static Reader getReader(String uri) throws IOException {
		
		if (uri.startsWith("http:")) {
			URLConnection conn = new URL(uri).openConnection();
			return new InputStreamReader(conn.getInputStream());
		} else {
			return new FileReader(uri);
		}
	}

}
