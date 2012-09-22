/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package search;

import java.sql.*;
import java.io.*;
import org.jsoup.Jsoup;
import org.json.simple.*;
import java.net.*;

/**
 *
 * @author Ed
 */
public class Main {

   private static final String HOST = "publicdb.cs.princeton.edu";
   private static final String PORT = "3306";
   private static final String DATABASE = "jglaze";
   private static final String USER = "jglaze";
   private static final String PASSWORD = "jglaze";

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
    public static void main(String[] args) throws Exception{
        //check_db();
        String query = "tsunami";
        //uery.replace(" ", "%%20");
        String domain = "";

        Connection connection = get_connection();

        PreparedStatement statement = connection.prepareStatement("Select * from Domain");
        ResultSet set = statement.executeQuery();

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
    public static void add_results(String title, String link, String snippet, String query, int LanguageID, int DomainID, Connection connection) {
         try {
         String body = read_url(link);
         String mysql_query = String.format("INSERT INTO Result (Title, Link, Snippet, Query, LanguageID, DomainID, Body) VALUES (?, ?, ?, ?, ?, ?, ?)");

         //, title, link, snippet, query, LanguageID, DomainID, body);
         PreparedStatement statement = connection.prepareStatement(mysql_query);
         statement.setString(1, title);
         statement.setString(2, link);
         statement.setString(3, snippet);
         statement.setString(4, query);
         statement.setInt(5, LanguageID);
         statement.setInt(6, DomainID);
         statement.setString(7, body);
         
         statement.execute();
        }
         catch (Exception e) { System.err.println(e);}
   }
    /*-----------------------------------------------------------------------*/
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
    public static String get_results(String query_term, String domain_code) {
        String result = null;
        String query = query_term + "site:" + domain_code;
        String url_string = "https://www.googleapis.com/customsearch/v1?key=AIzaSyBY1Pd9ttYbH3_IeXNwiO5Q9C7UQOwt2Vc&cx=011470580531091259047:zkviq8z5sb0&q=" + query;
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
    public static String remove_html(String body) {
        return Jsoup.parse(body).text();
    }
    /*-----------------------------------------------------------------------*/
    public static String read_url(String input_url) throws Exception {
        URL current_url = new URL(input_url);
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