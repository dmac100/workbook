package workbook.view;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandList {
	private final Map<String, Runnable> callbacks = new LinkedHashMap<>();
	
	public void addCommand(String name, Runnable callback) {
		callbacks.put(requireNonNull(name), callback);
	}

	public List<String> findCommands(String findText) {
		return callbacks.keySet().stream()
			.filter(name -> name.toLowerCase().contains(findText.toLowerCase()))
			.collect(Collectors.toList());
	}

	public void runCommand(String name) {
		callbacks.get(name).run();
	}
}