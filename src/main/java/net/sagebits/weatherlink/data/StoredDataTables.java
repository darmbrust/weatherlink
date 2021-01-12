package net.sagebits.weatherlink.data;

import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;

public enum StoredDataTables
{
	ISS("iss"), SOIL("soil"), WLL_ENV("wll_env"), WLL_BAR("wll_bar");

	final private String tableName;
	private ArrayList<StoredDataTypes> columns = new ArrayList<>();
	
	static
	{
		//Need to make sure the StoredDataTypes have been init'ed before this call.  Don't remove the call to values()
		LogManager.getLogger().debug("creating tables if missing - {} potential columns", StoredDataTypes.values().length);
	}
	
	private StoredDataTables(String tableName)
	{
		this.tableName = tableName;
	}
	
	public String getTableName()
	{
		return tableName;
	}

	protected void addColumn(StoredDataTypes storedDataType)
	{
		columns.add(storedDataType);
	}
	
	public StoredDataTypes[] getColumns()
	{
		return columns.toArray(new StoredDataTypes[columns.size()]);
	}
	
}
