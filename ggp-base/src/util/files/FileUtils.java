package util.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {
    /** 
     * @param filePath the name of the file to open.
     */ 
    public static String readFileAsString(File file)
    {
        try {
            return readFileAsString(new BufferedReader(new FileReader(file)));
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
            return null;
        }
    }
    
	/** 
     * @param file the file to open.
     */ 
    private static String readFileAsString(BufferedReader reader)
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
        } catch(Exception ex) {
        	ex.printStackTrace();
            return null;
        }
    }

    public static void overwriteFileWithString(File file, String newContents) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.append(newContents);
        writer.close();
    }
}
