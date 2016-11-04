package workbook.script;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Locale;

/**
 * Splits a PrintStream so that one thread goes to one stream, and the rest go to another.
 */
public class PrintStreamSplitter extends PrintStream {
	private final Thread thread;
	private final PrintStream currentThreadStream;
	private final PrintStream otherThreadStream;
	
	public PrintStreamSplitter(Thread thread, PrintStream currentThreadStream, PrintStream otherThreadStream) {
		super(new ByteArrayOutputStream());
		
		this.thread = thread;
		this.currentThreadStream = currentThreadStream;
		this.otherThreadStream = otherThreadStream;
	}
	
	/**
	 * Returns the stream for the current thread.
	 */
	private PrintStream getPrintStream() {
		if(Thread.currentThread().equals(thread)) {
			return currentThreadStream;
		} else {
			return otherThreadStream;
		}
	}
	
	public PrintStream append(char c) {
		return getPrintStream().append(c);
	}
	
	public PrintStream append(CharSequence csq) {
		return getPrintStream().append(csq);
	}
	
	public PrintStream append(CharSequence csq, int start, int end) {
		return getPrintStream().append(csq, start, end);
	}
	
	public boolean checkError() {
		return getPrintStream().checkError();
	}
	
	public void close() {
		getPrintStream().close();
	}
	
	public void flush() {
		getPrintStream().flush();
	}
	
	public PrintStream format(Locale l, String format, Object... args) {
		return getPrintStream().format(l, format, args);
	}
	
	public PrintStream format(String format, Object... args) {
		return getPrintStream().format(format, args);
	}
	
	public void print(boolean b) {
		getPrintStream().print(b);
	}
	
	public void print(char c) {
		getPrintStream().print(c);
	}
	
	public void print(char[] s) {
		getPrintStream().print(s);
	}
	
	public void print(double d) {
		getPrintStream().print(d);
	}
	
	public void print(float f) {
		getPrintStream().print(f);
	}
	
	public void print(int i) {
		getPrintStream().print(i);
	}
	
	public void print(long l) {
		getPrintStream().print(l);
	}
	
	public void print(Object obj) {
		getPrintStream().print(obj);
	}
	
	public void print(String s) {
		getPrintStream().print(s);
	}
	
	public PrintStream printf(Locale l, String format, Object... args) {
		return getPrintStream().printf(l, format, args);
	}
	
	public PrintStream printf(String format, Object... args) {
		return getPrintStream().printf(format, args);
	}
	
	public void println() {
		getPrintStream().println();
	}
	
	public void println(boolean x) {
		getPrintStream().println(x);
	}
	
	public void println(char x) {
		getPrintStream().println(x);
	}
	
	public void println(char[] x) {
		getPrintStream().println(x);
	}
	
	public void println(double x) {
		getPrintStream().println(x);
	}
	
	public void println(float x) {
		getPrintStream().println(x);
	}
	
	public void println(int x) {
		getPrintStream().println(x);
	}
	
	public void println(long x) {
		getPrintStream().println(x);
	}
	
	public void println(Object x) {
		getPrintStream().println(x);
	}
	
	public void println(String x) {
		getPrintStream().println(x);
	}
	
	public void write(byte[] buf, int off, int len) {
		getPrintStream().write(buf, off, len);
	}
	
	public void write(int b) {
		getPrintStream().println(b);
	}
}