package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;

import editor.Editor;
import editor.OgnlReference;
import script.Script;
import util.ThrottledConsumer;
import view.CellList;
import view.Console;

public class MainController {
	private final Display display;
	private final Script script;
	private final List<Console> consoles = new ArrayList<>();
	private final List<Editor> editors = new ArrayList<>();
	
	private final Consumer<Object> evalConsumer = new ThrottledConsumer<>(100, true, result -> onEval(result));
	
	public MainController(Display display) {
		this.display = display;
		this.script = new Script();
	}

	public void addCellList(CellList cellList) {
		cellList.setExecuteFunction(command -> {
			Object result = script.eval(command, this::addOutput, this::addError);
			evalConsumer.accept(result);
			return result;
		});
	}
	
	private void onEval(Object result) {
		display.asyncExec(() -> {
			script.addVariable("_", result);
			editors.forEach(Editor::readValue);
		});
	}

	private void addOutput(String output) {
		consoles.forEach(console -> console.addOutput(output));
	}
	
	private void addError(String error) {
		consoles.forEach(console -> console.addError(error));
	}

	public void addConsole(Console console) {
		consoles.add(console);
	}

	public void addEditor(Editor editor) {
		editor.setReference(new OgnlReference(script.getVariableMap(), editor.getExpression()));
		editors.add(editor);
	}
}
