package workbook.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

import ClassPathAgent.ClassPathUtil;

public class IvyDownloader {
	private static File WORKBOOK_DIR = new File(System.getProperty("user.home"), ".workbook");
	
	/**
	 * Downloads and loads a dependency given by a short form ivy description.
	 */
	public static void downloadDependency(String dependency) {
		downloadDependencies(Arrays.asList(dependency));
	}
	
	/**
	 * Downloads and loads a list of dependencies given by short form ivy descriptions.
	 */
	public static void downloadDependencies(List<String> dependencies) {
		String retrievePattern = WORKBOOK_DIR.getAbsolutePath() + "/[organization]-[artifact]-[revision](-[classifier]).[ext]";
		
		List<File> files = new ArrayList<>();
		
		for(String dependency:dependencies) {
			String[] parts = dependency.split("[: ]");
			if(parts.length == 3) {
				try {
					files.addAll(retrieveIvy(parts[0], parts[1], parts[2], retrievePattern));
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		loadJars(files);
	}

	/**
	 * Loads a list of files into the system class loader. 
	 */
	private static void loadJars(List<File> files) {
		try {
			ClassPathUtil.addJarsToClassPath(files);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves a dependency from an ivy description, and returns the list of the retrieved files.
	 */
	private static List<File> retrieveIvy(String organization, String name, String revision, String retrievePattern) throws Exception {
		System.out.println("DOWNLOADING: " + organization + ", " + name + ", " + revision);
		
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
