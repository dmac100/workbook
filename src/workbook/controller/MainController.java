package workbook.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jdom2.Element;

import com.google.common.eventbus.EventBus;

import workbook.editor.reference.OgnlReference;
import workbook.editor.ui.Editor;
import workbook.event.ScriptTypeChangeEvent;
import workbook.model.Model;
import workbook.script.Engine;
import workbook.script.NameAndProperties;
import workbook.script.ScriptController;
import workbook.script.ScriptFuture;
import workbook.util.ThrottledConsumer;
import workbook.view.ConsoleTabbedView;
import workbook.view.ScriptTabbedView;
import workbook.view.WorksheetTabbedView;
import workbook.view.canvas.CanvasTabbedView;

public class MainController {
	private final ScriptController scriptController = new ScriptController();
	private final EventBus eventBus;
	private final Model model;
	
	private final List<ConsoleTabbedView> consoles = new ArrayList<>();
	
	private final Consumer<Void> flushConsoleConsumer;
	
	private final StringBuilder outputBuffer = new StringBuilder();
	private final StringBuilder errorBuffer = new StringBuilder();

	
	public MainController(EventBus eventBus, Model model) {
		this.eventBus = eventBus;
		this.model = model;
		
		scriptController.startQueueThread();
		
		flushConsoleConsumer = new ThrottledConsumer<Void>(100, true, result -> flushConsole());
	}
	
	public void clear() {
		consoles.clear();
	}

	public WorksheetTabbedView addWorksheet(WorksheetTabbedView worksheet) {
		worksheet.setExecuteFunction(command -> {
			ScriptFuture<Object> result = scriptController.eval(command, this::addOutput, this::addError);
			result.thenAccept(value -> scriptController.setVariable("_", value));
			return result;
		});
		return worksheet;
	}
	
	public WorksheetTabbedView addWorksheet(WorksheetTabbedView worksheet, Function<String, Object> commandFunction) {
		worksheet.setExecuteFunction(command -> {
			ScriptFuture<Object> result = scriptController.exec(() -> commandFunction.apply(command));
			result.thenAccept(value -> scriptController.setVariable("_", value));
			return result;
		});
		return worksheet;
	}
	
	public ScriptTabbedView addScriptEditor(ScriptTabbedView scriptEditor) {
		scriptEditor.setExecuteFunction(command -> {
			return scriptController.eval(command, this::addOutput, this::addError);
		});
		return scriptEditor;
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
		consoles.forEach(console -> console.addOutput(output));
		consoles.forEach(console -> console.addError(error));
	}
	
	public void clearConsole() {
		consoles.forEach(console -> console.clear());
	}

	public ConsoleTabbedView addConsole(ConsoleTabbedView console) {
		console.getControl().addDisposeListener(event -> consoles.remove(console));
		consoles.add(console);
		return console;
	}
	
	public CanvasTabbedView addCanvasView(CanvasTabbedView canvas) {
		canvas.setExecuteCallback(command -> {
			List<String> callbackNames = Arrays.asList("rect", "ellipse", "fill", "circle", "line", "text");
			ScriptFuture<List<NameAndProperties>> result = scriptController.evalWithCallbackFunctions(command, callbackNames, this::addOutput, this::addError);
			result.thenAccept(value -> {
				canvas.setCanvasItems(value);
			});
		});
		return canvas;
	}

	public <T extends Editor> T addEditor(T editor) {
		editor.setReferenceFunction(expression -> new OgnlReference(scriptController, expression));
		eventBus.register(editor);
		editor.getControl().addDisposeListener(event -> eventBus.unregister(editor));
		return editor;
	}
	
	public void interrupt() {
		scriptController.interrupt();
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
		Element scriptTypeElement = new Element("ScriptType");
		scriptTypeElement.setText(scriptController.getScriptType().toString());
		element.addContent(scriptTypeElement);
	}

	public void deserialize(Element element) {
		String scriptType = element.getChild("ScriptType").getText();
		scriptController.setScriptType(scriptType);
	}
}