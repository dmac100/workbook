package workbook.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;
import org.jdom2.Element;

import com.google.common.base.Splitter;
import com.google.common.eventbus.EventBus;

import workbook.event.OutputEvent;
import workbook.event.ScriptTypeChangeEvent;
import workbook.model.Model;
import workbook.script.Engine;
import workbook.script.ScriptController;
import workbook.util.ThrottledConsumer;

public class MainController {
	private final ScriptController scriptController = new ScriptController();
	private final EventBus eventBus;
	private final Model model;
	
	private final Consumer<Void> flushConsoleConsumer;
	
	private final StringBuilder outputBuffer = new StringBuilder();
	private final StringBuilder errorBuffer = new StringBuilder();

	public MainController(EventBus eventBus, Model model) {
		this.eventBus = eventBus;
		this.model = model;
		
		scriptController.startQueueThread();
		
		scriptController.setOutputCallbacks(
			line -> Display.getDefault().asyncExec(() -> addOutput(line)),
			line -> Display.getDefault().asyncExec(() -> addError(line))
		);
		
		flushConsoleConsumer = new ThrottledConsumer<Void>(100, true, result -> flushConsole());
	}
	
	private void addOutput(String output) {
		outputBuffer.append(output + "\n");
		flushConsoleConsumer.accept(null);
	}
	
	private void addError(String error) {
		errorBuffer.append(error + "\n");
		flushConsoleConsumer.accept(null);
	}
	
	private void flushConsole() {
		String output = outputBuffer.toString();
		String error = errorBuffer.toString();
		outputBuffer.setLength(0);
		errorBuffer.setLength(0);
		eventBus.post(new OutputEvent(output, error));
	}
	
	public void interrupt() {
		scriptController.interrupt();
	}
	
	public void clearGlobals() {
		scriptController.clearGlobals();
	}

	public ScriptController getScriptController() {
		return scriptController;
	}
	
	public void registerEngine(String scriptType, Engine engine) {
		scriptController.addEngine(scriptType, engine);
	}

	public void setEngine(String scriptType) {
		scriptController.setScriptType(scriptType)
			.thenRun(() -> {
				scriptController.getScript(engine -> {
					model.setScriptType(scriptType);
					model.setBrush(engine.getBrush());
					eventBus.post(new ScriptTypeChangeEvent());
				});
			});
	}
	
	public String getEngine() {
		return model.getScriptType();
	}
	
	public void addVariable(String name, Object value) {
		scriptController.setVariable(name, value);
	}

	public void serialize(Element element) {
		try {
			// Serialize script type.
			Element scriptTypeElement = new Element("ScriptType");
			scriptTypeElement.setText(scriptController.getScriptType().toString());
			element.addContent(scriptTypeElement);
			
			// Serialize globals.
			Element globalsElement = new Element("Globals");
			element.addContent(globalsElement);
			scriptController.exec(() -> {
				try {
					globalsElement.setText(serializeGlobals(scriptController.getGlobalsSync()));
				} catch(IOException e) {
					e.printStackTrace();
				}
				return null;
			}).get();
		} catch(ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void deserialize(Element element) {
		try {
			// Deserialize script type.
			String scriptType = element.getChildText("ScriptType");
			scriptController.setScriptType(scriptType);
			
			// Deserialize globals.
			if(element.getChild("Globals") != null) {
				Map<String, Object> globalsMap = deserializeGlobals(element.getChildText("Globals"));
				scriptController.exec(() -> {
					scriptController.getGlobalsSync().putAll(globalsMap);
					return null;
				});
			}
		} catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Deserializes the global map from a String.
	 */
	private static Map<String, Object> deserializeGlobals(String text) throws IOException, ClassNotFoundException {
		byte[] globals = Base64.getDecoder().decode(text.replaceAll("\\s", ""));
		ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(globals));
		
		Map<String, Object> globalsMap = new HashMap<>();
		
		// Read name and value of each variable.
		while(true) {
			String name = (String) objectInputStream.readObject();
			if(name == null) break;
			Object value = objectInputStream.readObject();
			globalsMap.put(name, value);
		}
		
		return globalsMap;
	}

	/**
	 * Returns the globals map serialized into a String.
	 */
	private static String serializeGlobals(Map<String, Object> globalsMap) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
		
		for(Entry<String, Object> entry:globalsMap.entrySet()) {
			if(!entry.getKey().equals("system")) {
				if(entry.getValue() instanceof Serializable) {
					// Serialize name and value of each variable.
					objectOutputStream.writeObject(entry.getKey());
					objectOutputStream.writeObject(entry.getValue());
				}
			}
		}
		
		// Mark end of variables.
		objectOutputStream.writeObject(null);

		return wrap(Base64.getEncoder().encodeToString(outputStream.toByteArray()));
	}

	/**
	 * Returns the string with wrapped lines.
	 */
	private static String wrap(String string) {
		int w = 60;
		StringBuilder wrapped = new StringBuilder();
		for(int c = 0; c < string.length(); c += w) {
			wrapped.append(string.substring(c, Math.min(string.length(), c + w)));
			wrapped.append("\n");
		}
		return wrapped.toString();
	}
}