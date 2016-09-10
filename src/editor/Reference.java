package editor;

import java.util.concurrent.CompletableFuture;

public interface Reference {
	public CompletableFuture<Object> set(Object value);
	public CompletableFuture<Object> get();
}
