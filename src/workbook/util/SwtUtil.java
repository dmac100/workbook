package workbook.util;

import java.util.function.Consumer;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class SwtUtil {
	public static void forEachDescendant(Composite parent, Consumer<Control> consumer) {
		for(Control child:parent.getChildren()) {
			consumer.accept(child);
			if(child instanceof Composite) {
				forEachDescendant((Composite) child, consumer);
			}
		}
	}
}