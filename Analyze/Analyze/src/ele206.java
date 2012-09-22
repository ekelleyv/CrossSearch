/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author JT Glaze
 */
public class ele206 {



    public static void main(String[] args) {

        byte a[] = new byte[256];
        byte b[] = new byte[256];
        byte cy = (byte) 0;

        int i = 0;
        int sumx = 0;
        int sumy = 0;
        byte c[] = new byte[256];

        a = randomArray(a);
        b = randomArray(b);

        while (i < 256) {
            if (a[i] < 0) {
                c[i] = (byte) (a[i] * b[i]);
                sumx += c[i];
            }
            else {
                c[i] = (byte) (a[i] * (b[i] + cy));
                sumy += c[i];
            }
            i++;
        }

        System.out.println("a: ");
        for (byte d : a)
            System.out.print(d + ", ");
        System.out.println("\nb: ");
                for (byte d : b)
            System.out.print(d + ", ");
        System.out.println("\nc: ");
        for (byte d : c)
            System.out.print(d + ", ");
        System.out.println("\nsumx: " + sumx);
        System.out.println("sumy: " + sumy);
    }

    public static byte[] randomArray(byte[] x) {
        for (int i = 0; i < x.length; i++) {
            x[i] = (byte) (256*(Math.random() - .5));
     //       System.out.print(y + ", ");
        }

        return x;
    }
}
