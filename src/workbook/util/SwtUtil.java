package workbook.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class SwtUtil {
	public static void forEachDescendant(Composite parent, Consumer<Control> consumer) {
		for(Control child:parent.getChildren()) {
			consumer.accept(child);
			if(child instanceof Composite) {
				forEachDescendant((Composite) child, consumer);
			}
		}
	}
	
	/**
	 * Runs a callable on the display thread, and returns the result.
	 */
	public static <T> T execDisplay(Callable<T> callable) {
		try {
			CompletableFuture<T> future = new CompletableFuture<>();
			Display.getDefault().asyncExec(() -> {
				try {
					future.complete(callable.call());
				} catch(Exception e) {
					future.completeExceptionally(e);
				}
			});
			return future.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Error executing on display thread", e);
		}
	}
	
	/**
	 * Returns whether parent is an ancestor of child.
	 */
	public static boolean isAncestor(Control parent, Control child) {
		if(child == null) {
			return false;
		}
		if(child == parent) {
			return true;
		}
		return isAncestor(parent, child.getParent());
	}
}