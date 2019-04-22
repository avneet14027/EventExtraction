package it.unibz.inf.stuffie;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.Map;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

public class QueryDB {

	static Driver driver;
	
	 public static void main(String args[]) {
		
			AccessDatabase("bolt://localhost:7687", "neo4j", "123456");
			
			String query1 = "MATCH (n) where n.lemma contains 'death' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid,x.headline";
			String query2 = "MATCH (n) where n.lemma contains 'protest' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid";
			String query3 = "MATCH (n) where n.lemma contains 'strike' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid";
			String query4 = "MATCH (n) where n.lemma contains 'meet' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid";
			String query5 = "MATCH (n) where n.lemma contains 'bomb' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid";
			String query6 = "MATCH (n) where n.lemma contains 'stone pelt' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid";
			String query7 = "MATCH (n) where n.lemma contains 'pellet' " + "OPTIONAL MATCH (x)--(n) " + "return x.location,x.date,x.sentence,n.name,x.dbid";
			
			//String query8 = "MATCH (n) where date(n.date) = date('25-03-2016')" + "return n.sentence,n.date,n.location";
			String query8 = "MATCH (n) where n.location contains 'Kashmir' " + "return n.sentence,n.date,n.location,n.dbid,n.headline";
			
			
			tryQuery(query1);
			
			closeDatabase();
	 }
	
	 public static void AccessDatabase(String uri, String user, String password){
	        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
	 }
	 
	 public static void tryQuery(String query) {
		 String query_result = "";
		 try (Session session = driver.session())
	        {
	             
	    			StatementResult result = session.run(query);
	    	        while (result.hasNext()){
	    	        	Record record = result.next();
	    	        	Map<String,Object> map = record.asMap();
	    	        	for (Map.Entry<String,Object> entry : map.entrySet())  
	    	                System.out.println("Key: " + entry.getKey() + 
	    	                                 ", Value: " + entry.getValue()); 
	    	        	System.out.println("\n\n");
	    	        	//query_result = record.toString();
		    	        //System.out.println(query_result);
	    	        }
	                    	    	        
	    		session.close();
	        }

	 }
    public static void closeDatabase(){
        // Closing a driver immediately shuts down all open connections.
        driver.close();
    }
	
}
