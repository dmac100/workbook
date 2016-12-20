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
			globalsElement.setText(scriptController.serializeGlobals().get());
		} catch(ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void deserialize(Element element) {
		// Deserialize script type.
		String scriptType = element.getChildText("ScriptType");
		scriptController.setScriptType(scriptType);
		
		// Deserialize globals.
		scriptController.deserializeGlobals(element.getChildText("Globals"));
	}
}