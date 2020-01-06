package net.sagebits.weatherlink.data;

import java.util.Objects;
import org.apache.commons.math3.util.Precision;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class WeatherProperty extends SimpleObjectProperty<Object>
{
	private long timeStamp;
	WeatherProperty boundTo;

	public WeatherProperty(String name)
	{
		super(null, name);
		timeStamp = 0;
	}

	public WeatherProperty(String name, int initial)
	{
		this(name);
		setValue(initial);
	}

	public void setTimeStamp(long timeStamp)
	{
		this.timeStamp = timeStamp;
	}

	/**
	 * Will return the bound timestamp, if bound
	 */
	public long getTimeStamp()
	{
		return boundTo == null ? timeStamp : boundTo.getTimeStamp();
	}

	public long getLocalTimeStamp()
	{
		return timeStamp;
	}

	/*
	 * I'm not calling the super.bind or unbind, nor am I overriding isBound, as I want to hide this binding from the end users.
	 */

	@Override
	public void bind(ObservableValue<? extends Object> newObservable)
	{
		if (newObservable instanceof WeatherProperty)
		{
			boundTo = (WeatherProperty) newObservable;
			boundTo.addListener(new ChangeListener<>()
			{
				@Override
				public void changed(ObservableValue<?> observable, Object oldValue, Object newValue)
				{
					fireValueChangedEvent();
				}
			});
		}
		else
		{
			throw new RuntimeException("Can only bind to my type");
		}
	}

	@Override
	public void unbind()
	{
		boundTo = null;
	}

	@Override
	public Object get()
	{
		if (boundTo == null || boundTo.asDouble().get() == -100.0)
		{
			return super.get();
		}
		if ((this.timeStamp - 8000) > boundTo.timeStamp)
		{
			//We don't seem to be getting live updates, return the newer stored data. 
			return super.get();
		}
		return boundTo.get();
	}
	
	@Override
	public void set(Object newValue)
	{
		if (newValue == null)
		{
			System.err.println("Setting a null??? " + this.getName());
		}
		if (!Objects.equals(super.get(), newValue))
		{
			super.set(newValue);
		}
	}
	
	private DoubleBinding db;

	public DoubleBinding asDouble()
	{
		if (db == null)
		{
			db = new DoubleBinding()
			{
				{
					super.bind(WeatherProperty.this);
				}
	
				@Override
				public void dispose()
				{
					super.unbind(WeatherProperty.this);
				}
	
				@Override
				protected double computeValue()
				{
					final Object value = WeatherProperty.this.get();
					if (value == null)
					{
						System.err.println("Why is value null for " + WeatherProperty.this.getName());
						new Exception().printStackTrace();
					}
					return (value == null) ? -100 : Precision.round(((Number)value).doubleValue(), 1);
				}
	
				@Override
				public ObservableList<ObservableValue<?>> getDependencies()
				{
					return FXCollections.<ObservableValue<?>> singletonObservableList(WeatherProperty.this);
				}
			};
		}
		return db;
	}

}
