package workbook.editor.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Paint;
import java.util.Date;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.jdom2.Element;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;

import workbook.script.ScriptController;
import workbook.view.TabbedView;

class Chart {
	private final ChartPanel chartPanel = new ChartPanel(null);
	
	private ChartType type = ChartType.XYLINE;
	private CategoryDataset categoryDataset = new DefaultCategoryDataset();
	private XYDataset xyDataset = new DefaultXYDataset();
	private XYDataset timeDataset = new TimeSeriesCollection();
	
	private enum ChartType {
		BAR("Bar Chart"),
		STACKEDBAR("Stacked Bar Chart"),
		LINE("Line Chart"),
		STACKEDAREA("Stacked Area Chart"),
		SCATTER("Scatter Plot"),
		XYLINE("XY Line Chart"),
		TIMECHART("Time Chart");
		
		private String label;
		
		private ChartType(String label) {
			this.label = label;
		}
		
		public String getLabel() {
			return label;
		}
	}
	
	public Chart() {
		refreshChart();
	}
	
	private JFreeChart createChart(ChartType type) {
		switch(type) {
			case BAR:
				return ChartFactory.createBarChart(null, null, null, categoryDataset);
			case STACKEDBAR:
				return ChartFactory.createStackedBarChart(null, null, null, categoryDataset);
			case LINE:
				return ChartFactory.createLineChart(null, null, null, categoryDataset);
			case STACKEDAREA:
				return ChartFactory.createStackedAreaChart(null, null, null, categoryDataset);
			case SCATTER:
				return ChartFactory.createScatterPlot(null, null, null, xyDataset);
			case XYLINE:
				return ChartFactory.createXYLineChart(null, null, null, xyDataset);
			case TIMECHART:
				return ChartFactory.createTimeSeriesChart(null, null, null, timeDataset);
			default:
				throw new IllegalArgumentException("Unknown type: " + type);
		}
	}
	
	private void refreshChart() {
		JFreeChart chart = createChart(type);
		
		Paint[] colors = new Paint[] {
  			parseColor("#1f77b4"),
  			parseColor("#ff7f0e"),
  			parseColor("#2ca02c"),
  			parseColor("#d62728"),
  			parseColor("#9467bd"),
  			parseColor("#8c564b"),
  			parseColor("#e377c2"),
  			parseColor("#7f7f7f"),
  			parseColor("#bcbd22"),
  			parseColor("#17becf")
  		};
		
		chartPanel.setChart(chart);
		chartPanel.setMouseWheelEnabled(true);
		
		JPopupMenu menu = new JPopupMenu();
		for(ChartType type:ChartType.values()) {
			JMenuItem item = new JMenuItem();
			item.setText(type.getLabel());
			item.addActionListener(e -> setType(type.name()));
			menu.add(item);
		}
		
		chartPanel.setPopupMenu(menu);
		
		chartPanel.addChartMouseListener(new ChartMouseListener() {
			public void chartMouseClicked(ChartMouseEvent event) {
				chartPanel.restoreAutoBounds();
			}

			public void chartMouseMoved(ChartMouseEvent event) {
			}
		});
		
		if(chart.getPlot() instanceof XYPlot) {
			chart.getXYPlot().setDomainPannable(true);
			chart.getXYPlot().setRangePannable(true);
			chart.getXYPlot().setBackgroundPaint(parseColor("#ffffff"));
			chart.getXYPlot().setRangeGridlinePaint(parseColor("#cccccc"));
			chart.getXYPlot().setDomainGridlinePaint(parseColor("#cccccc"));
			
			XYItemRenderer renderer = chart.getXYPlot().getRenderer();
			for(int i = 0; i < 20; i++) {
				renderer.setSeriesPaint(i, colors[i % colors.length]);
				renderer.setSeriesStroke(i, new BasicStroke(2.0f));
			}
		}
		
		if(chart.getPlot() instanceof CategoryPlot) {
			chart.getCategoryPlot().setRangePannable(true);
			chart.getCategoryPlot().setBackgroundPaint(parseColor("#ffffff"));
			chart.getCategoryPlot().setRangeGridlinePaint(parseColor("#cccccc"));
			chart.getCategoryPlot().setDomainGridlinePaint(parseColor("#cccccc"));
			
			if(chart.getCategoryPlot().getRenderer() instanceof BarRenderer) {
				((BarRenderer)chart.getCategoryPlot().getRenderer()).setBarPainter(new StandardBarPainter());
			}
			
			CategoryItemRenderer renderer = chart.getCategoryPlot().getRenderer();
			for(int i = 0; i < 20; i++) {
				renderer.setSeriesPaint(i, colors[i % colors.length]);
				renderer.setSeriesStroke(i, new BasicStroke(2.0f));
			}
		}
	}
	
	public String getChartType() {
		return type.name();
	}

	public void setType(String type) {
		for(ChartType chartType:ChartType.values()) {
			if(chartType.name().equals(type)) {
				this.type = chartType;
			}
		}
		refreshChart();
	}
	
	public void setData(Object data) {
		this.categoryDataset = createCategoryDataset(data);
		this.xyDataset = createXYDataset(data);
		this.timeDataset = createTimeDataset(data);
		refreshChart();
	}
	
	/**
	 * Creates an xy dataset from data of the forms:
	 * [[1, 2], [3, 4], [5, 6], ...]
	 * [series1: [[1, 2], [3, 4], [5, 6], ...], ...]
	 */
	private XYDataset createXYDataset(Object data) {
		
		XYSeriesCollection dataset = new XYSeriesCollection();
		
		if(data instanceof Iterable) {
			dataset.addSeries(createXYSeries("Series1", (Iterable<?>) data));
		} else if(data instanceof Map) {
			((Map<?, ?>) data).forEach((key, value) -> {
				if(key instanceof String && value instanceof Iterable) {
					dataset.addSeries(createXYSeries((String) key, (Iterable<?>) value));
				}
			});
		}
		
		return dataset;
	}

	/**
	 * Creates an xy series from data of the form:
	 * [[1, 2], [3, 4], [5, 6], ...]
	 */
	private XYSeries createXYSeries(String name, Iterable<?> data) {
		XYSeries series = new XYSeries(name);
		
		for(Object item:(Iterable<?>) data) {
			if(item instanceof Iterable) {
				Object[] items = Iterables.toArray((Iterable<?>) item, Object.class);
				if(items.length >= 2 && items[0] instanceof Number && items[1] instanceof Number) {
					series.add((Number) items[0], (Number) items[1]);
				}
			}
		}
		
		return series;
	}
	
	/**
	 * Creates an time dataset from data of the forms:
	 * [[2000/01/01, 2], [2000/01/03, 4], [2000/01/05, 6], ...]
	 * [series1: [[2000/01/01, 2], [2000/01/03, 4], [2000/01/05, 6], ...], ...]
	 */
	private TimeSeriesCollection createTimeDataset(Object data) {
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		
		if(data instanceof Iterable) {
			dataset.addSeries(createTimeSeries("Series1", (Iterable<?>) data));
		} else if(data instanceof Map) {
			((Map<?, ?>) data).forEach((key, value) -> {
				if(key instanceof String && value instanceof Iterable) {
					dataset.addSeries(createTimeSeries((String) key, (Iterable<?>) value));
				}
			});
		}
		
		return dataset;
	}

	/**
	 * Creates an time series from data of the form:
	 * [[2000/01/01, 2], [2000/01/03, 4], [2000/01/05, 6], ...]
	 */
	private TimeSeries createTimeSeries(String name, Iterable<?> data) {
		TimeSeries series = new TimeSeries(name);
		
		for(Object item:(Iterable<?>) data) {
			if(item instanceof Iterable) {
				Object[] items = Iterables.toArray((Iterable<?>) item, Object.class);
				if(items.length >= 2 && items[0] instanceof Date && items[1] instanceof Number) {
					series.add(new Minute((Date) items[0]), (Number) items[1]);
				}
			}
		}
		
		return series;
	}

	/**
	 * Creates a category dataset from data of the forms:
	 * [['column1', 10], ['column2', 20], ...]
	 * [['column1', 'row1', 10], ['column2', 'row2', 20], ...]
	 */
	private CategoryDataset createCategoryDataset(Object data) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		
		if(data instanceof Iterable) {
			for(Object item:(Iterable<?>) data) {
				if(item instanceof Iterable) {
					Object[] items = Iterables.toArray((Iterable<?>) item, Object.class);
					if(items.length == 2 && items[0] instanceof Comparable && items[1] instanceof Number) {
						dataset.addValue((Number) items[1], "Series1", (Comparable<?>) items[0]);
					} else if(items.length == 3 && items[0] instanceof Comparable && items[1] instanceof Comparable && items[2] instanceof Number) {
						dataset.addValue((Number) items[2], (Comparable<?>) items[1], (Comparable<?>) items[0]);
					}
				}
			}
		}
		
		return dataset;
	}
	
	private static Color parseColor(String hex) {
		if(!hex.matches("#[a-fA-F0-9]{6}")) throw new IllegalArgumentException("Invalid color: " + hex);

		return new Color(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16), Integer.parseInt(hex.substring(5, 7), 16));
	}

	public Container getContainer() {
		return chartPanel;
	}
}

/**
 * An editor that displays the value as a chart.
 */
public class ChartTabbedEditor extends Editor implements TabbedView {
	private final Composite composite;
	private final Chart chart;
	
	public ChartTabbedEditor(Composite parent, EventBus eventBus, ScriptController scriptController) {
		super(eventBus, scriptController);
		
		composite = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		composite.setLayout(new FillLayout());
		
		this.chart = new Chart();
		SWT_AWT.new_Frame(composite).add(chart.getContainer());
		
		eventBus.register(this);
		getControl().addDisposeListener(event -> eventBus.unregister(this));
	}
	
	public void setValue(Object value) {
		Display.getDefault().asyncExec(() -> {
			chart.setData(value);
		});
	}
	
	public void serialize(Element element) {
		super.serialize(element);
		
		Element chartType = new Element("ChartType");
		chartType.setText(chart.getChartType());
		element.addContent(chartType);
	}

	public void deserialize(Element element) {
		chart.setType(element.getChildText("ChartType"));
		
		super.deserialize(element);
	}
	
	public Control getControl() {
		return composite;
	}
}