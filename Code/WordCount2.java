
package WordCount;

import java.util.*;
import java.sql.*;
import java.io.*;
import java.net.*;

/**
 * COS 435 FINAL PROJECT - CrossSearch
 * WordCount2.java
 * @author JT Glaze & Ed Kelley
 */
public class WordCount2 {

    //Private Variables
    private static final int DOMAIN_NUMS = 5;
    private static final int LINK_NUM = 5;
    private static final int SAMPLE_NUM = 10;
    private static final int stopWordCount = 119;
    private static final int initHashSize = 50;
    private static final int reportSize = 300;

    private static Hashtable stopWords = new Hashtable(stopWordCount);
    private static final String stopWordDelimiter = ",";
    private static final String wordDelimiter = "[ !\"#$%&'()*+,-./:;<=>?@\\^_`{|}~]+";

    //Database Access
    private static final String HOST = "localhost";
    private static final String PORT = "5555";
    private static final String DATABASE = "jglaze";
    private static final String USER = "jglaze";
    private static final String PASSWORD = "jglaze";

    //Key Value Pair Object
    private static class kvPair implements Comparable<kvPair> {
        private final String key; //Word
        private final double value; //Weight

        kvPair(String k, double v) {
            this.key = k;
            this.value = v;
        }

        //Sort by decreasing weight
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

    public static void main(String[] args) throws Exception {

     //Build query
     String query = args[0];
     for (int i = 1; i < args.length; i++) {
         query += " " + args[i];
        }

     String[][] title = new String[DOMAIN_NUMS][LINK_NUM];
     String[][] link = new String[DOMAIN_NUMS][LINK_NUM];
     String[][] snippet = new String[DOMAIN_NUMS][LINK_NUM];
     String[] trainingFiles = new String[DOMAIN_NUMS];
     String[] domainNames = new String[DOMAIN_NUMS];

     //Connect to database
     Connection connection = get_connection();
     PreparedStatement DomainStatement = connection.prepareStatement("Select * from Domain");
     ResultSet DomainSet = DomainStatement.executeQuery();

     int j = 0;
     while (DomainSet.next()) {
        //Get Domain data
        domainNames[j] = DomainSet.getString("Country") + ": " + DomainSet.getString("suffix");
        int DomainID = DomainSet.getInt("DomainID");

        //Get Results data
        PreparedStatement statement = connection.prepareStatement("Select * from Result where query = '" + query + "' AND DomainID = '" + DomainID + "' ORDER BY DomainID");
        ResultSet set = statement.executeQuery();
           for (int i = 0; i < LINK_NUM && set.next(); i++) {
               title[j][i] = set.getString("Title");
               link[j][i] = set.getString("Link");
               snippet[j][i] = set.getString("Snippet");
               trainingFiles[j] += set.getString("Body");
            }
         j++;
     }
     connection.close(); 

     //Build stopWords
     BuildStopWordsTable();

     kvPair[][] results = new kvPair[DOMAIN_NUMS][reportSize];

        for (int i = 0; i < trainingFiles.length; i++) {

            //Count words in (S-p)
            Hashtable baseWords = new Hashtable(initHashSize);
            int totBaseWords = 0;
            for (int k = 0; k < trainingFiles.length; k++) {
                if (k != i)
                    totBaseWords = BuildCounts(trainingFiles[k], baseWords, totBaseWords);
            }
            //Calculate significance of words in (S-p)
            CalcPercents(baseWords, totBaseWords);

            //Count words in p
            Hashtable wordCount = new Hashtable(initHashSize);
            int totalWords = BuildCounts(trainingFiles[i], wordCount, baseWords);
            //Calculate weight of words in p
            CalcPercents(wordCount, baseWords, totalWords);
            //Sort words by decreasing weight
            PriorityQueue<kvPair> countPQ = BuildPQ(wordCount);
            //Return top 300 words
            for (int k = 0, size = countPQ.size(); k < reportSize && k < size; k++) {
                results[i][k] = countPQ.poll();
            }
            //Display results
            printDomainInfo(domainNames[i], results[i], title[i], link[i], snippet[i]);
        }
        seeFullResults(domainNames, results);
    }

    //Equation from http://www.vias.org/tmdatanaleng/cc_scaling.html
    private static double[] scaleRelSize(kvPair top, kvPair bottom) {
        double hi = top.value;
        double lo = bottom.value;
        double botRange = 1;
        double topRange = 5;
        double a[] = new double[2];
        
        a[0] = (((botRange * hi) - (topRange * lo)) / (hi - lo));
        a[1] = ((topRange - botRange) / (hi - lo));
        
        return a;
    }


    private static void printDomainInfo(String domainName, kvPair[] results, String[] title, String[] link, String[] snippet) {
        //Print domain country and name
        System.out.println("<span class=\"country\">" + domainName + "</span><br/>");
        //Print result title (hyperlinked) and snippet
        for (int i = 0; i < title.length; i++) {
            System.out.println("<a href=\"" + link[i] + "\"><span class=\"link\">" + title[i] + "</span></a><br/>");
            System.out.println("<span class=\"snippet\">" + snippet[i] + "</snippet><br/>");
        }
        //Print top 10 words sized by weight
        System.out.println("<br/><b><font color=\"AA2808\">TOP WORDS</font></b><br/>");
        double a[] = scaleRelSize(results[0], results[SAMPLE_NUM]);
        for (int i = 0; i < SAMPLE_NUM; i++) {
            System.out.println("<font size=\"" + ((int) ((results[i].value * a[1]) + a[0])) + "\">" + results[i].key + " </font>");
            if (i == 4)
                System.out.println("<br/>");
        }
        System.out.println("<br/>");
        //Print WordleTM button
        wordleLink(results);
        System.out.println("<br/><br/>");
     }

    //Creates a results .html file which contains the top 300 words for each domain
    private static void seeFullResults(String[] domainNames, kvPair[][] results) throws IOException {
        File resultsFile = new File("Results.html");
        String text = "<html lang=\"en\"><body>";
        text += "<font size=\"8\"><b>Full Results</b></font><br/>";

        for (int i = 0; i < domainNames.length; i++) {
            text += "<p><font size=\"7\"><b>"+ domainNames[i] + "</b></font><br/>";

            for (int j = 0; j < results[i].length; j++) {
                text += results[i][j].key + " : " + results[i][j].value + "<br/>";
            }
            text += "</p>";
        }
        text += "</body></html>";

        //This code adapted from http://www.javapractices.com/topic/TopicAction.do?Id=42
        Writer output = new BufferedWriter(new FileWriter(resultsFile));
        try {
            output.write(text);
        }
        finally {
            output.close();
        }

        //Add link to this file to the results page
        System.out.println("<a href=\"" + resultsFile.getPath() + "\">See Full Results</a><br/>");

    }

    //Prints button which posts results to Wordle.net
    private static void wordleLink(kvPair[] results) {
        System.out.println("<form action=\"http://www.wordle.net/advanced\" method=\"POST\"  target=\"_blank\"> "
                + "<textarea name=\"wordcounts\" style=\"display:none\">");
        for (int i = 0; i < results.length; i++) {
            System.out.println(results[i].toString() + "<wordle>");
        }
        System.out.println("</textarea><br/><input type=\"submit\" class=\"submit\" value=\"Wordle Results\"/></form>");
    }

    //Build word counts for (S-p)
    private static int BuildCounts(String trainingString, Hashtable wordCount, int totalWords) throws FileNotFoundException {

        //Seperate words by spaces or non-alphanumeric characters
        Scanner reader = new Scanner(trainingString).useDelimiter(wordDelimiter);

        while (reader.hasNext()) {
            String word = reader.next();
            word = word.toLowerCase();
            totalWords++;

            //Eliminate stop words, numerals, and two-character words
            if ((!stopWords.containsKey(word)) && word.matches(".*[a-zA-Z].*")&&(word.length()>2)) {
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

    //Build word counts for p
    private static int BuildCounts(String trainingString, Hashtable wordCount, Hashtable baseWords) throws FileNotFoundException {

        int totalWords = 0;
        //Seperate words by spaces or non-alphanumeric characters
        Scanner reader = new Scanner(trainingString).useDelimiter(wordDelimiter);

        while (reader.hasNext()) {
            String word = reader.next();
            word = word.toLowerCase();
            totalWords++;

            //Eliminate stop words, numerals, and two-character words
            if ((!stopWords.containsKey(word))&&(baseWords.containsKey(word))&&(word.matches(".*[a-zA-Z].*"))&&(word.length()>2)) {
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

    //Create kvPairs from words and weights, sort them in a PriorityQueue
    private static PriorityQueue<kvPair> BuildPQ(Hashtable wordCount) {
        PriorityQueue<kvPair> countPQ = new PriorityQueue(wordCount.size());

        for (Enumeration keys = wordCount.keys(); keys.hasMoreElements();) {
            String key = (String) keys.nextElement();
            kvPair KV = new kvPair(key, (Double) wordCount.get(key));
            countPQ.add(KV);
        }
        return countPQ;
    }

    //Calculate relative significance of words in (S-p)
    private static void CalcPercents(Hashtable wordCount, int totalWords) {
        for (Enumeration keys = wordCount.keys(); keys.hasMoreElements();) {
            String key = (String) keys.nextElement();
            double count = ((Integer) wordCount.get(key)).doubleValue();
            wordCount.put(key, (count / (double) totalWords));
        }
    }

    //Calculate weights of words in p
    private static void CalcPercents(Hashtable wordCount, Hashtable baseWords, int totalWords) {
        for (Enumeration keys = wordCount.keys(); keys.hasMoreElements();) {
            String key = (String) keys.nextElement();
            double percent = ((Integer) wordCount.get(key)).doubleValue();
            double basePercent = ((Double) baseWords.get(key));
            percent = ((percent / (double) totalWords) / (basePercent));
            wordCount.put(key, percent);
        }
    }

    //Stop Words downloaded from http://www.textfixer.com/resources/common-english-words.txt
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

    //Adapted from COS 333 code - http://www.cs.princeton.edu/courses/archive/spring11/cos333/lectures/04db/UpdateRows.java
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
}