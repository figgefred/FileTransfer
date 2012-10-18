/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upload;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part of the upload module.
 * 
 * Upload class implements Runnable and can be instanced in a thread.
 * 
 *  - It can be wrapped and used in a multithreaded environment.
 *  - It can be used to upload a file once as a single instance (main method).
 * 
 *  - An instance of Upload will always do the following steps:
 *  1.  initClient() -> Initiate client by sending vital setup values
 *      (this includes a MD5 checksum value)
 *  2.  waitForPing() -> Wait for a ping from client indicating its ready to receive
 *  3.  uploadFile() -> Upload the file 
 * 
 * @author Frederick Ceder
 */
public class Upload implements IUpload {

    private Socket sock;    
    private OutputStream out;
    private InputStream in;
    private PrintWriter outPrint = null;
    
    // Default packageSize
    private int packageSize = 65536;
    private String fileName;
    private File file;
    private String host;
    private String fileHash = null;
    
    private long fileSize;
    private long uploadedBytes;
    
    // Constant for indicating that end of line has been reached
    private final String EOL = "eol";

    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        try {
            String fileName = args[0];
            int port = Integer.parseInt(args[1]);
            ServerSocket servsock = new ServerSocket(port);
            (new Thread(new Upload(servsock.accept(), fileName))).start();      
        } catch(NumberFormatException ex) {
            System.err.println("Unknown port!");
            System.exit(0);
        } catch(FileNotFoundException ex)  {
            System.err.println("File not found!");
            System.exit(0);
        } catch(IOException ex) {
            System.err.println("IOexception!");
            System.exit(0);
        } 
    }
    
    /**
     * Initiates an upload instance.
     * 
     * packagesSize will be defaulted to 65kb.
     * 
     * @param sock
     * @param fileName
     * @throws IOException 
     */
    public Upload(Socket socket, String fileName) throws IOException
    {
        this.sock = socket;
        this.fileName = fileName;
        this.file = new File(this.fileName);
        if(!file.exists()) throw new FileNotFoundException();
        this.fileSize = file.length();
        this.uploadedBytes = 0;
        this.host = socket.getInetAddress().getHostAddress();
        // Prepare streams
        out = sock.getOutputStream();
        in = sock.getInputStream();
    }
    
    /**
     * Initiates an upload instance.
     * 
     * packagesSize will be defaulted to 65kb.
     * 
     * @param sock
     * @param fileName
     * @throws IOException 
     */
    public Upload(Socket socket, File file) throws IOException
    {
        this.sock = socket;
        this.file = file;
        if(!file.exists()) throw new FileNotFoundException();
        this.fileName = fileName;
        this.fileSize = file.length();
        this.uploadedBytes = 0;
        this.host = socket.getInetAddress().getHostAddress();
        // Prepare streams
        out = sock.getOutputStream();
        in = sock.getInputStream();
    }

    /**
     * Initiates an upload instance.
     * 
     * packagesSize will be defaulted to 65kb.
     * 
     * @param sock
     * @param fileName
     * @throws IOException 
     */
    public Upload(Socket socket, String fileName, String fileHash) throws IOException
    {
        this.sock = socket;
        this.fileName = fileName;
        this.file = new File(this.fileName);
        if(!file.exists()) throw new FileNotFoundException();
        this.fileSize = file.length();
        this.uploadedBytes = 0;
        this.host = socket.getInetAddress().getHostAddress();
        // Prepare streams
        out = sock.getOutputStream();
        in = sock.getInputStream();
        
        this.fileHash = fileHash;
    }
    
    /**
     * Initiates an upload instance.
     * 
     * @param socket
     * @param fileName
     * @param packageSize
     * @throws IOException 
     */
    public Upload(Socket socket, String fileName, int packageSize) throws IOException
    {
        this.sock = socket;
        this.packageSize = packageSize;
        this.fileName = fileName;
        this.file = new File(this.fileName);
        if(!file.exists()) throw new FileNotFoundException();
        this.fileSize = file.length();
        this.uploadedBytes = 0;
        this.host = socket.getInetAddress().getHostAddress();
        // Prepare streams
        out = sock.getOutputStream();
        in = sock.getInputStream();
    }
    
    /**
     * Initiates an upload instance.
     * 
     * @param socket
     * @param fileName
     * @param packageSize
     * @throws IOException 
     */
    public Upload(Socket socket, File file, int packageSize) throws IOException
    {
        this.sock = socket;
        this.packageSize = packageSize;
        this.file = file;
        if(!file.exists()) throw new FileNotFoundException();
        this.fileName = file.getName();
        this.fileSize = file.length();
        this.uploadedBytes = 0;
        this.host = socket.getInetAddress().getHostAddress();
        // Prepare streams
        out = sock.getOutputStream();
        in = sock.getInputStream();
    }
    
    /**
     * Initiates an upload instance.
     * 
     * @param socket
     * @param fileName
     * @param packageSize
     * @throws IOException 
     */
    public Upload(Socket socket, String fileName, int packageSize, String fileHash) throws IOException
    {
        this.sock = socket;
        this.packageSize = packageSize;
        this.fileName = fileName;
        this.file = new File(this.fileName);
        if(!file.exists()) throw new FileNotFoundException();
        this.fileSize = file.length();
        this.uploadedBytes = 0;
        this.host = socket.getInetAddress().getHostAddress();
        // Prepare streams
        out = sock.getOutputStream();
        in = sock.getInputStream();
        
        this.fileHash = fileHash;
    }
    
    /**
     * Initiates an upload instance.
     * 
     * @param socket
     * @param fileName
     * @param packageSize
     * @throws IOException 
     */
    public Upload(Socket socket, File file, int packageSize, String fileHash) throws IOException
    {
        this.sock = socket;
        this.packageSize = packageSize;
        this.file = file;
        if(!file.exists()) throw new FileNotFoundException();
        this.fileName = file.getName();
        this.fileSize = file.length();
        this.uploadedBytes = 0;
        this.host = socket.getInetAddress().getHostAddress();
        // Prepare streams
        out = sock.getOutputStream();
        in = sock.getInputStream();
        
        this.fileHash = fileHash;
    }
    
    @Override
    public void run() {
        try {
            // Prepare client
            initClient();
            System.out.println(logMsg("Waits for readyping..."));
            // Wait for ping that client is ready to receive
            waitForPing();
            System.out.println(logMsg("Client is ready!"));

            // Start upload of file
            uploadFile();

            System.out.println(logMsg("Upload finished!"));

        } catch (InterruptedException ex) {
        } catch (IOException ex) {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        } catch(NoSuchAlgorithmException ex) {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        }  finally {
            try {
                in.close();
                out.close();
                sock.close();
            } catch (IOException ex) {
                Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Initializes the Client. Sends filesize, filename and packagesize values
     * to client.
     * 
     * Also sends a filehash (MD5) value to client.
     * 
     * @throws IOException 
     */
    private void initClient() throws IOException, NoSuchAlgorithmException, InterruptedException
    {
        // Calculate MD5 checksum of file.
        if(fileHash == null) fileHash = calculateHash(MessageDigest.getInstance("MD5"), fileName);  
        System.out.println(logMsg("Sends filename and size."));
        // Printwriter in order to send strings over outputstream
        sendText("size " + Long.toString(file.length()), false);
        if(Thread.interrupted()) throw new InterruptedException();
        sendText("name " + fileName, false);
        if(Thread.interrupted()) throw new InterruptedException();
        sendText("packagesize " + packageSize, false);
        if(Thread.interrupted()) throw new InterruptedException();
        sendText("md5hash " + fileHash, true);
        if(Thread.interrupted()) throw new InterruptedException();
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
        if(Thread.interrupted()) throw new InterruptedException();
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
            if(Thread.interrupted()) throw new InterruptedException();
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
     * Sends a textstring to a client. If EOL is true then this means that
     * the end of line symbol will be sent too, indicating that nothing more
     * will be sent.
     * 
     * @param input
     * @param eol
     * @throws IOException 
     */
    private void sendText(String input, boolean eol) throws IOException
    {
        if(outPrint == null) outPrint = new PrintWriter(out);
        outPrint.println(input);
        if(eol) {
            outPrint.println(EOL);
        }
        outPrint.flush();
    }

    /**
     * Waits for a response from the client, indicating that it is ready.
     * @throws IOException 
     */
    private void waitForPing() throws IOException
    {
        // Reads a byte.
        in.read();
        System.out.println(logMsg("Received readycheck."));
    }

    /**
     * Uploads a file. 
     * 
     * File is read into packages of the size packagesize and sent off individually.
     * 
     * NOTE client should be checked if it is ready to receive, so that it can
     * write the packages into a file.
     * 
     * @throws FileNotFoundException
     * @throws IOException
     * @throws NoSuchAlgorithmException 
     */
    private void uploadFile() throws FileNotFoundException, IOException, NoSuchAlgorithmException, InterruptedException
    {
        BufferedInputStream bis = null;
        try {
            
            // A "package"
            byte[] byteArray = new byte[packageSize];             
            
            // Inputstream from the file
            bis = new BufferedInputStream(new FileInputStream(file));                

            int packages = (int) (file.length()/packageSize) + 1;
            logMsg(packages + "");                

            System.out.println(logMsg("Starts sending..."));
            for(int i = 0; i < packages; i++)
            {
                // Reads at least an arraylength of bytes from the file into
                // the package
                if(Thread.interrupted()) throw new InterruptedException();
                int bytesRead = bis.read(byteArray, 0, byteArray.length);
                
                // Writes the amount data in the array to the outputstream
                out.write(byteArray, 0, bytesRead);
                out.flush();
                uploadedBytes += bytesRead;
           }
           System.out.println(logMsg("Sent file!"));
        } finally {
            bis.close();
        }
    }
    
    /**
     * This method logs the uploadprogress. 
     *
     * 
     * @param msg
     * @return 
     */
    private String logMsg(String msg)
    {
       // return "";
        return "THREAD-" + host + " " + msg;
    }
    
   /**
    * Returns a percentage value of
    * @return progressPercentage
    */
    @Override
    public int getProgress()
    {
        return (int) (((float) uploadedBytes / (float) fileSize) * 100);
    }
   
    
    /**
     * Returns the bytes uploaded at that instance.
     * Note that this value changes very quickly.
     * @return 
     */
    @Override
    public long getBytesDownloaded()
    {
        return uploadedBytes;
    }
    
    /**
     * Returns the name of the file being uploaded
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


}