package workbook.view;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public class MenuBuilder {
	private Shell shell;
	private Menu menubar;
	private Menu menu;
	private MenuItem item;
	
	public MenuBuilder(Shell shell) {
		this.shell = shell;
		this.menubar = new Menu(shell, SWT.BAR);
	}
	
	private MenuBuilder(Menu menu) {
		this.menu = menu;
	}
	
	public MenuBuilder addMenu(String name) {
		MenuItem item = new MenuItem(menubar, SWT.MENU);
		this.menu = new Menu(item);
		item.setText(name);
		item.setMenu(menu);
		
		return this;
	}
	
	public MenuBuilder addItem(String name) {
		if(menu == null) throw new IllegalStateException("No menu");
		
		this.item = new MenuItem(menu, SWT.NONE);
		item.setText(name);
		
		return this;
	}
	
	public MenuBuilder addSubmenu(String name, Consumer<MenuBuilder> consumer) {
		this.item = new MenuItem(menu, SWT.CASCADE);
		item.setText(name);
		
		Menu submenu = new Menu(item);
		item.setMenu(submenu);
		
		consumer.accept(new MenuBuilder(submenu));
		
		return this;
	}
	
	public MenuBuilder setEnabled(boolean enabled) {
		if(menu == null) throw new IllegalStateException("No menu");
		
		item.setEnabled(enabled);
		
		return this;
	}
	
	public MenuBuilder addSeparator() {
		if(menu == null) throw new IllegalStateException("No menu");
		
		this.item = new MenuItem(menu, SWT.SEPARATOR);
		
		return this;
	}
	
	public MenuBuilder addSelectionListener(SelectionListener listener) {
		if(item == null) throw new IllegalStateException("No menuitem");
		
		item.addSelectionListener(listener);
		
		return this;
	}
	
	public MenuBuilder addSelectionListener(Runnable callback) {
		return addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				callback.run();
			}
		});
	}
	
	public MenuBuilder setAccelerator(int a) {
		if(item == null) throw new IllegalStateException("No menuitem");
		
		item.setAccelerator(a);
		
		return this;
	}
	
	public void build() {
		if(shell != null) {
			Menu oldMenu = shell.getMenuBar();
			shell.setMenuBar(menubar);
			if(oldMenu != null) {
				oldMenu.dispose();
			}
		}
	}
}