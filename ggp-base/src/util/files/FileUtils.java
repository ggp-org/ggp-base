package util.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileUtils {
    /** 
     * @param filePath the name of the file to open.
     */ 
    public static String readFileAsString(File file)
    {
        try {
            return readFileAsString(new BufferedReader(new FileReader(file)));
        } catch (FileNotFoundException e) {
            return "";
        }
    }
    
    /** 
     * @param filePath the name of the file to open.
     */ 
    public static String readFileAsString(InputStream in)
    {
        return readFileAsString(new BufferedReader(new InputStreamReader(in)));
    }    
    
	/** 
     * @param file the file to open.
     */ 
    public static String readFileAsString(BufferedReader reader)
    {
        try
        {
            StringBuilder fileData = new StringBuilder(10000);
            char[] buf = new char[1024];
            int numRead=0;
            while((numRead=reader.read(buf)) != -1){
                fileData.append(buf, 0, numRead);
            }
            reader.close();
            return fileData.toString();
        }
        catch(Exception ex)
        {
            // TODO: this might be TOO suppressed
            // TODO: perhaps make this return NULL instead?
//            System.err.println("Unable to read file " + file + " as string");
//            ex.printStackTrace();
            return "";
        }
    }
    
}
