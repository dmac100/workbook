package workbook.editor.ui;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.google.common.eventbus.EventBus;

import workbook.script.ScriptController;
import workbook.view.FontList;
import workbook.view.TabbedView;

class HexView {
	private final Composite parent;
	private final StyledText styledText1;
	private final StyledText styledText2;
	private final StyledText styledText3;
	
	private byte[] data;
	
	private Consumer<byte[]> selectedCallback;
	
	public HexView(Composite parent) {
		this.parent = parent;
		
		Font font = FontList.MONO_NORMAL;
		
		GridLayout gridLayout = new GridLayout(3, false);
		gridLayout.marginHeight = 2;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = 5;
		gridLayout.verticalSpacing = 0;
		
		parent.setLayout(gridLayout);
		
		GridData gridData1 = new GridData(SWT.FILL, SWT.FILL, false, true);
		GridData gridData2 = new GridData(SWT.FILL, SWT.FILL, false, true);
		GridData gridData3 = new GridData(SWT.FILL, SWT.FILL, false, true);
		
		styledText1 = new StyledText(parent, SWT.BORDER);
		styledText1.setFont(font);
		styledText1.setLayoutData(gridData1);
		styledText1.setEditable(false);
		styledText1.setMargins(3, 3, 3, 3);
		
		styledText2 = new StyledText(parent, SWT.BORDER);
		styledText2.setFont(font);
		styledText2.setLayoutData(gridData2);
		styledText2.setEditable(false);
		styledText2.setMargins(3, 3, 3, 3);
		
		styledText3 = new StyledText(parent, SWT.BORDER | SWT.V_SCROLL);
		styledText3.setFont(font);
		styledText3.setLayoutData(gridData3);
		styledText3.setEditable(false);
		styledText3.setMargins(3, 3, 3, 3);
		
		styledText2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				int x = middlePaneToByteOffset(event.x);
				int y = middlePaneToByteOffset(event.y);
				
				styledText2.setSelection(byteOffsetToMiddlePane(x), byteOffsetToMiddlePane(y));
				styledText3.setSelection(byteOffsetToRightPane(x), byteOffsetToRightPane(y));
				
				onSelected(x, y);
			}
		});
		
		styledText3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				int x = rightPaneToByteOffset(event.x);
				int y = rightPaneToByteOffset(event.y);

				styledText2.setSelection(byteOffsetToMiddlePane(x), byteOffsetToMiddlePane(y));
				
				onSelected(x, y);
			}
		});
		
		styledText1.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(MouseEvent event) {
				styledText3.setTopIndex(styledText3.getTopIndex() - event.count);
				
				styledText1.setTopIndex(styledText3.getTopIndex());
				styledText2.setTopIndex(styledText3.getTopIndex());
			}
		});
		
		styledText2.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(MouseEvent event) {
				styledText3.setTopIndex(styledText3.getTopIndex() - event.count);
				
				styledText1.setTopIndex(styledText3.getTopIndex());
				styledText2.setTopIndex(styledText3.getTopIndex());
			}
		});

		styledText3.getVerticalBar().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				styledText1.setTopIndex(styledText3.getTopIndex());
				styledText2.setTopIndex(styledText3.getTopIndex());
			}
		});
	}
	
	public void setSelectedCallback(Consumer<byte[]> callback) {
		this.selectedCallback = callback;
	}
	
	private static int middlePaneToByteOffset(int x) {
		return (x - (x / (16 * 3 + 1)) + 1) / 3;
	}
	
	private static int byteOffsetToRightPane(int x) {
		return (x / 16) + x;
	}
	
	private static int rightPaneToByteOffset(int x) {
		return x - x / (16 + 1);
	}
	
	private static int byteOffsetToMiddlePane(int x) {
		return (x / 16) + (x * 3);
	}
	
	private void onSelected(int x, int y) {
		if(selectedCallback != null) {
			if(y - x <= 8) {
				byte[] s = new byte[y - x];
				if(y <= data.length) {
					System.arraycopy(data, x, s, 0, s.length);
					selectedCallback.accept(s);
				}
			}
		}
	}
	
	private void refreshTextValues() {
		StringBuilder s1 = new StringBuilder();
		StringBuilder s2 = new StringBuilder();
		for(int x = 0; x < data.length; x += 16) {
			s1.append(padZeroLeft(Integer.toHexString(x), 8));
			for(int y = x; y < x + 16 && y < data.length; y++) {
				int c = (int) data[y];
				s2.append(padZeroLeft(Integer.toHexString(c & 0xFF), 2) + " ");
			}
			s1.append("\n");
			s2.append("\n");
		}
		styledText1.setText(s1.toString());
		styledText2.setText(s2.toString());
		
		styledText1.getParent().layout();
	}
	
	public static String padZeroLeft(String text, int length) {
		StringBuilder s = new StringBuilder();
		for(int x = text.length(); x < length; x++) {
			s.append('0');
		}
		s.append(text);
		return s.toString();
	}

	public void setData(byte[] data) {
		this.data = data;
		
		StringBuilder s = new StringBuilder();
		for(int x = 0; x < data.length; x += 16) {
			for(int y = x; y < x + 16 && y < data.length; y++) {
				char c = (char) data[y];
				s.append((c >= 32 && c <= 126) ? c : '.');
			}
			s.append("\n");
		}
		styledText3.setText(s.toString());
		
		refreshTextValues();
	}
}

/**
 * A hex editor that shows the bytes within a byte array.
 */
public class HexTabbedEditor extends Editor implements TabbedView {
	private final Composite parent;
	private final EventBus eventBus;
	private final HexView hexView;
	private final SashForm sashForm;
	
	private final Text littleEndianSigned;
	private final Text bigEndianSigned;
	private final Text littleEndianUnsigned;
	private final Text bigEndianUnsigned;
	private final Text littleEndianFloat;
	private final Text bigEndianFloat;
	private final Text littleEndianBinary;
	private final Text bigEndianBinary;
	private final Text littleEndianHex;
	private final Text bigEndianHex;

	public HexTabbedEditor(Composite parent, EventBus eventBus, ScriptController scriptController) {
		super(eventBus, scriptController);
		
		this.parent = parent;
		this.eventBus = eventBus;
		
		parent.setLayout(new FillLayout());
		
		this.sashForm = new SashForm(parent, SWT.VERTICAL);
		Composite top = new Composite(sashForm, SWT.NONE);
		Composite bottom = new Composite(sashForm, SWT.NONE);
		
		registerEvents();
		
		this.hexView = new HexView(top);
		hexView.setSelectedCallback(this::onSelected);
		
		bottom.setLayout(new GridLayout(4, false));
		
		littleEndianSigned = addFormItem(bottom, "Little-Endian Signed");
		bigEndianSigned = addFormItem(bottom, "Big-Endian Signed");
		littleEndianUnsigned = addFormItem(bottom, "Little-Endian Unsigned");
		bigEndianUnsigned = addFormItem(bottom, "Big-Endian Unsigned");
		littleEndianFloat = addFormItem(bottom, "Little-Endian Float");
		bigEndianFloat = addFormItem(bottom, "Big-Endian Float");
		littleEndianBinary = addFormItem(bottom, "Little-Endian Binary");
		bigEndianBinary = addFormItem(bottom, "Big-Endian Binary");
		littleEndianHex = addFormItem(bottom, "Little-Endian Hex");
		bigEndianHex = addFormItem(bottom, "Big-Endian Hex");
	}
	
	private Text addFormItem(Composite parent, String labelText) {
		GridData gridData = new GridData();
		gridData.widthHint = 130;
		
		Label label = new Label(parent, SWT.NONE);
		label.setText(labelText);
		Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(gridData);
		text.setEditable(false);
		return text;
	}

	private void onSelected(byte[] s) {
		long bigEndian = 0;
		long littleEndian = 0;
		
		for(int i = 0; i < s.length; i++) {
			bigEndian = (bigEndian << 8) + (s[i] & 0xFF);
		}
		
		for(int i = s.length - 1; i >= 0; i--) {
			littleEndian = (littleEndian << 8) + (s[i] & 0xFF);
		}

		if(s.length == 1) {
			littleEndianSigned.setText(String.valueOf((byte) littleEndian));
			bigEndianSigned.setText(String.valueOf((byte) bigEndian));
			littleEndianFloat.setText("");
			bigEndianFloat.setText("");
		} else if(s.length == 2) {
			littleEndianSigned.setText(String.valueOf((short) littleEndian));
			bigEndianSigned.setText(String.valueOf((short) bigEndian));
			littleEndianFloat.setText("");
			bigEndianFloat.setText("");
		} else if(s.length == 4) {
			littleEndianSigned.setText(String.valueOf((int) littleEndian));
			bigEndianSigned.setText(String.valueOf((int) bigEndian));
			littleEndianFloat.setText(String.valueOf(Float.intBitsToFloat((int) littleEndian)));
			bigEndianFloat.setText(String.valueOf(Float.intBitsToFloat((int) bigEndian)));
		} else if(s.length == 8) {
			littleEndianSigned.setText(String.valueOf((long) littleEndian));
			bigEndianSigned.setText(String.valueOf((long) bigEndian));
			littleEndianFloat.setText(String.valueOf(Double.longBitsToDouble((long) littleEndian)));
			littleEndianFloat.setText(String.valueOf(Double.longBitsToDouble((long) bigEndian)));
		} else {
			littleEndianSigned.setText("");
			bigEndianSigned.setText("");
			littleEndianFloat.setText("");
			bigEndianFloat.setText("");
		}

		littleEndianUnsigned.setText(Long.toUnsignedString(littleEndian));
		bigEndianUnsigned.setText(Long.toUnsignedString(bigEndian));
		littleEndianBinary.setText(HexView.padZeroLeft(Long.toBinaryString((long) littleEndian), s.length * 8));
		bigEndianBinary.setText(HexView.padZeroLeft(Long.toBinaryString((long) bigEndian), s.length * 8));
		littleEndianHex.setText("0x" + HexView.padZeroLeft(Long.toHexString((long) littleEndian), s.length * 2));
		bigEndianHex.setText("0x" + HexView.padZeroLeft(Long.toHexString((long) bigEndian), s.length * 2));
	}

	public Control getControl() {
		return sashForm;
	}

	public void setValue(Object value) {
		if(value instanceof byte[]) {
			Display.getDefault().asyncExec(() -> {
				if(!parent.isDisposed()) {
					hexView.setData((byte[]) value);
				}
			});
		}
	}
}