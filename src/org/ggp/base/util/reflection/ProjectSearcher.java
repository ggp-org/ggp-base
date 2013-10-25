package org.ggp.base.util.reflection;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Modifier;
import java.util.*;

import com.google.common.collect.Sets;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.util.configuration.ProjectConfiguration;

public class ProjectSearcher {
	public static void main(String[] args)
	{
		System.out.println(getAllClassesThatAre(Gamer.class));
	}

	public static List<Class<?>> getAllClassesThatAre(Class<?> ofThisType)
	{
		return getAllClassesThatAre(ofThisType, true);
	}

	public static List<Class<?>> getAllClassesThatAre(Class<?> ofThisType, boolean mustBeConcrete)
	{
		List<Class<?>> rval = new ArrayList<Class<?>>();
        findClassesInList(allClasses, ofThisType, mustBeConcrete, rval);
        findClassesInList(injectedClasses, ofThisType, mustBeConcrete, rval);
        return rval;
	}

    private static void findClassesInList(Set<String> classesToSearch, Class<?> ofThisType,
                                          boolean mustBeConcrete, List<Class<?>> rval) {
        for(String name : classesToSearch) {
            if(name.contains("Test_"))
                continue;

            Class<?> c = null;
            try {
                c = Class.forName(name);
            } catch (ClassNotFoundException ex)  {
                throw new RuntimeException(ex);
            }

            if(ofThisType.isAssignableFrom(c) && (!mustBeConcrete || !Modifier.isAbstract(c.getModifiers())) )
                rval.add(c);
        }
    }

    private static Set<String> allClasses = findAllClasses();
    private static Set<String> injectedClasses = Sets.newHashSet();

    public static <T> void injectClass(Class<T> klass) {
        injectedClasses.add(klass.getCanonicalName());
    }

	private static Set<String> findAllClasses()
	{
		FilenameFilter filter = new FilenameFilter() {
	        public boolean accept(File dir, String name) {
	            return !name.startsWith(".");
	        }
	    };

		Set<String> rval = Sets.newHashSet();
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
}
