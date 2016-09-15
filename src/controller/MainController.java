package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;

import editor.Editor;
import editor.OgnlReference;
import script.ScriptController;
import script.ScriptFuture;
import util.ThrottledConsumer;
import view.CellList;
import view.Console;

public class MainController {
	private final ScriptController scriptController = new ScriptController();
	private final List<Console> consoles = new ArrayList<>();
	private final List<Editor> editors = new ArrayList<>();
	
	private final Consumer<Object> evalConsumer;
	
	public MainController() {
		scriptController.startQueueThread();
		
		evalConsumer = new ThrottledConsumer<>(100, true, result -> {
			Display.getDefault().asyncExec(
				() -> onEval(result)
			);
		});
	}

	public void addCellList(CellList cellList) {
		cellList.setExecuteFunction(command -> {
			ScriptFuture<Object> result = scriptController.eval(command, this::addOutput, this::addError);
			result.thenAccept(evalConsumer);
			return result;
		});
	}
	
	private void onEval(Object result) {
		scriptController
			.setVariable("_", result)
			.thenRun(() -> editors.forEach(Editor::readValue));
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
		editor.setReference(new OgnlReference(scriptController, editor.getExpression()));
		editors.add(editor);
	}
	
	public void interrupt() {
		scriptController.interrupt();
	}

	public ScriptController getScriptController() {
		return scriptController;
	}
}