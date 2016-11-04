package workbook.script;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Creates an OutputStream that calls a callback for each line that's written to it.
 */
public class LineReader {
	private final OutputStream outputStream;
	private final CompletableFuture<?> future;
	
	public LineReader(final Consumer<String> callback) {
		this.future = new CompletableFuture<Void>();
		
		this.outputStream = new OutputStream() {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			
			public void write(int b) throws IOException {
				if(b != '\r') {
					if(b == '\n') {
						callback.accept(new String(buffer.toByteArray()));
						buffer = new ByteArrayOutputStream();
					} else {
						buffer.write(b);
					}
				}
			}
			
			public void close() {
				future.complete(null);
			}
		};
	}
	
	/**
	 * Returns the OutputStream to write to, which will call the main callback for each line.
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}

	/**
	 * Waits until there are no more lines left - the OutputStream is closed and all lines have been
	 * passed to the callback.
	 */
	public void waitUntilDone() {
		try {
			future.get();
		} catch(InterruptedException | ExecutionException e) {
			throw new RuntimeException("Error waiting for stream", e);
		}
	}
} 