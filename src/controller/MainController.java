package controller;

import java.util.ArrayList;
import java.util.List;

import editor.Editor;
import editor.ScriptBindingReference;
import editor.StringEditor;
import script.Script;
import view.CellList;
import view.Console;

public class MainController {
	private final Script script;
	private final List<Console> consoles = new ArrayList<>();
	private final List<Editor> editors = new ArrayList<>();
	
	public MainController() {
		this.script = new Script();
	}

	public void addCellList(CellList cellList) {
		cellList.setExecuteFunction(command -> {
			Object result = script.eval(command, this::addOutput, this::addError);
			onEval();
			return result;
		});
	}
	
	private void onEval() {
		editors.forEach(Editor::readValue);
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

	public void addEditor(StringEditor editor) {
		editor.setReference(new ScriptBindingReference(script, editor.getName()));
		editors.add(editor);
	}
}
