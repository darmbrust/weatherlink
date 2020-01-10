package net.sagebits.weatherlink.data;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.stream.Stream;
import javafx.util.Pair;

public class DataCondenser
{

	public enum MISSING_BEHAVIOR {SKIP, NAN, ZERO}
	
	/**
	 * 
	 * @param minsPerAvg
	 * @param dataProvider
	 * @param zeroMissing if there is no data for an interval, should we return 0 for that interval, or skip it?
	 * @return
	 */
	public static ArrayList<Pair<Long, Double>> averageEvery(int minsPerAvg, Stream<Pair<Long, Number>> dataProvider, MISSING_BEHAVIOR zeroMissing)
	{
		ArrayList<Pair<Long, Double>> result = new ArrayList<>();
		
		DoubleAccumulator da = new DoubleAccumulator(Double::sum, 0d);
		AtomicInteger countAccumulated = new AtomicInteger();
		AtomicLong nextAvgInterval = new AtomicLong(Long.MIN_VALUE);
		
		dataProvider.forEach(pair ->
		{
			if (nextAvgInterval.get() == Long.MIN_VALUE)
			{
				//Calculate the first interval point.
				nextAvgInterval.set(pair.getKey() + (minsPerAvg * 60 * 1000));
			}
			while (pair.getKey() >= nextAvgInterval.get())
			{
				if (countAccumulated.get() > 0 || zeroMissing == MISSING_BEHAVIOR.ZERO || zeroMissing == MISSING_BEHAVIOR.NAN)
				{
					result.add(new Pair<>(nextAvgInterval.get(), (countAccumulated.get() > 0 ? (da.get() / (double)countAccumulated.get()) : 
						(zeroMissing == MISSING_BEHAVIOR.ZERO ? 0 : Double.NaN))));
				}
				else
				{
					//don't add an entry.
				}
				da.reset();
				nextAvgInterval.getAndAdd(minsPerAvg * 60 * 1000);
				countAccumulated.set(0);
			}
			
			if (pair.getValue() != null)
			{
				da.accumulate(pair.getValue().doubleValue());
				countAccumulated.getAndIncrement();
			}
		});

		return result;
	}
	
	public static ArrayList<Pair<Long, Double>> maxEvery(int minsPerAvg, Stream<Pair<Long, Number>> dataProvider)
	{
		ArrayList<Pair<Long, Double>> result = new ArrayList<>();
		DoubleAccumulator da = new DoubleAccumulator(Double::max, Double.MIN_VALUE);
		AtomicLong nextAvgInterval = new AtomicLong(Long.MIN_VALUE);
		
		dataProvider.forEach(pair ->
		{
			if (nextAvgInterval.get() == Long.MIN_VALUE)
			{
				//Calculate the first interval point.
				nextAvgInterval.set(pair.getKey() + (minsPerAvg * 60 * 1000));
			}
			while (pair.getKey() >= nextAvgInterval.get())
			{
				result.add(new Pair<>(nextAvgInterval.get(), (da.get() > Double.MIN_VALUE ? da.get() : 0.0)));
				da.reset();
				nextAvgInterval.getAndAdd(minsPerAvg * 60 * 1000);
			}
			if (pair.getValue() != null)
			{
				da.accumulate(pair.getValue().doubleValue());
			}
		});

		return result;
	}
}
