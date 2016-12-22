package workbook.controller;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import com.google.common.eventbus.EventBus;

import workbook.event.MinorRefreshEvent;
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
		eventBus.post(new MinorRefreshEvent(this));
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
			addChildrenFromString(globalsElement, scriptController.serializeGlobals().get());
			element.addContent(globalsElement);
		} catch(ExecutionException | InterruptedException | JDOMException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public void deserialize(Element element) {
		// Deserialize script type.
		String scriptType = element.getChildText("ScriptType");
		scriptController.setScriptType(scriptType);
		
		// Deserialize globals.
		try {
			List<Element> globals = element.getChild("Globals").getChildren();
			if(!globals.isEmpty()) {
				String globalsXml = toXmlString(globals.get(0));
				scriptController.deserializeGlobals(globalsXml).get();
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the contents of the element as an XML String.
	 */
	private static String toXmlString(Element element) {
		return new XMLOutputter().outputString(element);
	}

	/**
	 * Adds the contents of childXml to the parent element after parsing as xml.
	 */
	private static void addChildrenFromString(Element parent, String childXml) throws JDOMException, IOException {
		Element element = new SAXBuilder().build(new StringReader(childXml)).getRootElement();
		element.detach();
		parent.addContent(element);
	}
}