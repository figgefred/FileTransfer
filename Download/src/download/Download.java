/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package download;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Part of the Download module.
 * 
 * Download class implements Runnable and can be instanced in a thread.
 * 
 *  - It can be wrapped and used in a multithreaded environment.
 *  - It can be used to download a file once as a single instance (main method).
 * 
 *  - An instance of Upload will always do the following steps:
 *  1.  initDownload() -> Initiate client by receiving vital setup values from server
 *      (this includes a MD5 checksum value)
 *  2.  Check that no file is overwritten or downloaded if it already exists
 *      If there is a conflict a renaming process of the filename will start or
 *      download will be aborted (as file already exists, step 3-5 skipped)
 *  3.  ping() -> Tell server that client is ready to receive file
 *  4.  doDownload() -> Download the file
 *  5.  Check that downloaded file is valid, that is not corrupt or incomplete
 *      by comparing MD5 checksums.
 * 
 * @author Frederick Ceder
 * @author fred
 */
public class Download implements IDownload {

    private static boolean consoleInstance = false;
    
    private int port;
    private String host;
    
    InputStream inStream;
    
    final String EOL = "eol";
    Socket downloadSocket = null;
    long fileSize;
    String fileName;
    int packageSize;
    String externalFileHash = null;
    String localFileHash = null;
    
    long downloadedBytes = 0;
    OutputStream out = null;
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try{
            if(args.length == 2) {
                (new Thread(new Download(args[0], Integer.parseInt(args[1]), true))).start();
            }
            else if(consoleInstance) System.out.println("Invalid parameter.\nEXAMPLE: java -jar Download.jar host-ip port");
            else (new Thread(new Download("localhost", 9999, true))).start();
        } catch(NumberFormatException e)
        {
            System.err.println("Invalid port.");
            System.exit(0);
        }
    }  

    /**
     * Initiates a Download instance.
     * 
     * @param host
     * @param port 
     */
    public Download(String host, int port)
    {
        this.host = host;
        this.port = port;
    }
    
    /**
     * Initiates a Download instance.
     * 
     * @param host
     * @param port 
     */
    public Download(String host, int port, boolean consoleInstance)
    {
        this.host = host;
        this.port = port;
        Download.consoleInstance = consoleInstance;
    }
    
    
    /**
     * Initiates a Download instance.
     * 
     * @param host
     * @param port 
     */
    public Download(Socket socket)
    {
        this.host = socket.getInetAddress().getHostAddress();
        this.port = socket.getPort();
        downloadSocket = socket;
    }
    
    
    

    @Override
    public void run() {
        try {
            if(consoleInstance) System.out.println("Initializing download...");
            // Intialize vital fields, such as downloadsocket
            initDownload(host, port);
            if(consoleInstance) System.out.println("Initialization complete!\n");
            
            // Initialize the outputstream
            out = downloadSocket.getOutputStream();
            
            // Check if file of respective name already exists
            if((new File(fileName).exists())) { 
                if(consoleInstance) System.out.println("\nWARNING! Conflicts found.\n");
                if(consoleInstance) System.out.println("Inspecting content...");
                // Calculate a hashvalue (MD5) of existing file
                localFileHash = calculateHash(MessageDigest.getInstance("MD5"), fileName);
            }
            
            // if localFileHash is not null, then we have had a name conflict
            if(localFileHash != null)
            {
                if(localFileHash.equals(externalFileHash))
                {
                    fileName = null;
                }
                else
                {
                    if(consoleInstance) System.out.println("\tDifferent files. Renaming...");
                    String pattern = ".+\\.[\\w]+";
                    Pattern pat = Pattern.compile(pattern);
                    int counter = 1;
                    renameFile(pat, counter);
                    if(consoleInstance) System.out.println("\tRenaming done!");
                    if(consoleInstance) System.out.println("Inspection complete!\n");
                }
            }
            
            if(fileName != null) 
            {
                if(consoleInstance) System.out.print("Asking server for file...");
                ping(out);            
                System.out.print(" Done!\n");
                if(consoleInstance) System.out.println("Downloading started...");
                doDownload();
                if(consoleInstance) System.out.println("Downloading completed!\n");
                if(consoleInstance) System.out.println("Checking file validity...");
                localFileHash = calculateHash(MessageDigest.getInstance("MD5"), fileName);
                if(localFileHash.equals(externalFileHash)) { 
                    if(consoleInstance) System.out.println("File is valid!\n\nSession completed!");
                }
                else if(consoleInstance)
                    System.err.println("ERROR: Download didn't finish as expected, file might be corrupt or incomplete.");
            }
            else { 
                if(consoleInstance) System.out.println("File already exists!");
                if(consoleInstance) System.out.println("\nDownload aborted.");
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Download.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Download.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            Logger.getLogger(Download.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | NoSuchAlgorithmException ex) {
            Logger.getLogger(Download.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                downloadSocket.close();
            } catch (IOException ex) {
                Logger.getLogger(Download.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
    
    /**
     * Downloads a file. 
     * 
     * File are read from packages of the size packagesize and written to a 
     * file individually.
     * 
     * NOTE 
     *  -   client must have runned initClient() to initialize vital 
     *      values/fields 
     *  -   client must, if the server requires so, ping the server that client
     *      is ready to receive data
     *  -   client can after these 1 or 2 steps start the download
     * 
     * @throws FileNotFoundException
     * @throws IOException
     * @throws NoSuchAlgorithmException 
     */  
    public void doDownload() throws IOException, InterruptedException
    {
        byte[] byteArray = new byte[packageSize];
        //InputStream is = downloadSocket.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(inStream);
        FileOutputStream fos = new FileOutputStream(fileName);
        BufferedOutputStream bfos = new BufferedOutputStream(fos);
        
        int packages = (int) (fileSize/packageSize) +1;
        int print = 10;
        if(consoleInstance) System.out.println("\tReceiving " + packages + " packages");
        int bytesRead;
        while(true)
        {
            if(Thread.interrupted()) throw new InterruptedException();
            bytesRead = bis.read(byteArray);
            downloadedBytes += bytesRead;
            bfos.write(byteArray, 0, bytesRead);
            bfos.flush();
            if(consoleInstance && getProgress() == print)
            {
                switch(print) {
                    case 100: System.out.println("100%\n"); break;
                    case 10: System.out.print("\n\t" + print + "%..."); print+=10; break;
                    default: System.out.print(print + "%..."); print+=10; break;
                }
            }
            if(downloadedBytes >= fileSize) break;
        }
        if(consoleInstance) System.out.println("\tReceived " + downloadedBytes + " bytes");
        bfos.close();
    }

    /**
     * Initializes the Client. Receives filesize, filename and packagesize values
     * from server.
     * 
     * Also receives a MD5 checksum.
     * 
     * @throws IOException 
     */
    private void initDownload(String host, int port) throws UnknownHostException, IOException, InterruptedException
    {
            if(downloadSocket == null) downloadSocket = new Socket(host, port);
            if(consoleInstance) System.out.println("\tConnected to " + downloadSocket.getInetAddress().getHostAddress() + ":" + downloadSocket.getPort());
            
            inStream = downloadSocket.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
            String tmp = in.readLine();
            while(tmp != null && !(tmp.toLowerCase().trim().equals(EOL)))
            {
                if(Thread.interrupted()) throw new InterruptedException();
                Scanner scan = new Scanner(tmp);
                tmp = (scan.next()).trim().toLowerCase();
            switch (tmp) {
                case "name":
                    fileName = scan.next();
                    if(consoleInstance) System.out.println("\tIdentified file as: " + fileName);
                    break;
                case "size":
                    fileSize = Long.parseLong(scan.next());
                    if(consoleInstance) System.out.println("\tIdentified filesize as: " + fileSize + " bytes");
                    break;
                case "packagesize":
                    packageSize = Integer.parseInt(scan.next());
                    if(consoleInstance) System.out.println("\tWill be expecting packages of size: " + packageSize + " bytes");
                    break;
                case "md5hash":
                    externalFileHash = scan.next();
                    break;
            }
                tmp = in.readLine();
            }
    }
    
    /**
     * Pings the server by sending a byte.
     * 
     * This can be considered as a readyping.
     * 
     * @param out
     * @throws InterruptedException
     * @throws IOException 
     */
    private void ping(OutputStream out) throws IOException 
    {
        out.write(1); out.flush();
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
            String fileName) throws FileNotFoundException, IOException, InterruptedException {
        BufferedInputStream bis = null;
        try {
            FileInputStream     fis = new FileInputStream(fileName);
            bis = new BufferedInputStream(fis);
            DigestInputStream dis = new DigestInputStream(bis, algorithm);
            // read the file and update the hash calculation
            while (dis.read() != -1) {
                if(Thread.interrupted()) throw new InterruptedException();
            }
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

    /**
     * Renames a file by adding a number to its name.
     * 
     * Example: file becomes file1
     * Example: file.exe becomes file1.exe
     * 
     * @param pattern
     * @param counter 
     */
    private void renameFile(Pattern pattern, int counter) throws InterruptedException {

        StringBuilder tmpName = null;
        if(pattern.matcher(fileName).matches())
        {
            Scanner scan = new Scanner(fileName);
            scan.useDelimiter("\\.");
            StringBuilder sb = new StringBuilder();
            String tmp = scan.next();
            boolean done = false;
            while(!done)
            {
                if(Thread.interrupted()) throw new InterruptedException();
                sb.append(tmp);
                tmp = scan.next();
                if(scan.hasNext())
                {
                    done = false;
                    sb.append(".");
                }
                else done = true;
            }
            tmpName = new StringBuilder().append(sb.toString());
            tmpName.append(counter).append(".").append(tmp);
        }
        else {
            tmpName = new StringBuilder().append(fileName).append(counter);
        }
        if((new File(tmpName.toString()).exists())) { 
            counter++;
            renameFile(pattern, counter);
        }
        else fileName = tmpName.toString();
    }
    
   /**
    * Returns a percentage value of the downloadprogress made.
    * @return progressPercentage
    */
    @Override
    public int getProgress()
    {
        return (int) (((float) downloadedBytes / (float) fileSize) * 100);
    }
    
    /**
     * Returns the name of the file were the read in data is written to.
     * @return 
     */
    @Override
    public String getFileName()
    {
        return fileName;
    }
    
    /**
     * Returns size of the source file in bytes.
     * @return 
     */
    @Override
    public long getFileSize()
    {
        return fileSize;
    }
    
    /**
     * Returns the bytes downloaded at that instance.
     * Note that this value changes very quickly.
     * @return 
     */
    @Override
    public long getBytesDownloaded()
    {
        return downloadedBytes;
    }
    
}
