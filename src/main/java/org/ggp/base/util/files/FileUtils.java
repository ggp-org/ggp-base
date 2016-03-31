package org.ggp.base.util.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class FileUtils {
    private FileUtils() {
    }

    public static String readFileAsString(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder fileData = new StringBuilder(10000);
            char[] buf = new char[1024];
            int numRead=0;
            while((numRead=reader.read(buf)) != -1){
                fileData.append(buf, 0, numRead);
            }
            return fileData.toString();
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writeStringToFile(File file, String s) throws IOException {
        try (PrintStream out = new PrintStream(new FileOutputStream(file, false))) {
            out.print(s);
        }
    }
}
