package workbook.script;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class LineReaderTest {
	@Test
	public void test() {
		List<String> lines = new ArrayList<>();
		LineReader lineReader = new LineReader(line -> lines.add(line));
		
		PrintWriter printWriter = new PrintWriter(lineReader.getOutputStream());
		
		printWriter.println("abc");
		printWriter.println("def");
		printWriter.flush();
		
		assertEquals(Arrays.asList("abc", "def"), lines);
	}
}