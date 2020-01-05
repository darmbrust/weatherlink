package net.sagebits.weatherlink.data;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

public class WeatherProperty extends SimpleObjectProperty<Object>
{
	private long timeStamp;
	WeatherProperty boundTo;

	public WeatherProperty()
	{
		timeStamp = 0;
	}
	
	public WeatherProperty(int initial)
	{
		this();
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
			boundTo = (WeatherProperty)newObservable;
			boundTo.addListener(new InvalidationListener()
			{
				@Override
				public void invalidated(Observable observable)
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
		if (boundTo == null || boundTo.asString().get().equals("-1"))
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
}
