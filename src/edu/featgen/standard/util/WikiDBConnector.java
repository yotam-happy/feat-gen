package edu.featgen.standard.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 
 * Singleton JDBC handler for connecting to my wiki DB
 *
 */
public class WikiDBConnector {

	private static final int TERMS_PER_QUERY = 3000;
	private static WikiDBConnector dbConnector = null;
	
	private Connection connect = null;
	
	private WikiDBConnector(String host, String dbName, String user, String password) {
		try {
			// This will load the MySQL driver, each DB has its own driver
			Class.forName("com.mysql.jdbc.Driver");
		    // Setup the connection with the DB
		    connect = DriverManager.getConnection("jdbc:mysql://" + host + "/" + dbName + "?"
		    		+ "user=" + user + "&password=" + password);
		} catch (ClassNotFoundException | SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static synchronized void initialize(String host, String dbName, String user, String password) {
		dbConnector = new WikiDBConnector(host, dbName, user, password);
	}
	
	public static WikiDBConnector getInstance() {
		return dbConnector;
	}
	
	/**
	 * The returned map has an entry for each word in bow. The key is the word and the
	 * value is the wikipedia article id
	 */
	public Map<String, Integer> mapBOWToWikiConcepts(Set<String> bow) {
		Map<String, Integer> ret = new HashMap<String, Integer>();
		
		Iterator<String> iter = bow.iterator();
		while(iter.hasNext()) {
			StringBuffer sb = new StringBuffer();
			sb.append("SELECT id, title_stemmed from pages where title_stemmed in (");
			List<String> words = new ArrayList<String>();
			int n = 0;
			while (iter.hasNext() && n < TERMS_PER_QUERY) {
				words.add(iter.next());
				sb.append(n != 0 ? ",?" : "?");
				n++;
			}
			sb.append(")");
			
			try {
				PreparedStatement preparedStatement = 
						connect.prepareStatement(sb.toString());
				for (int i = 0; i < words.size(); i++) {
					preparedStatement.setString(i+1, words.get(i));
				}
				ResultSet resultSet = preparedStatement.executeQuery();
				
				while (resultSet.next()) {
					String titleStemmed = resultSet.getString("title_stemmed");
					Integer id = resultSet.getInt("id");
					ret.put(titleStemmed, id);
				}
				preparedStatement.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		return ret;
	}
	/**
	 * The returned map has multiple entries for each item in from (article/category). 
	 * The key is the id in from and the values are the wikipedia category ids it is in
	 */
	public Map<Integer, Integer> getCategories(Collection<Integer> from) {
		Map<Integer, Integer> ret = new HashMap<Integer, Integer>();
		
		Iterator<Integer> iter = from.iterator();
		while(iter.hasNext()) {
			StringBuffer sb = new StringBuffer();
			sb.append("SELECT from_id, to_id from categorylinks where from_id in (");
			List<Integer> ids = new ArrayList<Integer>();
			int n = 0;
			while (iter.hasNext() && n < TERMS_PER_QUERY) {
				ids.add(iter.next());
				sb.append(n != 0 ? ",?" : "?");
				n++;
			}
			sb.append(")");

			try {
				PreparedStatement preparedStatement = 
						connect.prepareStatement(sb.toString());
				for (int i = 0; i < ids.size(); i++) {
					preparedStatement.setInt(i+1, ids.get(i));
				}
				ResultSet resultSet = preparedStatement.executeQuery();
				
				while (resultSet.next()) {
					Integer ret_from_id = resultSet.getInt("from_id");
					Integer to_id = resultSet.getInt("to_id");
					ret.put(ret_from_id, to_id);
				}
				preparedStatement.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		return ret;
	}
}
