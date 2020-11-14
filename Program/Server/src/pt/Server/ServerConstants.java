package pt.Server;

import java.io.File;

public class ServerConstants {
	
	public static final int MULTICAST_PORT = 5432;
	public static final String MULTICAST_GROUP = "239.234.102.123";
	
	
	public static final String HEARTBEAT = "HEARTBEAT";
	public static final int HEARTBEAT_SEND_INTERVAL = 1000 * 8;
	public static final int HEARTBEAT_WAIT_INTERVAL = 1000 * 10;
	
	public static final String CAME_ONLINE = "CAME_ONLINE";
	public static final String CAME_OFFLINE = "CAME_OFFLINE";
	public static final String AM_ONLINE = "AM_ONLINE";
	public static final String NEW_MESSAGE = "NEW_MESSAGE";
	
	public static final String UPDATE_USER_COUNT = "UPDATE_USER_COUNT";
	public static final float ACCEPT_PERCENTAGE_THRESHOLD = 0.5f;
	
	private static final String DATABASE_URL = "jdbc:mysql://{1}:3306/{2}?allowPublicKeyRetrieval=true&autoReconnect=true&useSSL=false";
	public static final String DATABASE_NAME = "main";
	public static final String DATABASE_USER_NAME = "server";
	public static final String DATABASE_USER_PASSWORD = "VeryStrongPassword";
	
	public static String getDatabaseURL(String address, String name) {
		return DATABASE_URL.replace("{1}", address).replace("{2}", name);
	}
	
	public static final String PUBLIC_IP_ADDRESS_API = "https://api.ipify.org";
	
	public static final int DEFAULT_GET_MESSAGES_AMOUNT = 25;
	
	private static final String FILES_PATH = "files";
	
	public static String getFilesPath() {
		return FILES_PATH + "_" + ServerMain.getInstance().getDatabaseName();
	}
	
	public static String getTransferredFilesPath() {
		return getFilesPath() + File.separator + ServerConstants.TRANSFERRED_FILES;
	}
	
	
	public static final String USER_IMAGES_DIRECTORY = "user_images";
	
	public static final String TRANSFERRED_FILES = "transferred_files";
	
	public static final int FAKE_USER_SYNC_COUNT = 25;
	public static final String ASK_SYNCHRONIZER = "ASK_SYNCHRONIZER";
}
