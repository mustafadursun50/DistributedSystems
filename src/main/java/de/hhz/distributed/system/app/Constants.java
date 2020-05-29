package de.hhz.distributed.system.app;

public class Constants {
	public static int SERVER_PORT_START = 800;
	public static int SERVER_UUID_START = 0;
	public final static int MULTICAST_PORT = 4446;
	public final static int MULTICAST_PORT2 = 5556;
	public final static int NUMBER_OF_SERVERS = 1;
	public final  static String MULTICAST_ADDRESS = "230.0.0.0";
	public final  static String PING_LEADER_TO_REPLICA = "PingLeaderToReplica";
	public final static long MAX_PING_LIMIT_SEC = 5;
	public static final Object PROPERTY_HOST_ADDRESS = "host";
	public static final Object PROPERTY_HOST_PORT = "port";
}
