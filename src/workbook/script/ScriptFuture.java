package workbook.script;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * A future created to allow asynchronous communication with the script engine.
 * Contains the default exception handlers, and runs callbacks on the script thread.
 */
public class ScriptFuture<T> {
	private final CompletableFuture<T> future = new CompletableFuture<>();
	private final ScriptController scriptController;
	
	public ScriptFuture(ScriptController scriptController) {
		this.scriptController = scriptController;
	}
	
	public void complete(T result) {
		future.complete(result);
		future.exceptionally(this::exceptionHandler);
	}
	
	public void completeExceptionally(Throwable throwable) {
		future.completeExceptionally(throwable);
	}

	public T get() throws InterruptedException, ExecutionException {
		return future.get();
	}

	public void thenAccept(Consumer<T> callback) {
		future
			.thenAccept(runOnScriptThread(callback))
			.exceptionally(this::exceptionHandler);
	}

	public void thenRun(Runnable callback) {
		future
			.thenRun(runOnScriptThread(callback))
			.exceptionally(this::exceptionHandler);
	}
	
	public void thenRunAlways(Runnable callback) {
		future
			.thenRun(runOnScriptThread(callback))
			.exceptionally(e -> {
				exceptionHandler(e);
				callback.run();
				return null;
			})
			.exceptionally(this::exceptionHandler);
	}
	
	private Consumer<T> runOnScriptThread(Consumer<T> callback) {
		return value -> {
			scriptController.exec(() -> {
				callback.accept(value);
				return null;
			});
		};
	}
	
	private Runnable runOnScriptThread(Runnable callback) {
		return () -> {
			scriptController.exec(() -> {
				callback.run();
				return null;
			});
		};
	}

	private <V> V exceptionHandler(Throwable e) {
		e.printStackTrace();
		return null;
	}
}
