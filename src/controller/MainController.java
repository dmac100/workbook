package controller;

import java.util.ArrayList;
import java.util.List;

import script.Script;
import view.CellList;
import view.Console;

public class MainController {
	private final Script script;
	private final List<Console> consoles = new ArrayList<>();
	
	public MainController() {
		this.script = new Script();
	}

	public void addCellList(CellList cellList) {
		cellList.setExecuteFunction(command -> script.eval(command, this::addOutput, this::addError));
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
}
