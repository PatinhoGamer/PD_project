package pt.Server;

import pt.Common.*;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;

public class Synchronizer {
	
	private static final String GET_USERS = "GET_USERS";
	private static final String GET_CHANNELS = "GET_CHANNELS";
	private static final String GET_USERS_CHANNELS = "GET_USERS_CHANNELS";
	private static final String GET_MESSAGES = "GET_MESSAGES";
	public static final int USERS_BLOCK = 40;
	public static final int CHANNELS_BLOCK = 30;
	public static final int USERS_CHANNELS_BLOCK = 100;
	public static final int MESSAGES_BLOCK = 20;
	private static final String NO_MORE_USERS = "NO_MORE_USERS";
	private static final String NO_MORE_CHANNELS = "NO_MORE_CHANNELS";
	private static final String NO_MORE_USERS_CHANNELS = "NO_MORE_USERS_CHANNELS";
	private static final String NO_MORE_MESSAGES = "NO_MORE_MESSAGES";
	
	private final ServerAddress otherServer;
	private final ServerAddress thisServer;
	private final DatagramSocket socket;
	
	public Synchronizer(ServerAddress otherServer, ServerAddress thisServer, DatagramSocket socket) {
		this.otherServer = otherServer;
		this.thisServer = thisServer;
		this.socket = socket;
	}
	
	public void receiveData() throws Exception {
		//Setup connection with server
		System.out.println("sent");
		//UDPHelper.sendUDPObjectReliably(new ServerCommand(ServerConstants.ASK_SYNCHRONIZER, server), socket, server.getAddress(), server.getUDPPort());
		UDPHelper.sendUDPObjectReliably(ServerConstants.ASK_SYNCHRONIZER, null, otherServer, socket);
		System.out.println("sent reliably");
		// send to the other server to get all missing users -----------------------------------------------------------
		int lastUserId = UserManager.getLastUserId();
		sendCommand(GET_USERS, lastUserId);
		
		while (true) {
			Command command = (Command) receiveCommand();
			if (command.getProtocol().equals(NO_MORE_USERS)) {
				break;
			}
			ArrayList<UserInfo> newUsers = (ArrayList<UserInfo>) command.getExtras();
			for (var user : newUsers) {
				//TODO send photo, chunks of 5KB
				//String imagePath = UserManager.saveImage(user);
				//UserManager.insertFull(user, imagePath);
				if (!UserManager.insertFull(user, "")) {
					System.out.println("User insert");
					socket.close();
					return;
				}
			}
		}
		
		// send to the other server to get all missing channels --------------------------------------------------------
		int lastChannelId = ChannelManager.getLastChannelId();
		sendCommand(GET_CHANNELS, lastChannelId);
		
		while (true) {
			Command command = (Command) receiveCommand();
			if (command.getProtocol().equals(NO_MORE_CHANNELS)) {
				break;
			}
			ArrayList<ChannelInfo> newChannels = (ArrayList<ChannelInfo>) command.getExtras();
			for (var channel : newChannels) {
				if (!ChannelManager.insertChannel(channel)) {
					System.out.println("Channel insert");
					socket.close();
					return;
				}
			}
		}
		
		// synchronize user_channel ------------------------------------------------------------------------------------
		int lastConnectionId = ChannelManager.getLastUserChannelId();
		sendCommand(GET_USERS_CHANNELS, lastConnectionId);
		
		while (true) {
			Command command = (Command) receiveCommand();
			if (command.getProtocol().equals(NO_MORE_USERS_CHANNELS)) {
				break;
			}
			ArrayList<Ids> channelUsers = (ArrayList<Ids>) command.getExtras();
			for (var id : channelUsers) {
				if (!ChannelManager.registerUserToChannel(id.getUserId(), id.getChannelId())) {
					System.out.println("registerUserToChannel insert");
					socket.close();
					return;
				}
			}
		}
		
		// send to the other server to get all missing messages --------------------------------------------------------
		int lastMessageId = MessageManager.getLastMessageId();
		sendCommand(GET_MESSAGES, lastMessageId);
		
		while (true) {
			Command command = (Command) receiveCommand();
			if (command.getProtocol().equals(NO_MORE_MESSAGES)) {
				break;
			}
			ArrayList<MessageInfo> newMessages = (ArrayList<MessageInfo>) command.getExtras();
			for (var message : newMessages) {
				if (!MessageManager.insertFull(message)) {
					System.out.println("Message insert");
					socket.close();
					return;
				}
			}
		}
		// channel_message and user_message already get inserted along with the messages due to foreign keys -----------
	}
	
	public void sendData() throws Exception {
		System.out.println("waiting for synchronization request");
		while (true) {
			ServerCommand command = (ServerCommand) receiveCommand();
			if (!command.getServerAddress().equals(otherServer)) {
			
			}
			
			switch (command.getProtocol()) {
				
				case GET_USERS -> {
					int lastUserId = (int) command.getExtras();
					
					ArrayList<UserInfo> usersAfterId = UserManager.getAfterId(lastUserId);
					sendByBlocks(usersAfterId, USERS_BLOCK);
					
					sendCommand(NO_MORE_USERS, null);
				}
				
				case GET_CHANNELS -> {
					int lastChannelId = (int) command.getExtras();
					
					ArrayList<ChannelInfo> channelsAfterID = ChannelManager.getAfterId(lastChannelId);
					sendByBlocks(channelsAfterID, CHANNELS_BLOCK);
					
					sendCommand(NO_MORE_CHANNELS, null);
				}
				
				case GET_USERS_CHANNELS -> {
					int lastConnectionId = (int) command.getExtras();
					
					ArrayList<Ids> channelUsers = ChannelManager.getChannelUsersAfterIds(lastConnectionId);
					sendByBlocks(channelUsers, USERS_CHANNELS_BLOCK);
					
					sendCommand(NO_MORE_USERS_CHANNELS, null);
				}
				
				case GET_MESSAGES -> {
					int lastMessageId = (int) command.getExtras();
					
					ArrayList<MessageInfo> messagesAfterId = MessageManager.getAfterId(lastMessageId);
					sendByBlocks(messagesAfterId, MESSAGES_BLOCK);
					
					sendCommand(NO_MORE_CHANNELS, null);
				}
			}
		}
	}
	
	private void sendCommand(String protocol, Object object) throws IOException {
		UDPHelper.sendUDPObjectReliably(protocol, object, otherServer, socket);
	}
	
	private Object receiveCommand() throws Exception {
		return UDPHelper.receiveUDPObjectReliably(socket, otherServer);
	}
	
	private void sendByBlocks(ArrayList list, int blockSize) throws Exception {
		if (list.size() == 0) {
			return;
		}
		for (int usersCount = 0; usersCount < list.size(); usersCount++) {
			ArrayList block = new ArrayList();
			
			for (int blockCounter = 0; (blockCounter < blockSize) && (usersCount < list.size()); blockCounter++, usersCount++) {
				block.add(list.get(usersCount));
			}
			sendCommand("", block);
		}
	}
}