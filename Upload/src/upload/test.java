/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upload;

import java.io.*;
import java.net.ServerSocket;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fred
 */
public class test {
    
    public static void main(String[] args) throws NoSuchAlgorithmException
    {
        new test(9999);
    }
    
    Console con = System.console();
    public int port;
    public String fileName;
    public String fileHash;
    
    public test(int port) throws NoSuchAlgorithmException
    {
        this.port = port;
        String tmp;
        Scanner scan;
        uploadMasterThread t = null;
        while(true) {
            try {
                query("");
                scan = new Scanner(con.readLine());
                tmp = scan.next();
                switch (tmp.toLowerCase().trim()) {
                    case "set":
                        String set1 = "filename";
                        if(!scan.hasNext())
                        {
                            System.out.println("Set: ");
                            System.out.println("\t " + set1);
                            query("Set what?");
                            scan = new Scanner(con.readLine());
                        }
                        tmp = scan.next();
                        if(tmp.toLowerCase().trim().equals(set1)) 
                        {
                            if(!scan.hasNext()) {
                                query("What file? ");
                                scan = new Scanner(con.readLine());
                            }
                            tmp = scan.next();
                            if( validFile(tmp)) {
                                System.out.println("Hashing...");
                                String tmp2 = calculateHash(MessageDigest.getInstance("MD5"), tmp);  
                                fileHash = tmp2;
                                fileName = tmp;
                                System.out.println("Focusing " + tmp);
                            } else throw new FileNotFoundException();
                        }
                        break;
                    case "dir":
                        File file = new File(".");
                        for(File f : file.listFiles())
                        {
                            System.out.println(f.getPath());
                        }
                        break;
                    case "start":
                    case "upload":
                        if(t == null) {
                            if(fileName == null && !scan.hasNext()) {
                                query("What file? ");
                                scan = new Scanner(con.readLine());
                            }
                            if(fileName == null) { 
                                tmp = scan.next();
                                if(validFile(tmp)) { 
                                    System.out.println("Hashing...");
                                    String tmp2 = calculateHash(MessageDigest.getInstance("MD5"), tmp);  
                                    fileHash = tmp2;
                                    fileName = tmp;
                                    System.out.println("Focusing " + tmp);
                                } else throw new FileNotFoundException();
                            }
                            t = new uploadMasterThread(port);
                            t.start();
                            System.out.println("Listening on port " + port + " started.");
                        }
                        else System.out.println("Thread already started!");
                        break;
                    case "q":
                    case "quit":
                        System.out.println("Quits.");
                        System.exit(0);
                        break;
                    case "?":
                        System.out.println("Listening on port " + port);
                        System.out.println("Focusing file " + fileName);
                        break;
                }
            } catch (NumberFormatException ex) {
                System.err.println("Invalid input. Expected a number.");
            } catch (FileNotFoundException ex) {
                System.err.println("Invalid input. File is not found.");
            } catch (IOException ex) {
                Logger.getLogger(test.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
    }
    
    private boolean validFile(String s)
    {
        return new File(s).exists();
    }
    
    private void query(String s)
    {
        System.out.println(s);
        System.out.print(">> ");
    }
    
    /**
     * Method to calculate MD5 checksum of a file
     * 
     * Cudos to: http://www.javablogging.com/sha1-and-md5-checksums-in-java/
     * 
     * @param algorithm
     * @param fileName
     * @return
     * @throws Exception 
     */
    private String calculateHash(MessageDigest algorithm,
            String fileName) throws FileNotFoundException, IOException {
        BufferedInputStream bis = null;
        try {
            FileInputStream     fis = new FileInputStream(fileName);
            bis = new BufferedInputStream(fis);
            DigestInputStream dis = new DigestInputStream(bis, algorithm);
            // read the file and update the hash calculation
            while (dis.read() != -1);
            // get the hash value as byte array
            byte[] hash = algorithm.digest();
            return byteArray2Hex(hash);
        } finally {
            bis.close();
        }
    }

    private static String byteArray2Hex(byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    
    
    public class uploadMasterThread extends Thread
    {
        ServerSocket servsock = null;
        
        public uploadMasterThread(int port) throws IOException
        {
            servsock = new ServerSocket(port);
        }
        
        @Override
        public void run() 
        {
            try {
                while(true)
                {
                    Thread t = new Thread(new Upload(servsock.accept(), fileName, fileHash));
                    t.start();
                }
            } catch (Exception ex) {
                System.out.println("Something did go wrong :/");
                System.out.println(ex.getMessage());
                return;
            } finally {
                try {
                    servsock.close();
                } catch (IOException ex) {
                    Logger.getLogger(test.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
    }   
}
