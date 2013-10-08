package org.ggp.base.util.reflection;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.ggp.base.apps.kiosk.GameCanvas;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.util.configuration.ProjectConfiguration;


public class ProjectSearcher {
	public static void main(String[] args)
	{
		System.out.println(getAllGamers());
	}
	
	private static <T> List<Class<? extends T>> getAllClassesThatAre(Class<T> ofThisType) 
	{
		return getAllClassesThatAre(ofThisType, true);
	}
	
	private static <T> List<Class<? extends T>> getAllClassesThatAre(Class<T> ofThisType, boolean mustBeConcrete)
	{
		List<Class<? extends T>> rval = new ArrayList<Class<? extends T>>();
		for(String name : allClasses) {
			if(name.contains("Test_"))
				continue; 
			
			Class<?> c = null;
			try {	
				c = Class.forName(name);
			} catch (ClassNotFoundException ex)  { 
				throw new RuntimeException(ex); 
			}
			
			if(ofThisType.isAssignableFrom(c) && (!mustBeConcrete || !Modifier.isAbstract(c.getModifiers()))) {
				@SuppressWarnings("unchecked")
				Class<? extends T> cls = (Class<? extends T>) c; 
				rval.add(cls);
			}
		}
		return rval;
	}
	
	private static List<String> allClasses = findAllClasses();
	
	private static List<String> findAllClasses()
	{
		FilenameFilter filter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
		    return !name.startsWith(".");
		}
	    };
		
		List<String> rval = new ArrayList<String>();
		Stack<File> toProcess = new Stack<File>();		
		for(String classDirName : ProjectConfiguration.classRoots)
		    toProcess.add(new File(classDirName));
		while(!toProcess.empty())
		{
			File f = toProcess.pop();
			if(!f.exists())
				System.out.println("Could not find expected file: [" + f + "] when running ProjectSearcher.");
			if(f.isDirectory())
				toProcess.addAll(Arrays.asList(f.listFiles(filter)));
			else
			{
				if(f.getName().endsWith(".class"))
				{					
					String fullyQualifiedName = f.getPath();
					for(String classDirName : ProjectConfiguration.classRoots) {
					    fullyQualifiedName = fullyQualifiedName.replaceAll("^" + classDirName.replace(File.separatorChar, '.'), "");
					}
					fullyQualifiedName = fullyQualifiedName.replaceAll("\\.class$","");
					fullyQualifiedName = fullyQualifiedName.replaceAll("^[\\\\/]", "");
					fullyQualifiedName = fullyQualifiedName.replaceAll("[\\\\/]", ".");
					rval.add(fullyQualifiedName);
				}
			}
		}
		
		return rval;
	}

	public static List<Class<? extends GameCanvas>> getAllGameCanvases() {
		return getAllClassesThatAre(GameCanvas.class);
	}

	public static List<Class<? extends Gamer>> getAllGamers() {
		return getAllClassesThatAre(Gamer.class);
	}
}
