package ClassPathAgent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import net.bytebuddy.agent.ByteBuddyAgent;

public class ClassPathUtil {
	private static File agentJar = null;
	
	public static void addJarsToClassPath(List<File> jarFiles) throws IOException {
		try {
			addJarsToClassPathUsingUrlClassLoader(jarFiles);
		} catch(Exception e) {
			addJarsToClassPathUsingAgent(jarFiles);
		}
	}
	
	private static void addJarsToClassPathUsingAgent(List<File> jarFiles) throws IOException {
		if(agentJar == null) {
			agentJar = Files.createTempFile("agent", ".jar").toFile();
			agentJar.deleteOnExit();
		}
		
		try(InputStream inputStream = ClassPathUtil.class.getResourceAsStream("/ClassPathAgent/agent.jar")) {
			FileUtils.copyInputStreamToFile(inputStream, agentJar);
		}
		
		long pid = ProcessHandle.current().pid();
		
		ByteBuddyAgent.attach(agentJar, String.valueOf(pid), StringUtils.join(jarFiles, ","));
	}
	
	private static void addJarsToClassPathUsingUrlClassLoader(List<File> jarFiles) throws ReflectiveOperationException, IOException {
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		
		URLClassLoader urlClassLoader = ((URLClassLoader) classLoader);
		
		Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		method.setAccessible(true);
		for(File jarFile:jarFiles) {
			method.invoke(urlClassLoader, jarFile.toURL());
		}
	}
}