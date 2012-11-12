package ex2;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class QueryEngine
{
	private DirectoryReader reader;
	private Directory dir;
	private Analyzer anal;
	private static Version version = Version.LUCENE_40;

	public QueryEngine() throws IOException
	{
		File workpath = new File(".");
		dir = FSDirectory.open(new File(workpath.getAbsolutePath() + "\\index"));
		workpath.delete();
		anal = new StandardAnalyzer(version);
	}

	public List<Document> searchIndex(String queryString) throws IOException,
			ParseException
	{
		List<Document> result = new Vector<Document>();
		
		reader = DirectoryReader.open(dir);

		IndexSearcher searcher = new IndexSearcher(reader);
		QueryParser titleParser = new QueryParser(Version.LUCENE_40, "title", anal);
		QueryParser bodyParser = new QueryParser(Version.LUCENE_40, "full_body", anal);

		Query titleQuery = titleParser.parse(queryString);
		Query bodyQuery = bodyParser.parse(queryString);
		ScoreDoc[] bodyHits = searcher.search(bodyQuery, 10).scoreDocs;
		ScoreDoc[] titleHits = searcher.search(titleQuery, 10).scoreDocs;
		
		for (int i = 0; i < titleHits.length; i++)
		{
			//System.out.print("jest!!!\t\t");
			Document hitDoc = searcher.doc(titleHits[i].doc);
			//System.out.println(hitDoc.toString());
			result.add(hitDoc);			
		}
		for (int i = 0; i < bodyHits.length; i++)
		{
			//System.out.print("jest!!!\t\t");
			Document hitDoc = searcher.doc(bodyHits[i].doc);
			//System.out.println(hitDoc.toString());
			result.add(hitDoc);			
		}
		reader.close();
		dir.close();
		
		return result;
	}
}
