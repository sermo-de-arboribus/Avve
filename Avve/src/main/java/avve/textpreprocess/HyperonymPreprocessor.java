package avve.textpreprocess;

import java.sql.*;
import java.util.*;

import org.apache.logging.log4j.Logger;

import avve.epubhandling.EbookContentData;
import avve.textpreprocess.hyperonym.HyperonymProperties;
import avve.textpreprocess.hyperonym.HyperonymPropertyName;

/**
 * This class uses a MySQL database connection to try retrieving hyperonyms for all (non-lemmatized) words of an EbookContentData's text.
 * The database connection properties are handled through the helper classes in the avve.textpreprocess.hyperonym package.
 * 
 * The algorithm used for hyperonym retrieval is based on:
 * Bloehdorn / Hotho: Boosting for Text Classification with Semantic Features, in: B. Mobasher et al. (Eds.): WebKDD 2004, LNAI 3932, pp. 149-166
 * 
 * @author Kai Weber
 *
 */
public class HyperonymPreprocessor implements TextPreprocessor
{
	private static final ResourceBundle errorMessageBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	private static final String hyperonymQueryTemplate = 
			"SELECT sl.synset_id, targsynset.synset_preferred_term, lt.link_name, t2.word AS target_word " 
			+ " FROM synset_link sl "
			+ "  INNER JOIN synset s ON s.id = sl.synset_id "
			+ "  INNER JOIN link_type lt ON sl.link_type_id = lt.id "
			+ "  LEFT JOIN term t ON sl.synset_id = t.synset_id " 
			+ "  LEFT JOIN term t2 ON sl.target_synset_id = t2.synset_id "
			+ "  INNER JOIN synset targsynset ON t2.synset_id = targsynset.id "
			+ "WHERE s.is_visible "
			+ "  AND lt.id = 1 "
			+ "  AND t.word = ?;";
	private static final String categoryQueryTemplate = "SELECT category_name, t.word "
			+ "FROM category cat "
			+ "INNER JOIN category_link cl on cat.id = cl.category_id "
			+ "LEFT JOIN synset syn ON cl.synset_id = syn.id "
			+ "LEFT JOIN term t ON t.synset_id = syn.id "
			+ "WHERE t.word = ?;";
	
	private Logger logService;
	private HyperonymProperties hyperonymProperties;
	
	public HyperonymPreprocessor(final Logger logService)
	{
		this.logService = logService;
		hyperonymProperties = HyperonymProperties.getInstance(this.logService);
		loadMysqlDriver();
		this.logService.debug(infoMessagesBundle.getString("avve.textpreprocess.hyperonymPreprocessorCreated"));
	}

	@Override
	public String getName()
	{
		return "HyperonymPreprocessor";
	}

	@Override
	public void process(EbookContentData contentData)
	{
		if(contentData.getTokens() == null || contentData.getTokens().length == 0)
		{
			logService.warn(String.format(errorMessageBundle.getString("avve.textpreprocess.noTokensAvailable"), getName() + ".process()"));
		}
		else
		{
			SortedMap<String, Integer> hyperonymFrequencies = new TreeMap<String, Integer>();
			SortedMap<String, Integer> terms = new TreeMap<String, Integer>();
			// An in-memory cache to reduce number of DB lookups
			HashSet<String> termCache = new HashSet<String>();
			
			Connection dbConnection = getMysqlDbConnnection();
			PreparedStatement sqlStatement = null;
			int numberOfLookups = 0;
			
			try
			{
				sqlStatement = dbConnection.prepareStatement("SELECT * from term t WHERE t.word LIKE ?");
				
				// determine tokens which exist in the thesaurus
				int windowsize = 3;
				
				for(String[] sentence : contentData.getTokens())
				{
					// this part is based on an algorithm published in Bloehdorn / Hotho: Boosting for Text Classification with Semantic Features, in: B. Mobasher et al. (Eds.): WebKDD 2004, LNAI 3932, pp. 149-166.
					int i = 1;
					while(i < sentence.length)
					{
						for(int j = Math.min(windowsize, sentence.length - i + 1); j > 0; j--)
						{
							String[] currentWindow = Arrays.copyOfRange(sentence, i-1, i + j - 1);
							String currentTerm = String.join(" ", currentWindow);
							
							// this currentTerm has already been found in the hyperonym database; just increment the term's count
							if(termCache.contains(currentTerm) && terms.containsKey(currentTerm))
							{
								terms.put(currentTerm, terms.get(currentTerm) + 1);
								i = i + j;
							}
							// this currentTerm has already been looked up and is not found in the database; just ignore it
							else if (termCache.contains(currentTerm))
							{
								if ( j == 1)
								{
									i = i + j;
								}	
							}
							// look up current term in database
							else
							{
								sqlStatement.setString(1, currentTerm);
								ResultSet rs = sqlStatement.executeQuery();
								numberOfLookups++;
								termCache.add(currentTerm);
								
								if(rs.first())
								{
									if(terms.containsKey(currentTerm))
									{
										terms.put(currentTerm, terms.get(currentTerm) + 1);
									}
									else
									{
										terms.put(currentTerm, 1);
									}
									i = i + j;
									break;
								}
								else if ( j == 1)
								{
									i = i + j;
								}	
							}
						}	
					}
				}
				
				// determine categories and hyperonyms
				for(String term : terms.keySet())
				{	
					PreparedStatement categoryStatement = dbConnection.prepareStatement(categoryQueryTemplate);
					PreparedStatement hyperonymStatement = dbConnection.prepareStatement(hyperonymQueryTemplate);
					
					categoryStatement.setString(1, term);
					hyperonymStatement.setString(1, term);
					
					try
					{
						ResultSet categoryQueryResult = categoryStatement.executeQuery();
						numberOfLookups++;
						
						while(categoryQueryResult.next())
						{
							String categoryName = categoryQueryResult.getString(1);
							
							if(hyperonymFrequencies.containsKey(categoryName))
							{
								// we add the number of term occurrences to the category name count
								hyperonymFrequencies.put(categoryName, hyperonymFrequencies.get(categoryName) + terms.get(term));
							}
							else
							{
								hyperonymFrequencies.put(categoryName, terms.get(term));
							}
						}
						
						ResultSet hyperonymQueryResult = hyperonymStatement.executeQuery();
						numberOfLookups++;
						
						if(hyperonymQueryResult.first())
						{
							// if the DB has a preferred term for the synset, use this one. Otherwise only use the first term of a synset.
							String preferredTerm = hyperonymQueryResult.getString(2);
							String hyperonymName = hyperonymQueryResult.getString(4);
							String termToUse = (null != preferredTerm && preferredTerm.length() > 0) ? preferredTerm : hyperonymName;
							
							if(hyperonymFrequencies.containsKey(termToUse))
							{
								// we add the number of term occurrences to the category name count
								hyperonymFrequencies.put(termToUse, hyperonymFrequencies.get(termToUse) + terms.get(term));
							}
							else
							{
								hyperonymFrequencies.put(hyperonymName, terms.get(term));
							}
						}
					}
					catch(SQLException exc)
					{
						logService.error(errorMessageBundle.getString("avve.textpreprocess.dbStatementException"));
						logService.error(exc);
					}
					finally
					{
						categoryStatement.close();
						hyperonymStatement.close();
					}
				}
				
				sqlStatement.close();
				dbConnection.close();
			}
			catch (SQLException exc)
			{
				logService.error(errorMessageBundle.getString("avve.textpreprocess.dbStatementException"));
				logService.error(exc);
			}
			finally
			{
				logService.info(String.format(infoMessagesBundle.getString("avve.textpreprocess.dbLookupsPerformed"), numberOfLookups));
			}
			
			contentData.setHyperonymFrequencies(hyperonymFrequencies);
		}
	}

	private Connection getMysqlDbConnnection()
	{
		String dbHost = hyperonymProperties.getProperty(HyperonymPropertyName.DB_HOST);
		String dbUser = hyperonymProperties.getProperty(HyperonymPropertyName.DB_USER);
		String dbPassword = hyperonymProperties.getProperty(HyperonymPropertyName.DB_PASSWORD);
		String dbUrl = "jdbc:mysql://"+dbHost+":3306/openthesaurus?useSSL=false&requireSSL=false";
		
		try
		{
			Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
			return conn;
		}
		catch (SQLException exc)
		{
			logService.error(String.format(errorMessageBundle.getString("avve.textpreprocess.dbConnectionException"), dbUrl));
			logService.error(exc);
			
		    System.out.println("SQLState: " + exc.getSQLState());
		    System.out.println("VendorError: " + exc.getErrorCode());
		    exc.printStackTrace();
		    
			return null;
		}
	}

	private void loadMysqlDriver()
	{
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		}
		catch (Exception exc)
		{
			logService.error(String.format(errorMessageBundle.getString("avve.textpreprocess.dbDriverLoadException"), getName()));
		}
	}
}