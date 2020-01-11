package net.sagebits.weatherlink.gui.gapchart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

/**
 * 
 * {@link GapLineChart}
 *
 * Borrowed from https://gist.github.com/sirolf2009/ae8a7897b57dcf902b4ed747b05641f9
 * 
 * @param <X>
 * @param <Y>
 */
public class GapLineChart<X, Y> extends LineChart<X, Y>
{

	public GapLineChart(Axis<X> xAxis, Axis<Y> yAxis)
	{
		super(xAxis, yAxis);
	}

	public GapLineChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data)
	{
		super(xAxis, yAxis, data);
	}

	@Override
	protected void layoutPlotChildren()
	{
		List<PathElement> constructedPath = new ArrayList<>(getData().size());
		for (int seriesIndex = 0; seriesIndex < getData().size(); seriesIndex++)
		{
			Series<X, Y> series = getData().get(seriesIndex);
			if (series.getNode() instanceof Path)
			{
				ObservableList<PathElement> seriesLine = ((Path) series.getNode()).getElements();
				seriesLine.clear();
				constructedPath.clear();
				MoveTo nextMoveTo = null;
				for (Iterator<Data<X, Y>> it = getDisplayedDataIterator(series); it.hasNext();)
				{
					Data<X, Y> item = it.next();
					double x = getXAxis().getDisplayPosition(item.getXValue());
					double y = getYAxis().getDisplayPosition(getYAxis().toRealValue(getYAxis().toNumericValue(item.getYValue())));
					if ((Double.isNaN(x) || Double.isNaN(y)) 
							&& series.getData().size() > (series.getData().indexOf(item) + 1))
					{
						Data<X, Y> next = series.getData().get(series.getData().indexOf(item) + 1);
						double nextX = getXAxis().getDisplayPosition(next.getXValue());
						double nextY = getYAxis().getDisplayPosition(getYAxis().toRealValue(getYAxis().toNumericValue(next.getYValue())));
						constructedPath.add(new MoveTo(nextX, nextY));
					}
					else
					{
						if (nextMoveTo != null)
						{
							constructedPath.add(nextMoveTo);
							nextMoveTo = null;
						}
						constructedPath.add(new LineTo(x, y));
						Node symbol = item.getNode();
						if (symbol != null)
						{
							double w = symbol.prefWidth(-1);
							double h = symbol.prefHeight(-1);
							symbol.resizeRelocate(x - (w / 2), y - (h / 2), w, h);
						}
					}
				}

				if (!constructedPath.isEmpty())
				{
					PathElement first = constructedPath.get(0);
					seriesLine.add(new MoveTo(getX(first), getY(first)));
					seriesLine.addAll(constructedPath);
				}
			}
		}
	}

	@Override
	protected void updateAxisRange()
	{
		final Axis<X> xa = getXAxis();
		final Axis<Y> ya = getYAxis();
		List<X> xData = null;
		List<Y> yData = null;
		if (xa.isAutoRanging())
			xData = new ArrayList<X>();
		if (ya.isAutoRanging())
			yData = new ArrayList<Y>();
		if (xData != null || yData != null)
		{
			for (Series<X, Y> series : getData())
			{
				for (Data<X, Y> data : series.getData())
				{
					if (xData != null && (data.getXValue() instanceof Number && !Double.isNaN(((Number) data.getXValue()).doubleValue())))
					{
						xData.add(data.getXValue());
					}
					if (yData != null && (data.getYValue() instanceof Number && !Double.isNaN(((Number) data.getYValue()).doubleValue())))
					{
						yData.add(data.getYValue());
					}
				}
			}
			if (xData != null)
				xa.invalidateRange(xData);
			if (yData != null)
				ya.invalidateRange(yData);
		}
	}

	public double getX(PathElement element)
	{
		if (element instanceof LineTo)
		{
			return getX((LineTo) element);
		}
		else if (element instanceof MoveTo)
		{
			return getX((MoveTo) element);
		}
		else
		{
			throw new IllegalArgumentException(element + " is not a valid type");
		}
	}

	public double getX(LineTo element)
	{
		return element.getX();
	}

	public double getX(MoveTo element)
	{
		return element.getX();
	}

	public double getY(PathElement element)
	{
		if (element instanceof LineTo)
		{
			return getY((LineTo) element);
		}
		else if (element instanceof MoveTo)
		{
			return getY((MoveTo) element);
		}
		else
		{
			throw new IllegalArgumentException(element + " is not a valid type");
		}
	}

	public double getY(LineTo element)
	{
		return element.getY();
	}

	public double getY(MoveTo element)
	{
		return element.getY();
	}

}
