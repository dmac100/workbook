package classpathagent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import net.bytebuddy.agent.ByteBuddyAgent;

public class ClassPathUtil {
	private static File agentJar = null;
	
	private static Set<String> extraClassPath = new HashSet<>();
	
	public static void addJarsToClassPath(List<File> jarFiles) throws Exception {
		try {
			addJarsToClassPathUsingUrlClassLoader(jarFiles);
		} catch(Exception e) {
			addJarsToClassPathUsingAgent(jarFiles);
		}
		
		extraClassPath.addAll(Lists.transform(jarFiles, Object::toString));
	}
	
	private static void addJarsToClassPathUsingAgent(List<File> jarFiles) throws IOException, ReflectiveOperationException {
		if(agentJar == null) {
			agentJar = Files.createTempFile("agent", ".jar").toFile();
			agentJar.deleteOnExit();
		}
		
		try(InputStream inputStream = ClassPathUtil.class.getResourceAsStream("/classpathagent/agent.jar")) {
			FileUtils.copyInputStreamToFile(inputStream, agentJar);
		}
	
		Object currentProcess = Class.forName("java.lang.ProcessHandle").getMethod("current").invoke(null);
		long pid = (Long) Class.forName("java.lang.ProcessHandle").getMethod("pid").invoke(currentProcess);
		
		File jarListFile = Files.createTempFile("jarlist", ".txt").toFile();
		jarListFile.deleteOnExit();
		
		FileUtils.writeStringToFile(jarListFile, StringUtils.join(jarFiles, ","), Charsets.UTF_8);
		
		ByteBuddyAgent.attach(agentJar, String.valueOf(pid), jarListFile.getAbsolutePath());
		
		jarListFile.delete();
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