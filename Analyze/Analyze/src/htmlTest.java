/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author JT Glaze
 */
public class htmlTest {
    public static void main(String[] args) {
        String output = "";
        for (int i = 0; i < args.length; i++)
            output += args[i] + " ";

    //    System.out.println("test: " + output);

   /*     System.out.println(//"<h2> \n"
                "<body> \n"
                + "test: " + output
                + "\n</body> \n"
                + "</h2>");*/

//        System.out.println(output + "\n testtt");

        System.out.println("<a href=\"http://www.google.com\">" + output + "</a> "+ "<br/>testttt");
    }
}
