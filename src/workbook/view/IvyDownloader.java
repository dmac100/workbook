package workbook.view;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.retrieve.RetrieveReport;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.MessageLogger;

public class IvyDownloader {
	private static File WORKBOOK_DIR = new File(System.getProperty("user.home"), ".workbook");
	
	/**
	 * Downloads and loads a dependency given by a short form ivy description.
	 */
	public static void downloadDependency(String dependency) {
		String retrievePattern = WORKBOOK_DIR.getAbsolutePath() + "/[organization]-[artifact]-[revision](-[classifier]).[ext]";
		
		String[] parts = dependency.split("[: ]");
		if(parts.length == 3) {
			try {
				List<File> files = retrieveIvy(parts[0], parts[1], parts[2], retrievePattern);
				loadJars(files);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Loads a list of files into the system class loader. 
	 */
	private static void loadJars(List<File> files) {
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		if(classLoader instanceof URLClassLoader) {
			URLClassLoader urlClassLoader = ((URLClassLoader) classLoader);
			
			for(File file:files) {
				try {
					Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
					method.setAccessible(true);
					method.invoke(urlClassLoader, file.toURL());
				} catch (ReflectiveOperationException | MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Retrieves a dependency from an ivy description, and returns the list of the retrieved files.
	 */
	private static List<File> retrieveIvy(String organization, String name, String revision, String retrievePattern) throws Exception {
		Ivy ivy = Ivy.newInstance();
		ivy.getLoggerEngine().pushLogger(createQuietLogger());
		IvySettings settings = ivy.getSettings();
		ivy.configureDefault();

		DefaultModuleDescriptor md = DefaultModuleDescriptor.newDefaultInstance(
			ModuleRevisionId.newInstance(organization, name + "-caller", "working")
		);
		
		ModuleRevisionId moduleRevisionId = ModuleRevisionId.newInstance(organization, name, revision);
		DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, moduleRevisionId, false, false, true);
		dd.addDependencyConfiguration("default", "*");
		md.addDependency(dd);

		ResolveOptions resolveOptions = new ResolveOptions().setConfs(new String[] { "default" });
		resolveOptions.setLog(ResolveOptions.LOG_QUIET);
		ResolveReport resolveReport = ivy.resolve(md, resolveOptions);

		if(resolveReport.hasError()) {
			resolveReport.getAllProblemMessages().forEach(System.err::println);
		}
		
		RetrieveOptions retrieveOptions = new RetrieveOptions().setConfs(new String[] { "default" }).setDestArtifactPattern(retrievePattern);
		retrieveOptions.setLog(RetrieveOptions.LOG_QUIET);
		RetrieveReport retrieveReport = ivy.retrieve(md.getModuleRevisionId(), retrieveOptions);
		
		return new ArrayList<>((Collection<File>) retrieveReport.getRetrievedFiles());
	}

	/**
	 * Returns an ivy logger that doesn't produce any output.
	 */
	private static MessageLogger createQuietLogger() {
		return new AbstractMessageLogger() {
			public void log(String msg, int level) {}
			public void rawlog(String msg, int level) {}
			protected void doEndProgress(String msg) {}
			protected void doProgress() {}
		};
	}
}
