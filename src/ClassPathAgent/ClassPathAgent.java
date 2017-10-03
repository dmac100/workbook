package ClassPathAgent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarFile;

public class ClassPathAgent {
    public static void agentmain(String args, Instrumentation instrumentation) throws IOException {
    	String jarFiles = new String(Files.readAllBytes(Paths.get(args)), "UTF-8");
    	
    	for(String jarFile:jarFiles.split(",")) {
    		instrumentation.appendToSystemClassLoaderSearch(new JarFile(jarFile));
    	}
    }
}