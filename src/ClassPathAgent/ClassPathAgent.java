package ClassPathAgent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

public class ClassPathAgent {
    public static void agentmain(String args, Instrumentation instrumentation) throws IOException {
    	for(String arg:args.split(",")) {
    		instrumentation.appendToSystemClassLoaderSearch(new JarFile(arg));
    	}
    }
}