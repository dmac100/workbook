package workbook.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;

import workbook.model.Model;
import workbook.script.JavascriptEngine;
import workbook.script.ScriptController;

public class MainControllerTest {
	private MainController mainController;
	private ScriptController scriptController;
	
	@Before
	public void before() {
		mainController = new MainController(mock(EventBus.class), mock(Model.class));
		scriptController = mainController.getScriptController();
		mainController.registerEngine("Javascript", new JavascriptEngine());
		mainController.setEngine("Javascript");
	}
	
	@Test
	public void serializationRoundTrip() throws Exception {
		Element root = new Element("root");
		
		mainController.setVariable("x", 1);
		mainController.serialize(root);
		mainController.setVariable("x", 2);
		
		mainController.deserialize(root);
		Object result = scriptController.eval("x").get();
		assertEquals(1, result);
	}
	
	@Test
	public void serializationRoundTrip_keepSystem() throws Exception {
		mainController.setVariable("system", 3);
		
		Element root = new Element("root");
		mainController.serialize(root);
		mainController.deserialize(root);
		
		Object result = scriptController.eval("system").get();
		assertEquals(3, result);
	}
}
