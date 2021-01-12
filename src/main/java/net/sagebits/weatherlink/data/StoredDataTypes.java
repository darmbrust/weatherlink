package net.sagebits.weatherlink.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import com.fasterxml.jackson.databind.JsonNode;
import net.sagebits.weatherlink.data.live.LiveDataTypes;

/**
 * Maps the json data from wll to an H2 sql database for storage.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public enum StoredDataTypes
{
	did("device id", "VARCHAR", true, StoredDataTables.ISS, StoredDataTables.SOIL, StoredDataTables.WLL_ENV, StoredDataTables.WLL_BAR),
	lsid("logical sensor id", "VARCHAR", true, StoredDataTables.ISS, StoredDataTables.SOIL, StoredDataTables.WLL_ENV, StoredDataTables.WLL_BAR),
	ts("timestamp", "BIGINT", true, StoredDataTables.ISS, StoredDataTables.SOIL, StoredDataTables.WLL_ENV, StoredDataTables.WLL_BAR),
	//TODO TXID is not handled proper in the SQL store nor in the live data - things will break if we receive data with more than 1 unique TXID
	txid("transmitter id", "TINYINT", true, StoredDataTables.ISS, StoredDataTables.SOIL),
	temp("temp", "REAL", "most recent valid temperature (°F)", StoredDataTables.ISS),
	hum("humidity", "REAL", "most recent valid humidity (%RH)", StoredDataTables.ISS),
	dew_point("dew point", "REAL", "most recent valid dew point (°F)", StoredDataTables.ISS),
	wet_bulb("web bulb", "REAL", "most recent valid web bulb (°F)", StoredDataTables.ISS),
	heat_index("heat index", "REAL", "most recent valid heat index (°F)", StoredDataTables.ISS),
	wind_chill("wind chill", "REAL", "most recent valid wind chill (°F)", StoredDataTables.ISS),
	thw_index("temp heat wind index", "REAL", "most recent temp heat wind index (°F)", StoredDataTables.ISS),
	thsw_index("temp heat sun wind index", "REAL", "most recent valid temp heat sun wind index(°F)", StoredDataTables.ISS),
	wind_speed_last("last wind speed", "REAL", "most recent valid wind speed (mph)", LiveDataTypes.wind_speed_last, StoredDataTables.ISS),
	wind_dir_last("last wind speed", "REAL", "most recent valid wind direction (°degree)", LiveDataTypes.wind_dir_last,StoredDataTables.ISS),
	wind_speed_avg_last_1_min("last wind speed", "REAL", "average wind speed over last 1 min (mph)", StoredDataTables.ISS),
	wind_dir_scalar_avg_last_1_min("last wind speed", "REAL", "scalar average wind direction over last 1 min (°degree)", StoredDataTables.ISS),
	wind_speed_avg_last_2_min("last wind speed", "REAL", "average wind speed over last 2 min (mph)", StoredDataTables.ISS),
	wind_dir_scalar_avg_last_2_min("last wind speed", "REAL", "scalar average wind direction over last 2 min (°degree)", StoredDataTables.ISS),
	wind_speed_hi_last_2_min("last wind speed", "REAL", "maximum wind speed over last 2 min (mph)", StoredDataTables.ISS),
	wind_dir_at_hi_speed_last_2_min("last wind speed", "REAL", "gust wind direction over last 2 min (°degree)", StoredDataTables.ISS),
	wind_speed_avg_last_10_min("last wind speed", "REAL", "average wind speed over last 10 min (mph)", StoredDataTables.ISS),
	wind_dir_scalar_avg_last_10_min("last wind speed", "REAL", "scalar average wind direction over last 10 min (°degree)", StoredDataTables.ISS),
	wind_speed_hi_last_10_min("last wind speed", "REAL", "maximum wind speed over last 10 min (mph)", LiveDataTypes.wind_speed_hi_last_10_min, StoredDataTables.ISS),
	wind_dir_at_hi_speed_last_10_min("last wind speed", "REAL", "gust wind direction over last 10 min (°degree)", LiveDataTypes.wind_dir_at_hi_speed_last_10_min, StoredDataTables.ISS),
	rain_size("rain collector type / size", "TINYINT", "0: Reserved, 1: 0.01\", 2: 0.2 mm, 3:  0.1 mm, 4: 0.001\"", LiveDataTypes.rain_size, StoredDataTables.ISS),
	rain_rate_last("rain rate", "REAL", "most recent rain rate per hour", LiveDataTypes.rain_rate_last, StoredDataTables.ISS),
	rain_rate_hi("rain rate hi last minute", "REAL", "highest rain rate over last 1 minute", StoredDataTables.ISS),
	rainfall_last_15_min("rain in last 15 min", "REAL", "total rain over last 15 min", LiveDataTypes.rain_15_min, StoredDataTables.ISS),
	rain_rate_hi_last_15_min("rain rate hi last 15 minutes", "REAL", "highest rain rate over last 15 minute ", StoredDataTables.ISS),
	rainfall_last_60_min("rain in last hour", "REAL", "total rain for last 60 min", LiveDataTypes.rain_60_min, StoredDataTables.ISS),
	rainfall_last_24_hr("rain in last 24 hours", "REAL", "total rain for last 24 hours", LiveDataTypes.rain_24_hr, StoredDataTables.ISS),
	rain_storm("rain storm total", "REAL", "total rain since last 24 hour break in rain", LiveDataTypes.rain_storm, StoredDataTables.ISS),  //current rain storm
	rain_storm_start_at("storm start time", "BIGINT", "current rainstorm start time", LiveDataTypes.rain_storm_start_at, StoredDataTables.ISS),
	solar_rad("solar radiation", "REAL", "most recent solar radiation (W/m²)", StoredDataTables.ISS),
	uv_index("uv index", "REAL", "most recent UV index", StoredDataTables.ISS),
	rx_state("receiver state", "TINYINT", "configured radio receiver state", StoredDataTables.ISS, StoredDataTables.SOIL),
	trans_battery_flag("transmitter battery status flag", "BOOLEAN", "transmitter battery status flag", true, StoredDataTables.ISS, StoredDataTables.SOIL),
	rainfall_daily("rain since midnight", "REAL", "total rain count since local midnight", LiveDataTypes.rainfall_daily, StoredDataTables.ISS),
	rainfall_monthly("rain this month", "REAL", "total rain count since first of month at local midnight", LiveDataTypes.rainfall_monthly, StoredDataTables.ISS),
	rainfall_year("rain this year", "REAL", "total rain count since first of user-chosen month at local midnight", LiveDataTypes.rainfall_year, StoredDataTables.ISS),
	rain_storm_last("previous rain storm", "REAL", "total rain count since last 24 hour long break in rain", StoredDataTables.ISS),  //previous - non-current rain storm
	rain_storm_last_start_at("previous storm start time", "BIGINT", "last rainstorm start time", StoredDataTables.ISS),
	rain_storm_last_end_at("previous storm end time", "BIGINT", "last rainstorm end time", true, StoredDataTables.ISS),
	
	temp_1("temp 1", "REAL", "most recent valid soil temp slot 1 (°F)", StoredDataTables.SOIL), 
	temp_2("temp 2", "REAL", "most recent valid soil temp slot 2 (°F)", StoredDataTables.SOIL),
	temp_3("temp 3", "REAL", "most recent valid soil temp slot 3 (°F)", StoredDataTables.SOIL),
	temp_4("temp 4", "REAL", "most recent valid soil temp slot 4 (°F)", StoredDataTables.SOIL),
	most_soil_1("soil moisture 1", "REAL", "most recent valid soil moisture slot 1 (cb)", StoredDataTables.SOIL),
	most_soil_2("soil moisture 2", "REAL", "most recent valid soil moisture slot 2 (cb)", StoredDataTables.SOIL),
	most_soil_3("soil moisture 3", "REAL", "most recent valid soil moisture slot 3 (cb)", StoredDataTables.SOIL),
	most_soil_4("soil moisture 4", "REAL", "most recent valid soil moisture slot 4 (cb)", StoredDataTables.SOIL),
	wet_leaf_1("leaf wetness 1", "TINYINT", "most recent valid leaf wetness slot 1", StoredDataTables.SOIL),
	wet_leaf_2("leaf wetness 2", "TINYINT", "most recent valid leaf wetness slot 2", StoredDataTables.SOIL),
	
	temp_in("temp", "REAL", "most recent valid temperature (°F)", StoredDataTables.WLL_ENV),
	hum_in("humidity", "REAL", "most recent valid humidity (%RH)", StoredDataTables.WLL_ENV),
	dew_point_in("dew point", "REAL", "most recent valid dew point (°F)", StoredDataTables.WLL_ENV),
	heat_index_in("heat index", "REAL", "most recent valid heat index (°F)", true, StoredDataTables.WLL_ENV),
	
	bar_sea_level("adjusted pressure", "REAL", "most recent bar sensor reading with elevation adjustment (inches)", StoredDataTables.WLL_BAR),
	bar_trend("barometric trend", "REAL", "current 3 hour bar trent (inches)", StoredDataTables.WLL_BAR),
	bar_absolute("absolute pressure", "REAL", "raw bar sensor reading (inches)", true, StoredDataTables.WLL_BAR),
	;
	
	//private static final Logger log = LogManager.getLogger(StoredDataTypes.class);
	private String displayName;
	private String dataType;
	private String doc;
	private StoredDataTables[] tables;
	private LiveDataTypes ldt;
	private boolean allowNulls;

	private static Hashtable<String, StoredDataTypes> lookupHash = new Hashtable<>();

	static {
		for (StoredDataTypes sdt : StoredDataTypes.values())
		{
			lookupHash.put(sdt.name(), sdt);
		}
	}
	
	private StoredDataTypes(String displayName, String dataType, boolean allowNulls, StoredDataTables ... tables)
	{
		this(displayName, dataType, displayName, allowNulls, tables);
	}
	
	private StoredDataTypes(String displayName, String dataType, String doc, StoredDataTables ... tables)
	{
		this(displayName, dataType, doc, null, false, tables);
	}
	
	private StoredDataTypes(String displayName, String dataType, String doc, boolean allowNulls, StoredDataTables ... tables)
	{
		this(displayName, dataType, doc, null, allowNulls, tables);
	}
	
	private StoredDataTypes(String displayName, String dataType, String doc, LiveDataTypes ldt, StoredDataTables ... tables)
	{
		this(displayName, dataType, doc, ldt, false, tables);
	}
	
	private StoredDataTypes(String displayName, String dataType, String doc, LiveDataTypes ldt, boolean allowNulls, StoredDataTables ... tables)
	{
		this.displayName = displayName;
		this.dataType = dataType;
		this.doc = doc;
		this.ldt = ldt;
		this.tables = tables;
		this.allowNulls = allowNulls;
		for (StoredDataTables sdt : this.tables)
		{
			sdt.addColumn(this);
		}
	}
	
	public String colCreate()
	{
		return name() + " " + dataType + (allowNulls ? "" : " NOT NULL");
	}
	
	public String getDisplayName()
	{
		return displayName;
	}
	
	public String getDoc()
	{
		return doc;
	}
	
	/**
	 * Note, this will NOT return a valid table name for storage keys did, lsid, ts and txid, trans_battery_flag, rx_state
	 * as those columns appear in multiple tables, and aren't really weather  data that this function is intended to be used for.
	 * @return
	 */
	public String getTableName()
	{
		if (tables.length > 1)
		{
			throw new RuntimeException("More than one table");
		}
		else
		{
			return tables[0].getTableName();
		}
	}
	
	public StoredDataTables[] getTables()
	{
		return tables;
	}
	
	/**
	 * Returns null, if no live data type is applicable
	 * @return
	 */
	public LiveDataTypes getLiveDataType()
	{
		return ldt;
	}
	
	public Object intoPreparedStatement(JsonNode data, int parameterIndex, PreparedStatement ps) throws SQLException
	{
		switch (dataType)
		{
			case "VARCHAR" : 
				String s = data.asText();
				ps.setString(parameterIndex, s);
				return s;
			case "BIGINT" : 
				long l = Long.parseLong(data.asText()) * 1000; 
				ps.setLong(parameterIndex, l);
				return l;
			case "TINYINT" : 
				byte b = Byte.parseByte(data.asText());
				ps.setByte(parameterIndex, b);
				return b;
			case "REAL" : 
				float f = Float.parseFloat(data.asText());
				ps.setFloat(parameterIndex, f);
				return f;
			case "BOOLEAN" : 
				boolean bool = data.asBoolean(); 
				ps.setBoolean(parameterIndex, bool);
				return bool;
			
			default:
				throw new RuntimeException("Unhandled type " + dataType);
		}
	}
	
	public void intoPreparedStatement(ResultSet rs, int parameterIndex, PreparedStatement ps) throws SQLException
	{
		switch (dataType)
		{
			case "VARCHAR" : 
				ps.setString(parameterIndex, rs.getString(parameterIndex));
				break;
			case "BIGINT" : 
				ps.setLong(parameterIndex, rs.getLong(parameterIndex));
				break;
			case "TINYINT" : 
				ps.setByte(parameterIndex, rs.getByte(parameterIndex));
				break;
			case "REAL" : 
				ps.setFloat(parameterIndex, rs.getFloat(parameterIndex));
				break;
			case "BOOLEAN" : 
				ps.setBoolean(parameterIndex, rs.getBoolean(parameterIndex));
				break;
			
			default:
				throw new RuntimeException("Unhandled type " + dataType);
		}
	}
	
	public boolean isNumeric()
	{
		switch (dataType)
		{
			case "BIGINT" : 
			case "TINYINT" : 
			case "REAL" : 
				return true;
			case "VARCHAR" : 
			case "BOOLEAN" : 
				return false;
				
			default:
				throw new RuntimeException("Unhandled type " + dataType);
		}
	}
	
	public boolean allowNulls()
	{
		return allowNulls;
	}
	
	
	public static StoredDataTypes match(String name)
	{
		return lookupHash.get(name);
	}
}
