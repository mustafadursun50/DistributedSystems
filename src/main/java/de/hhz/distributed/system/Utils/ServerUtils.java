package de.hhz.distributed.system.Utils;

import java.util.Map;

public class ServerUtils {
	public static <K, V> K getKey(Map<K, V> map, V value) {
		return map.entrySet()
					   .stream()
					   .filter(entry -> value.equals(entry.getValue()))
					   .map(Map.Entry::getKey)
					   .findFirst().get();
	}
}
