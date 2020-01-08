package net.sagebits.weatherlink.data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.stream.Stream;
import javafx.util.Pair;

public class DataCondenser
{
	
	public static ArrayList<Pair<Long, Double>> averageEvery(int minsPerAvg, Stream<Pair<Long, Number>> dataProvider)
	{
		ArrayList<Pair<Long, Double>> result = new ArrayList<>();
		
		DoubleAccumulator da = new DoubleAccumulator(Double::sum, 0d);
		AtomicInteger countAccumulated = new AtomicInteger();
		AtomicLong lastTimeOfProcessedGroup = new AtomicLong();
		AtomicInteger lastProcessedMinute = new AtomicInteger(-1);
		
		dataProvider.forEach(pair ->
		{
			LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(pair.getKey()), ZoneId.systemDefault());
			int min = ldt.getMinute();
			if (min % minsPerAvg == 0 && lastProcessedMinute.get() != min)
			{
				if (countAccumulated.get() > 0)
				{
					result.add(new Pair<>(lastTimeOfProcessedGroup.get(), (da.get() / (double)countAccumulated.get())));
				}
				lastProcessedMinute.set(min);
				da.reset();
				countAccumulated.set(0);
			}
			if (pair.getValue() != null)
			{
				da.accumulate(pair.getValue().doubleValue());
				countAccumulated.getAndIncrement();
			}
			lastTimeOfProcessedGroup.set(pair.getKey());
		});

		return result;
	}
	
	public static ArrayList<Pair<Long, Double>> maxEvery(int minsPerAvg, Stream<Pair<Long, Number>> dataProvider)
	{
		ArrayList<Pair<Long, Double>> result = new ArrayList<>();
		
		DoubleAccumulator da = new DoubleAccumulator(Double::max, Double.MIN_VALUE);
		AtomicLong lastTimeOfProcessedGroup = new AtomicLong();
		AtomicInteger lastProcessedMinute = new AtomicInteger(-1);
		
		dataProvider.forEach(pair ->
		{
			LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(pair.getKey()), ZoneId.systemDefault());
			int min = ldt.getMinute();
			if (min % minsPerAvg == 0 && lastProcessedMinute.get() != min)
			{
				if (da.get() > Double.MIN_VALUE)
				{
					result.add(new Pair<>(lastTimeOfProcessedGroup.get(), da.get()));
				}
				da.reset();
			}
			if (pair.getValue() != null)
			{
				da.accumulate(pair.getValue().doubleValue());
			}
			lastTimeOfProcessedGroup.set(pair.getKey());
		});

		return result;
	}
}
