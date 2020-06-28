package de.hhz.distributed.system.app;

public class Constants {
	public static int SERVER_PORT_START = 800;
	public static int SERVER_UUID_START = 0;
	public final static int SERVER_MULTICAST_PORT = 4446;
	public final static int CLIENT_MULTICAST_PORT = 4447;
	public final static int NUMBER_OF_SERVERS = 3;
	public final static String SERVER_MULTICAST_ADDRESS = "230.0.0.0";
	public final static String CLIENT_MULTICAST_ADDRESS = "230.0.0.1";

	public final static String PING_REPLICA = "PingLeaderToReplica";
	public final static int PING_INTERVALL_SEC = 10;
	public static final long START_FIRST_PING_AFTER_SEC = 3;
	public final static long MAX_PING_LIMIT_SEC = 10;
	public static final Object PROPERTY_HOST_ADDRESS = "host";
	public static final Object PROPERTY_HOST_PORT = "port";
	public static final String PRODUCT_DB_NAME = "productDb.txt";
	public static final String PING_LEADER = "PingReplicaToLeader";
	public static final String CLIENT_MULTICAST_MESSAGE = "hallo";
	public static final String PACKAGE_LOSS = "packageLoss";
	public static final String UPDATE_REPLICA = "updatereplicats";
	public static final String RESERVESUCCES = "reserve succesfully";
}
