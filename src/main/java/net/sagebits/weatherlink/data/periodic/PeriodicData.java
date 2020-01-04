package net.sagebits.weatherlink.data.periodic;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sagebits.weatherlink.data.StoredDataTypes;

/**
 * Instance class to interface with the DB where we stuff each periodic read of the full data set.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class PeriodicData
{
	private volatile static PeriodicData instance_;
	private final Logger log = LogManager.getLogger();
	
	
	private final Connection db;
	
	//singleton
	private PeriodicData() throws SQLException
	{
		File temp = new File("weatherLinkData");
		
		db = DriverManager.getConnection("jdbc:h2:" + temp.getAbsolutePath(), "sa", "");
		
		//One table for each of the data structures it returns.  
		db.prepareStatement("CREATE TABLE IF NOT EXISTS iss (" 
				+ StoredDataTypes.did.colCreate(false, true)
				+ StoredDataTypes.lsid.colCreate(false, true)
				+ StoredDataTypes.ts.colCreate(false, true) 
				+ StoredDataTypes.txid.colCreate(false, true)
				+ StoredDataTypes.temp.colCreate() 
				+ StoredDataTypes.hum.colCreate()
				+ StoredDataTypes.dew_point.colCreate()
				+ StoredDataTypes.wet_bulb.colCreate()
				+ StoredDataTypes.heat_index.colCreate()
				+ StoredDataTypes.wind_chill.colCreate()
				+ StoredDataTypes.thw_index.colCreate()
				+ StoredDataTypes.thsw_index.colCreate()
				+ StoredDataTypes.wind_speed_last.colCreate()
				+ StoredDataTypes.wind_dir_last.colCreate() 
				+ StoredDataTypes.wind_speed_avg_last_1_min.colCreate() 
				+ StoredDataTypes.wind_dir_scalar_avg_last_1_min.colCreate()
				+ StoredDataTypes.wind_speed_avg_last_2_min.colCreate()
				+ StoredDataTypes.wind_dir_scalar_avg_last_2_min.colCreate()
				+ StoredDataTypes.wind_speed_hi_last_2_min.colCreate()
				+ StoredDataTypes.wind_dir_at_hi_speed_last_2_min.colCreate()
				+ StoredDataTypes.wind_speed_avg_last_10_min.colCreate()
				+ StoredDataTypes.wind_dir_scalar_avg_last_10_min.colCreate()
				+ StoredDataTypes.wind_speed_hi_last_10_min.colCreate()
				+ StoredDataTypes.wind_dir_at_hi_speed_last_10_min.colCreate()
				+ StoredDataTypes.rain_size.colCreate()
				+ StoredDataTypes.rain_rate_last.colCreate()
				+ StoredDataTypes.rain_rate_hi.colCreate()
				+ StoredDataTypes.rainfall_last_15_min.colCreate()
				+ StoredDataTypes.rain_rate_hi_last_15_min.colCreate()
				+ StoredDataTypes.rainfall_last_60_min.colCreate()
				+ StoredDataTypes.rainfall_last_24_hr.colCreate()
				+ StoredDataTypes.rain_storm.colCreate()
				+ StoredDataTypes.rain_storm_start_at.colCreate()
				+ StoredDataTypes.solar_rad.colCreate()
				+ StoredDataTypes.uv_index.colCreate()
				+ StoredDataTypes.rx_state.colCreate()
				+ StoredDataTypes.trans_battery_flag.colCreate()
				+ StoredDataTypes.rainfall_daily.colCreate()
				+ StoredDataTypes.rainfall_monthly.colCreate()
				+ StoredDataTypes.rainfall_year.colCreate()
				+ StoredDataTypes.rain_storm_last.colCreate()
				+ StoredDataTypes.rain_storm_last_start_at.colCreate()
				+ StoredDataTypes.rain_storm_last_end_at.colCreate(true)
				+ ")").execute();
		db.prepareStatement("CREATE INDEX IF NOT EXISTS iss_index ON iss (did, lsid, ts)").execute();
		db.prepareStatement("CREATE TABLE IF NOT EXISTS soil (" 
				+ StoredDataTypes.did.colCreate(false, true)
				+ StoredDataTypes.lsid.colCreate(false, true)
				+ StoredDataTypes.ts.colCreate(false, true) 
				+ StoredDataTypes.txid.colCreate(false, true) 
				+ StoredDataTypes.temp_1.colCreate()
				+ StoredDataTypes.temp_2.colCreate()
				+ StoredDataTypes.temp_3.colCreate()
				+ StoredDataTypes.temp_4.colCreate()
				+ StoredDataTypes.most_soil_1.colCreate()
				+ StoredDataTypes.most_soil_2.colCreate()
				+ StoredDataTypes.most_soil_3.colCreate()
				+ StoredDataTypes.most_soil_4.colCreate()
				+ StoredDataTypes.wet_leaf_1.colCreate()
				+ StoredDataTypes.wet_leaf_2.colCreate()
				+ StoredDataTypes.rx_state.colCreate()
				+ StoredDataTypes.trans_battery_flag.colCreate(true)
				+ ")").execute();
		db.prepareStatement("CREATE INDEX IF NOT EXISTS soil_index ON soil (did, lsid, ts)").execute();
		db.prepareStatement("CREATE TABLE IF NOT EXISTS wll_env ("
				+ StoredDataTypes.did.colCreate(false, true)
				+ StoredDataTypes.lsid.colCreate(false, true)
				+ StoredDataTypes.ts.colCreate(false, true) 
				+ StoredDataTypes.temp_in.colCreate()
				+ StoredDataTypes.hum_in.colCreate()
				+ StoredDataTypes.dew_point_in.colCreate()
				+ StoredDataTypes.heat_index_in.colCreate(true)
				+ ")").execute();
		db.prepareStatement("CREATE INDEX IF NOT EXISTS wll_env_index ON wll_env (did, lsid, ts)").execute();
		db.prepareStatement("CREATE TABLE IF NOT EXISTS wll_bar (" 
				+ StoredDataTypes.did.colCreate(false, true)
				+ StoredDataTypes.lsid.colCreate(false, true)
				+ StoredDataTypes.ts.colCreate(false, true) 
				+ StoredDataTypes.bar_sea_level.colCreate()
				+ StoredDataTypes.bar_trend.colCreate()
				+ StoredDataTypes.bar_absolute.colCreate(true)
				+ ")").execute();
		db.prepareStatement("CREATE INDEX IF NOT EXISTS wll_bar_index ON wll_bar (did, lsid, ts)").execute();
	}
	
	public static PeriodicData getInstance() throws SQLException
	{
		if (instance_ == null)
		{
			synchronized (PeriodicData.class)
			{
				if (instance_ == null)
				{
					instance_ = new PeriodicData();
				}
			}
		}
		return instance_;
	}
	
	
	public void append(JsonNode data) throws SQLException
	{
		String did = Optional.ofNullable(data.get("did")).orElseThrow().asText();
		final long ts = Long.parseLong(Optional.ofNullable(data.get("ts")).orElseThrow().asText()) * 1000;
		handleConditions((ArrayNode) data.get("conditions"), ts, did);
	}
	
	private void handleConditions(ArrayNode an, long timeStamp, String did) throws SQLException
	{
		Iterator<JsonNode> it = an.elements();
		while (it.hasNext())
		{
			ObjectNode on = (ObjectNode) it.next();
			String dst = on.get("data_structure_type").asText();
			if (dst.equals("1"))
			{
				appendTable("iss", on, timeStamp, did);
			}
			else if (dst.equals("2"))
			{
				appendTable("soil", on, timeStamp, did);
			}
			else if (dst.equals("3"))
			{
				appendTable("wll_bar", on, timeStamp, did);
			}
			else if (dst.equals("4"))
			{
				appendTable("wll_env", on, timeStamp, did);
			}
			else
			{
				log.error("Unexpected data_structure_type {}", dst);
			}
		}
	}
	
	private void appendTable(String tableName, ObjectNode on, long timeStamp, String did) throws SQLException
	{
		long time = System.currentTimeMillis();
		StringBuilder colNames = new StringBuilder();
		StringBuilder valuePlaceholders = new StringBuilder();
		Iterator<Entry<String, JsonNode>> f = on.fields();
		List<Entry<String, JsonNode>> valuesToSet = new ArrayList<>(20);
		colNames.append("ts,did,");
		valuePlaceholders.append("?,?,");
		while (f.hasNext())
		{
			Entry<String, JsonNode> e = f.next();
			if (e.getValue().asText().equals("null"))
			{
				continue;
			}
			
			String name = e.getKey();
			if (name.equals("data_structure_type"))
			{
				continue;
			}
			colNames.append(name);
			colNames.append(",");
			valuePlaceholders.append("?,");
			valuesToSet.add(e);
		}
		colNames.setLength(colNames.length() - 1);
		valuePlaceholders.setLength(valuePlaceholders.length() - 1);
		if (db.isClosed())
		{
			log.warn("Still processing data after shutdown?  Tossing");
			return;
		}
		PreparedStatement ps = db.prepareStatement("INSERT INTO " + tableName + " (" + colNames.toString() + ") VALUES (" + valuePlaceholders.toString() + ")");
		int i = 1;
		ps.setLong(i++, timeStamp);
		ps.setString(i++, did);
		for (Entry<String, JsonNode> jn : valuesToSet)
		{
			StoredDataTypes.match(jn.getKey()).intoPreparedStatement(jn.getValue(), i++, ps);
		}
		ps.execute();
		log.debug("Appended table {} in {}ms",  tableName, (System.currentTimeMillis() - time));
	}

	public void shutDown() throws SQLException
	{
		db.close();
	}
}
