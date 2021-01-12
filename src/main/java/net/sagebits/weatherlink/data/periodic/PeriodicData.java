package net.sagebits.weatherlink.data.periodic;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sagebits.weatherlink.data.DataFetcher;
import net.sagebits.weatherlink.data.StoredDataTables;
import net.sagebits.weatherlink.data.StoredDataTypes;
import net.sagebits.weatherlink.data.WeatherProperty;

/**
 * Instance class to interface with the DB where we stuff each periodic read of the full data set.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class PeriodicData
{
	private volatile static PeriodicData instance_;
	private static final Logger log = LogManager.getLogger(PeriodicData.class);
	
	private final Connection db;
	private final Connection dbArchive;
	
	//singleton
	private PeriodicData() throws SQLException
	{
		File homeFolder = Paths.get(System.getProperty("user.home"), "Weather Link Live GUI Data").toAbsolutePath().toFile();
		homeFolder.mkdirs();
		File dbFile = new File(homeFolder, "weatherLinkData");
		File dbArchiveFile = new File(homeFolder, "weatherLinkDataArchive");
		log.info("Data folder: {}", dbFile.getAbsoluteFile().toString());
		
		db = DriverManager.getConnection("jdbc:h2:" + dbFile.getAbsolutePath() + ";TRACE_LEVEL_FILE=0", "sa", "");
		dbArchive = DriverManager.getConnection("jdbc:h2:" + dbArchiveFile.getAbsolutePath() + ";TRACE_LEVEL_FILE=0", "sa", "");
		
		createTables(db);
		createTables(dbArchive);
	}
	
	private void createTables(Connection currentDb) throws SQLException 
	{
		for (StoredDataTables sdt : StoredDataTables.values())
		{
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE IF NOT EXISTS ");
			sb.append(sdt.getTableName());
			sb.append(" (");
			for (StoredDataTypes dt : sdt.getColumns())
			{
				sb.append(dt.colCreate());
				sb.append(", ");
			}
			sb.setLength(sb.length() - 2);
			sb.append(")");
			currentDb.prepareStatement(sb.toString()).execute();
		}
		
		currentDb.prepareStatement("CREATE INDEX IF NOT EXISTS iss_index ON iss (did, lsid, ts)").execute();
		currentDb.prepareStatement("CREATE INDEX IF NOT EXISTS soil_index ON soil (did, lsid, ts)").execute();
		currentDb.prepareStatement("CREATE INDEX IF NOT EXISTS wll_env_index ON wll_env (did, lsid, ts)").execute();
		currentDb.prepareStatement("CREATE INDEX IF NOT EXISTS wll_bar_index ON wll_bar (did, lsid, ts)").execute();
	}
	
	public static PeriodicData getInstance()
	{
		if (instance_ == null)
		{
			synchronized (PeriodicData.class)
			{
				if (instance_ == null)
				{
					try
					{
						instance_ = new PeriodicData();
					}
					catch (SQLException e)
					{
						log.error("error setting up", e);
						throw new RuntimeException(e);
					}
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
		String lsid = null;
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
			if (name.equals(StoredDataTypes.lsid.name()))
			{
				lsid = e.getValue().asText();
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
		try (PreparedStatement ps = db.prepareStatement("INSERT INTO " + tableName + " (" + colNames.toString() + ") VALUES (" + valuePlaceholders.toString() + ")"))
		{
			int i = 1;
			ps.setLong(i++, timeStamp);
			ps.setString(i++, did);
			HashMap<StoredDataTypes, Object> updatedData = new HashMap<>(valuesToSet.size());
			for (Entry<String, JsonNode> jn : valuesToSet)
			{
				StoredDataTypes sdt = StoredDataTypes.match(jn.getKey());
				Object newData = sdt.intoPreparedStatement(jn.getValue(), i++, ps);
				updatedData.put(sdt, newData);
			}
			ps.execute();
			
			if (updatedData.containsKey(StoredDataTypes.wind_dir_last) &&
					((Float)updatedData.get(StoredDataTypes.wind_dir_last)).floatValue() == 0.0 
					&& ((Float)updatedData.get(StoredDataTypes.wind_speed_last)).floatValue() == 0.0)
			{
				//Its an oddity, that when the wind speed drops to 0, they also change the direction to 0, instead of leaving it alone.
				//Don't update our property in this case (but go ahead and store the real data in the DB).  
				//This is half of the solution (the periodic part), to keep the wind vane from swinging around when it is light and variable.
				updatedData.remove(StoredDataTypes.wind_dir_last);
				log.trace("Ignore 0 wind direction with 0 speed");
			}
			
			if (lsid == null)
			{
				log.error("lsid not provided as part of conditions?");
			}
			else
			{
				DataFetcher.getInstance().update(updatedData, did, lsid, timeStamp);
			}
		}
		log.trace("Appended table {} in {}ms",  tableName, (System.currentTimeMillis() - time));
	}
	
	public WeatherProperty getLatestData(String wllDeviceId, String sensorId, StoredDataTypes dt)
	{
		long time = System.currentTimeMillis();
		try (PreparedStatement ps = db.prepareStatement("SELECT o.ts, " + dt.name() + " FROM " + dt.getTableName() + " o"
				+ " INNER JOIN (SELECT did, lsid, MAX(ts) as ts FROM " + dt.getTableName()
				+ " WHERE did = ? and lsid = ? GROUP BY did, lsid) i"
				+ " ON i.did = o.did AND i.lsid = o.lsid AND i.ts = o.ts"))
//				+ " WHERE (did, lsid, ts) = (SELECT did, lsid, MAX(ts) FROM " + dt.getTableName() 
//					+ " WHERE did=? AND lsid = ? GROUP BY did,lsid)"))
		{
			ps.setString(1, wllDeviceId);
			ps.setString(2, sensorId);
			
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					WeatherProperty wp = new WeatherProperty(dt.name());
					wp.set(rs.getObject(dt.name()));
					wp.setTimeStamp(rs.getLong("ts"));
					return wp;
				}
			}
		}
		catch (SQLException e)
		{
			log.error("unexpected", e);
		}
		finally
		{
			log.trace("latest data query for {} took {}ms", dt, System.currentTimeMillis() - time);
		}
		return new WeatherProperty(dt.name());
	}
	
	public Optional<Float> getMaxForDay(String wllDeviceId, String sensorId, StoredDataTypes dt, Date day)
	{
		return getMinOrMinForDay(wllDeviceId, sensorId, dt, day, "MAX");
	}
	
	public Optional<Float> getMinForDay(String wllDeviceId, String sensorId, StoredDataTypes dt, Date day)
	{
		return getMinOrMinForDay(wllDeviceId, sensorId, dt, day, "MIN");
	}
	
	private Optional<Float> getMinOrMinForDay(String wllDeviceId, String sensorId, StoredDataTypes dt, Date day, String maxOrMin)
	{
		long time = System.currentTimeMillis();
		if (!dt.isNumeric())
		{
			throw new RuntimeException("Can't get min on non-numeric type!");
		}
		try
		{
			// today    
			Calendar date = new GregorianCalendar();
			date.setTime(day);
			// reset hour, minutes, seconds and millis
			date.set(Calendar.HOUR_OF_DAY, 0);
			date.set(Calendar.MINUTE, 0);
			date.set(Calendar.SECOND, 0);
			date.set(Calendar.MILLISECOND, 0);
			
			long start = date.getTimeInMillis();

			// next day
			date.add(Calendar.DAY_OF_MONTH, 1);
			long end = date.getTimeInMillis();
			
			try (PreparedStatement ps = db.prepareStatement("SELECT " + maxOrMin + "(" + dt.name() + ") AS " + dt.name() + " FROM " + dt.getTableName() 
				+ " WHERE did= ? AND lsid = ? AND ts >= ? AND ts < ?"))
			{
				ps.setString(1, wllDeviceId);
				ps.setString(2, sensorId);
				ps.setLong(3, start);
				ps.setLong(4, end);
				
				log.trace("Query: {}", ps.toString());
				
				try (ResultSet rs = ps.executeQuery())
				{
					if (rs.next())
					{
						return Optional.of(rs.getFloat(dt.name()));
					}
				}
			}
		}
		catch (SQLException e)
		{
			log.error("unexpected", e);
		}
		finally
		{
			log.trace("find {} took {}ms", maxOrMin, System.currentTimeMillis() - time);
		}
		return Optional.empty();
	}
	
	/**
	 * Get all stored data for the specified data types.  All requested data elements need to come from the same table 
	 * {@link StoredDataTypes#getTableName()} is the same for each.
	 * 
	 * Will return a list containing arrays where each array is sized one larger than the number of supplied data types.  
	 * The first item in each array will be a Long, which represent the data timestamp.  Each of the next object corresponds to a 
	 * requested dataType.
	 * 
	 * All arrays are the same length.  The list will be ordered from oldest to newest.
	 * @throws SQLException 
	 */
	public List<Object[]> getDataForRange(String wllDeviceId, String sensorId, Long start, Optional<Long> end, StoredDataTypes ... dataTypes) throws SQLException
	{
		long time = System.currentTimeMillis();
		try
		{
			if (dataTypes == null || dataTypes.length == 0)
			{
				throw new RuntimeException("Must supply at least one data type");
			}
			String tableName = null;
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT ts, ");
			for (StoredDataTypes sdt : dataTypes)
			{
				if (tableName == null)
				{
					tableName = sdt.getTableName();
				}
				else if (!tableName.equals(sdt.getTableName()))
				{
					throw new RuntimeException("All table names must be the same for requested data types");
				}
					
				sb.append(sdt.name());
				sb.append(", ");
			}
			
			sb.setLength(sb.length() - 2);
			
			sb.append(" FROM ");
			sb.append(tableName);
			sb.append(" WHERE did = ? and lsid = ? AND ts >= ?");
			if (end.isPresent())
			{
				sb.append(" AND ts < ?");
			}
			sb.append(" ORDER by ts");
			
			ArrayList<Object[]> results = new ArrayList<>();
			
			try (PreparedStatement ps = db.prepareStatement(sb.toString()))
			{
				ps.setString(1, wllDeviceId);
				ps.setString(2, sensorId);
				ps.setLong(3, start);
				if (end.isPresent())
				{
					ps.setLong(4, end.get());
				}
				
				log.trace("Query: {}", ps.toString());
				
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						int i = 0;
						Object[] rowResult = new Object[dataTypes.length + 1];
						rowResult[i++] = rs.getLong("ts");
						for (StoredDataTypes sdt : dataTypes)
						{
							rowResult[i++] = rs.getObject(sdt.name());
						}
						results.add(rowResult);
					}
				}
			}
			return results;
		}
		finally
		{
			log.trace("get data for range took {}ms", System.currentTimeMillis() - time);
		}
		
	}
	
	/**
	 * @return All unique WeatherLinkLive IDs, with a set of sensor IDs tied to each.
	 */
	public HashSet<String> getWeatherLinkDeviceIds()
	{
		long time = System.currentTimeMillis();
		HashSet<String> results = new HashSet<>();
		try
		{
			for (String table : new String[] {"iss", "soil", "wll_env", "wll_bar"})
			{
				try (PreparedStatement ps = db.prepareStatement("SELECT DISTINCT did from " + table);
						ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						results.add(rs.getString("did"));
					}
				}
			}
			
		}
		catch (SQLException e)
		{
			log.error("unexpected problem reading tables", e);
		}
		finally
		{
			log.trace("WllDeviceIdQuery took {}ms", System.currentTimeMillis() - time);
		}
		return results;
	}
	
	public Set<String> getSensorsFor(String wllDeviceId, StoredDataTypes sdt)
	{
		long time = System.currentTimeMillis();
		HashSet<String> results = new HashSet<>();
		try (PreparedStatement ps = db.prepareStatement("SELECT DISTINCT did, lsid from " + sdt.getTableName() + " WHERE did = ?"))
		{
			ps.setString(1, wllDeviceId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					results.add(rs.getString("lsid"));
				}
			}
		}
		catch (SQLException e)
		{
			log.error("unexpected problem reading tables", e);
		}
		finally
		{
			log.trace("SensorQueryFor took {}ms", System.currentTimeMillis() - time);
		}
		return results;
	}
	
	/**
	 * 
	 * @param minSecondsBeweenDataPoints allows to remove / trim the data set.
	 * If data is recorded once every 10 seconds - Setting the parameter to a value >= 10 will cause it to archive / keep all data.
	 * Setting it to a value higher than the record rate will cause it to skip records - keeping at most, one record per each minSecondsBetweenDataPoints 
	 * interval.  
	 */
	public void archiveData(int minSecondsBeweenDataPoints)
	{
		log.info("Running data archive routine");
		try
		{
			//archive all data older than 7 days
			final long archiveOlderThan = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
			log.debug("Archiving data older than {}", archiveOlderThan);
			for (StoredDataTables sdt : StoredDataTables.values())
			{
				//Trying to use a scrollable result set, to allow me to go back by one row results in terrible performance.
				//Rewrote the logic to copy the first result of each block, instead of the last.
				PreparedStatement ps = db.prepareStatement("SELECT * FROM " + sdt.getTableName() + " where ts < ? order by did, lsid, ts asc");
				ps.setLong(1, archiveOlderThan);
				
				ResultSet rs = ps.executeQuery();
				int processed = 0;
				int copied = 0;
				long lastCopiedTS = Long.MIN_VALUE;
				long skipTo = Long.MIN_VALUE;
				while (rs.next())
				{
					processed++;
					long rowTS = rs.getLong("ts");
					
					boolean idChange = false;
					if (rowTS < lastCopiedTS)
					{
						//This means the did or lsid changed.  Restart index
						idChange = true;
						skipTo = Long.MIN_VALUE;
					}
					
					if (rowTS < skipTo)
					{
						continue;
					}
					
					copyRow(rs, sdt);
					lastCopiedTS = idChange? Long.MIN_VALUE : rowTS;
					copied++;
					skipTo = rowTS + (minSecondsBeweenDataPoints * 1000);
				}
				
				log.debug("Copied {} items out of {} for table {} ", copied, processed, sdt.getTableName());
				ps.close();
				
				ps = db.prepareStatement("DELETE FROM " + sdt.getTableName() + " where ts < ?");
				ps.setLong(1, archiveOlderThan);
				ps.execute();
				ps.close();
				log.debug("Deleted archive data from main table");
			}
			
			log.info("data archive complete");
		}
		catch (SQLException e)
		{
			log.error("Failure archiving old data", e);
		}
	}

	private void copyRow(ResultSet rs, StoredDataTables sdt) throws SQLException
	{
		StringBuilder colNames = new StringBuilder();
		StringBuilder valuePlaceholders = new StringBuilder();
		for (StoredDataTypes c : sdt.getColumns())
		{
			colNames.append(c.name());
			colNames.append(", ");
			valuePlaceholders.append("?, ");
		}
		colNames.setLength(colNames.length() - 2);
		valuePlaceholders.setLength(valuePlaceholders.length() - 2);
		
		
		try (PreparedStatement ps = dbArchive.prepareStatement("INSERT INTO " + sdt.getTableName() 
			+ " (" + colNames.toString() + ") VALUES (" + valuePlaceholders.toString() + ")"))
		{
			int i = 1;
			for (StoredDataTypes c : sdt.getColumns())
			{
				c.intoPreparedStatement(rs, i++, ps);
			}
			ps.execute();
		}
	}

	public void shutDown() throws SQLException
	{
		log.info("DB Shutdown requested");
		dbArchive.close();
		db.close();
	}
}
