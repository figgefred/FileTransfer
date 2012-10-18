/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upload;

/**
 *
 * @author fred
 */
public interface IUpload extends Runnable {
    
   /**
    * Returns a percentage value of
    * @return progressPercentage
    */
    public int getProgress();
   
    
    /**
     * Returns the bytes uploaded at that instance.
     * Note that this value changes very quickly.
     * @return 
     */
    public long getBytesDownloaded();
    
    /**
     * Returns the name of the file being uploaded
     * @return 
     */
    public String getFileName();
    
    /**
     * Returns size of the source file in bytes.
     * @return 
     */
    public long getFileSize();
   
    
    
}
