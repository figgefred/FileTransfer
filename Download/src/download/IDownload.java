/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package download;

/**
 *
 * @author fred
 */
public interface IDownload extends Runnable {
    
   /**
    * Returns a percentage value of progress
    * @return progressPercentage
    */
    public int getProgress();
   
    
    /**
     * Returns the bytes downloaded at that instance.
     * Note that this value changes very quickly.
     * @return 
     */
    public long getBytesDownloaded();
    
    /**
     * Returns the name of the file being downloaded
     * @return 
     */
    public String getFileName();
    
    /**
     * Returns size of the external file in bytes.
     * @return 
     */
    public long getFileSize();
}
