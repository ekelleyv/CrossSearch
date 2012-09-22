
package WordCount;

import java.util.*;
import java.sql.*;
import java.io.*;
//import org.jsoup.Jsoup;
//import org.json.simple.*;
import java.net.*;

/**
 *
 * @author JT Glaze & Ed Kelley
 */
public class WordCount {

    private static final int DOMAIN_NUMS = 5;
    private static final int stopWordCount = 119;
    private static final int initHashSize = 50;
    private static final int reportSize = 10;
    private static Hashtable stopWords = new Hashtable(stopWordCount);
    private static final String stopWordDelimiter = ",";
//    private static final File stopWordsFile = new File("./StopWords.txt");
//    private static final File stopWordsFile = new File("C:\\Users\\JT Glaze\\Dropbox\\cos435\\Analyze\\Analyze\\src\\WordCount\\StopWords.txt");
//    private static Hashtable wordCount = new Hashtable(initHashSize);
//    private static final File countFiles = new File("C:\\Users\\JT Glaze\\Dropbox\\cos435\\Analyze\\Analyze\\src\\WordCount\\Training Files");
    private static final String wordDelimiter = "[ !\"#$%&'()*+,-./:;<=>?@\\^_`{|}~]+";
    private static final String HOST = "localhost";
    private static final String PORT = "5555";
    private static final String DATABASE = "jglaze";
    private static final String USER = "jglaze";
    private static final String PASSWORD = "jglaze";


    private static class kvPair implements Comparable<kvPair> {
        private final String key;
        private final double value;

        kvPair(String k, double v) {
            this.key = k;
            this.value = v;
        }

        public int compareTo(kvPair that) {
            if (this.value > that.value)
                return -1;
            else if (this.value == that.value)
                return 0;
            else
                return 1;
        }

        public String toString() {
            return key + ":" + value;
        }
    }

    //Add ability to pass in query terms
    public static void main(String[] args) throws Exception {

     //check_db();
     String query = args[0];
     String title = "";
     String link = "";
     String snippet = "";
     String[] trainingFiles = new String[DOMAIN_NUMS];
     String domain = "";

     Connection connection = get_connection();

     PreparedStatement DomainStatement = connection.prepareStatement("Select * from Domain");
     ResultSet DomainSet = DomainStatement.executeQuery();

     int j = 0;
     while (DomainSet.next()) {
     domain = DomainSet.getString("suffix");
     int DomainID = DomainSet.getInt("DomainID");
     System.out.println(domain);

     PreparedStatement statement = connection.prepareStatement("Select * from Result where query = '" + query + "' AND DomainID = '" + DomainID + "' ORDER BY DomainID");
     ResultSet set = statement.executeQuery();
        while (set.next()) {
            trainingFiles[j] += set.getString("Body");
         }
     j++;
 }


     connection.close();

     BuildStopWordsTable();

  //      String[] trainingFiles = countFiles.list();

        Hashtable baseWords = new Hashtable(initHashSize);
        int totBaseWords = 0;
        for (int i = 0; i < trainingFiles.length; i++)
             totBaseWords = BuildCounts(trainingFiles[i], baseWords, totBaseWords);
        CalcPercents(baseWords, totBaseWords);
 //       System.out.println(totBaseWords + "\n" + baseWords);

        for (int i = 0; i < trainingFiles.length; i++) {
            Hashtable wordCount = new Hashtable(initHashSize);
            int totalWords = BuildCounts(trainingFiles[i], wordCount, 0);
            CalcPercents(wordCount, baseWords, totalWords);
   //         System.out.println(totalWords + "\n" + wordCount);
            PriorityQueue<kvPair> countPQ = BuildPQ(wordCount);

//            System.out.println(trainingFiles[i] + countPQ.size());

            for (int k = 0, size = countPQ.size(); k < reportSize && k < size; k++) {
                System.out.println(countPQ.poll());
            }
/*            int j = 0;
            for (kvPair KV : countPQ) {
                System.out.println(KV);
                if (j++ >= reportSize)
                    break;
            }*/
            System.out.println();

        }


    }

    private static int BuildCounts(String trainingString, Hashtable wordCount, int totalWords) throws FileNotFoundException {

        Scanner reader = new Scanner(trainingString).useDelimiter(wordDelimiter);

        while (reader.hasNext()) {
            String word = reader.next();
            word = word.toLowerCase();
            totalWords++;

            if (! stopWords.containsKey(word)) {
                if (wordCount.containsKey(word)) {
                    Integer count = (Integer) wordCount.get(word);
                    wordCount.put(word, ++count);
                }
                else
                    wordCount.put(word, new Integer(1));
                }
        }

        return totalWords;
    }

    private static PriorityQueue<kvPair> BuildPQ(Hashtable wordCount) {
        PriorityQueue<kvPair> countPQ = new PriorityQueue(wordCount.size());

        for (Enumeration keys = wordCount.keys(); keys.hasMoreElements();) {
            String key = (String) keys.nextElement();
            kvPair KV = new kvPair(key, (Double) wordCount.get(key));
            countPQ.add(KV);
        }
        return countPQ;
    }

    private static void CalcPercents(Hashtable wordCount, int totalWords) {
        for (Enumeration keys = wordCount.keys(); keys.hasMoreElements();) {
            String key = (String) keys.nextElement();
            double count = ((Integer) wordCount.get(key)).doubleValue();
            wordCount.put(key, (count / (double) totalWords));
        }
    }

    private static void CalcPercents(Hashtable wordCount, Hashtable baseWords, int totalWords) {
        for (Enumeration keys = wordCount.keys(); keys.hasMoreElements();) {
            String key = (String) keys.nextElement();
            double percent = ((Integer) wordCount.get(key)).doubleValue();
            double basePercent = ((Double) baseWords.get(key));
            percent = ((percent / (double) totalWords) / (basePercent));
            wordCount.put(key, percent);
        }
    }

    private static void BuildStopWordsTable() throws Exception {

        Connection connection = get_connection();

        PreparedStatement StopWordsStatement = connection.prepareStatement("Select * from StopWords");
        ResultSet StopWordsSet = StopWordsStatement.executeQuery();

        String stopWordsFile = "";
        while (StopWordsSet.next()) {
            stopWordsFile += StopWordsSet.getString("StopWords");
        }

        Scanner reader = new Scanner(stopWordsFile).useDelimiter(stopWordDelimiter);

        while (reader.hasNext()) {
            stopWords.put(reader.next(), "");
        }

        connection.close();
    }

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

}
