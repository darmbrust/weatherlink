package net.sagebits.weatherlink.data.live;

import java.util.HashMap;
import java.util.Optional;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import net.sagebits.weatherlink.data.WeatherProperty;

/**
 * All attributes which are sent "Live" (every 2.5 seconds) by a WeatherLinkLive. Contains observables for all values.
 * The observables will update as data is available.
 * 
 * {@link ConditionsLive}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class ConditionsLive
{
	private final String lsid;
	private Character data_structure_type;
	private Character txid;

	private LongProperty ts = new SimpleLongProperty(0);

	private HashMap<LiveDataTypes, WeatherProperty> properties = new HashMap<>();

	protected ConditionsLive(String lsid, ObjectNode conditionData)
	{
		this.lsid = lsid;
		if (conditionData != null)
		{
			this.data_structure_type = Optional.ofNullable(conditionData.get("data_structure_type")).orElseThrow().asText().charAt(0);
			this.txid = Optional.ofNullable(conditionData.get("txid")).orElseThrow().asText().charAt(0);
		}

		for (LiveDataTypes ldt : LiveDataTypes.values())
		{
			properties.put(ldt, new WeatherProperty(ldt.name(), -100));
		}

	}

	protected void update(long timeStamp, ObjectNode conditionData)
	{
		String localLsid = Optional.ofNullable(conditionData.get("lsid")).orElseThrow().asText();
		if (lsid == null)
		if (!localLsid.equals(lsid))
		{
			throw new IllegalArgumentException("Wrong lsid value!");
		}
		char localdst = Optional.ofNullable(conditionData.get("data_structure_type")).orElseThrow().asText().charAt(0);
		if (data_structure_type == null)
		{
			data_structure_type = localdst;
		}
		else if (localdst != data_structure_type)
		{
			throw new IllegalArgumentException("Unexpected data_structure_type!");
		}
		char localtxid = Optional.ofNullable(conditionData.get("txid")).orElseThrow().asText().charAt(0);
		if (txid == null)
		{
			txid = localtxid;
		}
		else if (localtxid != txid)
		{
			throw new IllegalArgumentException("Unexpected txid!");
		}

		for (LiveDataTypes ldt : LiveDataTypes.values())
		{
			ldt.updateProperty(properties.get(ldt), conditionData, timeStamp);
		}

		Platform.runLater(() -> {
			ts.set(timeStamp);
		});

	}

	public String getLsid()
	{
		return lsid;
	}

	public char getData_structure_type()
	{
		return data_structure_type;
	}

	public char getTxid()
	{
		return txid;
	}

	public WeatherProperty getValue(LiveDataTypes ldt)
	{
		return properties.get(ldt);
	}
}
