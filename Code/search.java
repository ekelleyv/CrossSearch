package search;

import java.sql.*;
import java.io.*;
import org.jsoup.Jsoup;
import org.json.simple.*;
import java.net.*;

/**
 *
 * @author Ed Kelley and JT Glaze
 */
public class search {

   private static final String HOST = "localhost";
   private static final String PORT = "5555";
   private static final String DATABASE = "jglaze";
   private static final String USER = "jglaze";
   private static final String PASSWORD = "jglaze";

   //Adapted from COS333 Example Code
   public static Connection get_connection() {
         try    {  // Override the host if DB_SERVER_HOST is set.
         String host = System.getenv("DB_SERVER_HOST");
         if (host == null) host = HOST;

         Class.forName("com.mysql.jdbc.Driver");
         String url =
            "jdbc:mysql://" + host + ":" + PORT + "/" + DATABASE;

         Connection connection =
            DriverManager.getConnection(url, USER, PASSWORD);
         
         return connection;
       }
      catch (Exception e) { System.err.println(e); return null;}
    }

   /*-----------------------------------------------------------------------*/

   //Check if the query already exists in the database
   public static boolean query_exists(Connection connection, String query) {
                try {
         String mysql_query = String.format("select * from Result where Query = '" + query + "'");

         PreparedStatement statement = connection.prepareStatement(mysql_query);
         ResultSet set = statement.executeQuery();
         if (set.next()) {
             return true;
         }
        }
         catch (Exception e) { System.err.println(e);}
        return false;
   }
    /*-----------------------------------------------------------------------*/
    public static void main(String[] args) throws Exception{
        
        String query = args[0];
        for (int i = 1; i < args.length; i++) {
            query += " " + args[i];
        }
        Connection connection = get_connection();

        if (query_exists(connection, query)) return;
        
        PreparedStatement statement = connection.prepareStatement("Select * from Domain");
        ResultSet set = statement.executeQuery();
        //Replace spaces with %20 for URL
        query = query.replace(" ", "%20");
        String domain = "";

        while (set.next()) {
            domain = set.getString("suffix");
            int DomainID = set.getInt("DomainID");
            System.out.println(domain);
            String result = get_results(query, domain);
            parse_result(result, query, domain, connection, DomainID);
        }

        connection.close();
    }

    /*-----------------------------------------------------------------------*/
    //Put results into databse
    public static void add_results(String title, String link, String snippet, String query, int LanguageID, int DomainID, Connection connection) {
         try {
         String space_query = query.replace("%20", " ");
         String body = read_url(link);
         String mysql_query = String.format("INSERT INTO Result (Title, Link, Snippet, Query, LanguageID, DomainID, Body) VALUES (?, ?, ?, ?, ?, ?, ?)");

         //, title, link, snippet, query, LanguageID, DomainID, body);
         PreparedStatement statement = connection.prepareStatement(mysql_query);
         statement.setString(1, title);
         statement.setString(2, link);
         statement.setString(3, snippet);
         statement.setString(4, space_query);
         statement.setInt(5, LanguageID);
         statement.setInt(6, DomainID);
         statement.setString(7, body);
         
         statement.execute();
        }
         catch (Exception e) { System.err.println(e);}
   }
    /*-----------------------------------------------------------------------*/
    //Parse JSON to get search data
    public static void parse_result(String result, String query, String domain, Connection connection, int DomainID) {
        if (result != null) {
            Object obj = JSONValue.parse(result);
            JSONObject json_obj = (JSONObject)obj;
            JSONArray items = (JSONArray)json_obj.get("items");
            for (Object current : items) {
                JSONObject current_json = (JSONObject)current;
                Object title = current_json.get("title");
                Object link = current_json.get("link");
                Object snippet = current_json.get("snippet");
                String title_string = title.toString();
                String link_string = link.toString();
                String snippet_string = snippet.toString();

                add_results(title_string, link_string, snippet_string, query, 0, DomainID, connection);
            }
        }
    }

    /*-----------------------------------------------------------------------*/
    //Get query results from the Google API
    public static String get_results(String query_term, String domain_code) {
        String result = null;
        String query = query_term + "%20site:*" + domain_code;
        String url_string = "https://www.googleapis.com/customsearch/v1?key=AIzaSyBY1Pd9ttYbH3_IeXNwiO5Q9C7UQOwt2Vc&cx=011470580531091259047:zkviq8z5sb0&q=" + query;
        System.out.println(url_string);
        try {
            URL url = new URL(url_string);
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            result = builder.toString();
        }
        catch (Exception e)
        {
        }
       return result;
    }
    /*-----------------------------------------------------------------------*/
    //Remove HTML tags from body
    public static String remove_html(String body) {
        return Jsoup.parse(body).text();
    }
    /*-----------------------------------------------------------------------*/
    //Reads URL to obtain body, parts adapted from COS333 sample code
    public static String read_url(String input_url) throws Exception {
        URL current_url = new URL(input_url);
        URLConnection conn = current_url.openConnection();
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        InputStream stream = current_url.openStream();
        InputStreamReader reader = new InputStreamReader(stream);
	BufferedReader in = new BufferedReader(
				new InputStreamReader(
				current_url.openStream()));
	String inputLine;
        String body = "";

	while ((inputLine = in.readLine()) != null) {
            body = body + inputLine + "\n";
        }


	in.close();
        body = remove_html(body);
        return body;
    }
 }