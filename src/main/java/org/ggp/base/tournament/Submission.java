package org.ggp.base.tournament;

import org.ggp.base.player.GamePlayer;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.core.ZipFile;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.Block;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Filters.*;
import org.bson.Document;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.StandardJavaFileManager;

import org.apache.commons.io.FileUtils;
import java.io.File;
import java.nio.file.Files;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.io.IOException;

/*
*   Some printlns to be removed
*/
class Submission  {
    static final String home = System.getProperty("user.home");
    static final String uploadDir = home + "/.ggp-server/uploads/";
    static final String compileDir = home + "/.ggp-server/compiled/";

    public static void unzip(String fileName) {
        String base = fileName.split("\\.(?=[^\\.]+$)")[0];
        String destination = compileDir + base;
        String password = "password";
        String source = uploadDir + fileName;

        try {
            ZipFile zipFile = new ZipFile(source);
            if (zipFile.isEncrypted()) {
                zipFile.setPassword(password);
            }
            File uncompressed = new File(destination);
            uncompressed.mkdirs();
            zipFile.extractAll(destination);

            // delete __MACOSX folder
            File[] thingsInDir = uncompressed.listFiles();
            for (File f : thingsInDir) {
                if (f.isDirectory() && f.getName().equals("__MACOSX")) {
                    FileUtils.deleteDirectory(f);
                }
            }
        } catch (ZipException zipe) {
            zipe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void compile(String playerPackage) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

            String[] extensions = {"java"};
            Collection<File> allJavaFiles = FileUtils.listFiles(new File(playerPackage), extensions, true);
            Iterable<? extends JavaFileObject> compilationUnits2 =
               fileManager.getJavaFileObjects(allJavaFiles.toArray( new File[allJavaFiles.size()]) ); // use alternative method

            // reuse the same file manager to allow caching of jar files
            compiler.getTask(null, fileManager, null, null, null, compilationUnits2).call();

            fileManager.close();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static String compilePlayer(String compressedFileName) {
        // compile unzipped files, Need only one root package
        String base = compressedFileName.split("\\.(?=[^\\.]+$)")[0];
        String playerDir = compileDir + base;
        int numdir = 0;
        String packageName = "";
        File[] thingsInDir = new File(playerDir).listFiles();
        
        for (File f : thingsInDir) {
            if (f.isDirectory()) {
                numdir++;
                packageName = f.getName();
            }
        }
        
        if (numdir == 0 || numdir > 1) {
            System.out.println("Sorry, we need only one package.");
            return null;
        } else {
            System.out.println("playerDir = " + playerDir + "/" + packageName);
            compile(playerDir + "/" + packageName);
            return playerDir;
        }
    }

    public static void updateTournament(MongoCollection<Document> tournaments, Document player) {
        tournaments.updateOne(eq("_id", player.get("latestTourId")), 
                            new Document("$push", new Document("players", player.get("_id"))));
    }

    public static Document updatePlayer(MongoCollection<Document> players, 
                                        String compressedFileName, 
                                        String pathToClasses) {
        // Player status: 'Compiled', 'no Gamer class', 'Compile failed'
        try {
            
            URL url = new File(pathToClasses).toURL();
            URL[] urls = new URL[]{url};
            ClassLoader cl = new URLClassLoader(urls);
            String[] extensions = {"class"};
            String packageName = new File(pathToClasses).listFiles()[0].getName();
            String pathToPackage = pathToClasses + "/" + packageName;
            Collection<File> allClassFiles = 
                FileUtils.listFiles(new File(pathToPackage), extensions, false);
            
            for (Iterator<File> it = allClassFiles.iterator(); it.hasNext();) {
                File f = it.next();
                System.out.println("File name = " + f.getName());
                String playerName = f.getName().split("\\.(?=[^\\.]+$)")[0];
                String playerPackage = packageName + "." + playerName;
                Class aClass = cl.loadClass(playerPackage);
                if (Gamer.class.isAssignableFrom(aClass)) {
                    System.out.println( f.getName() + " is a player!!");
                    players.updateOne(eq("compressedFileName", compressedFileName),
                            new Document("$set", new Document("status", "compiled")
                                                    .append("name", playerName)
                                                    .append("pathToClasses", pathToClasses)));

                    return players.find(eq("compressedFileName", 
                        compressedFileName)).sort(descending("createdAt")).first();
                }
            }

            // URL url = new File(playerDir).toURL();
            // URL[] urls = new URL[]{url};
            // ClassLoader cl = new URLClassLoader(urls);
            // final String packageName = "csula";
            // final String playerName = "MyPlayer.java";
            // String playerPackage = packageName + playerName;
            // Class gamerClass = cl.loadClass(playerPackage);
            // Gamer gamer = (Gamer) gamerClass.newInstance();
            // int port = 9147;
            // GamePlayer sampleLegelPlayer = new GamePlayer(port, gamer);
            // sampleLegelPlayer.start();
            // sampleLegelPlayer.shutdown();
            
            // Object newObject = Class.forName(strFullyQualifiedClassName).newInstance();
            // Gamer gamer = (Gamer) chosenGamerClass.newInstance();
            // new GamePlayer(port, gamer).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        MongoClient mongoClient = new MongoClient( "localhost" , 3001 );
        MongoDatabase database = mongoClient.getDatabase("meteor");
        MongoCollection<Document> players = database.getCollection("players");
        MongoCollection<Document> tournaments = database.getCollection("tournaments");

        try {
            while (true) {
                for (Document thePlayer : 
                    players.find(eq("status", "not compiled")).sort(descending("createdAt"))) 
                {
                               
                    String compressedFileName = thePlayer.get("compressedFileName").toString();
                    // System.out.println("--- unzip and compile ---");
                    // System.out.println(compressedFileName);
                    unzip(compressedFileName);

                    String pathToClasses = compilePlayer(compressedFileName);
                    if (pathToClasses != null) {
                        updatePlayer(players, compressedFileName, pathToClasses);
                        updateTournament(tournaments, thePlayer);
                    }

                }

                Thread.sleep(20 * 1000);
                System.out.println("--- 20 seconds passed ---");
            }
        } catch (InterruptedException interruptE) {
            interruptE.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // runPlayer(playerDir);
    }
}
