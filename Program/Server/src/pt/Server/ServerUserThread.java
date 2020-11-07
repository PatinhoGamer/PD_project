package pt.Server;

import pt.Common.*;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;

public class ServerUserThread extends Thread {
	
	private final Socket socket;
	private final ObjectOutputStream oos;
	private final ObjectInputStream ois;
	
	private boolean isLoggedIn = false;
	private UserInfo userInfo;
	private int currentChannelId;
	
	private boolean keepReceiving = true;
	private ArrayList<ServerAddress> orderedServerAddresses;
	
	private ServerMain getApp() {
		return ServerMain.getInstance();
	}
	
	public ServerUserThread(Socket socket, ArrayList<ServerAddress> orderedServerAddresses) throws IOException {
		this.socket = socket;
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
		this.orderedServerAddresses = orderedServerAddresses;
	}
	
	@Override
	public void run() {
		try {
			sendCommand(Constants.SERVERS_LIST, orderedServerAddresses);
			orderedServerAddresses = null;
			receiveRequests();
		} catch (IOException e) {
			System.out.println("Exception sending servers list : " + e.getMessage());
			disconnect();
		}
	}
	
	private void receiveRequests() {
		try {
			while (keepReceiving) {
				Command command;
				try {
					command = (Command) ois.readObject();
				} catch (ClassNotFoundException e) {
					System.out.println("Error reading protocol : " + e.getLocalizedMessage());
					continue;
				} catch (IOException e) {
					throw new IOException("Connection lost");
				}
				System.out.println(command);
				handleRequest(command);
			}
		} catch (IOException e) { // Lost connection
			disconnect();
		} catch (NoSuchAlgorithmException | SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		getApp().removeConnected(this);
	}
	
	private void handleRequest(Command protocol) throws IOException, SQLException, NoSuchAlgorithmException {
		switch (protocol.getProtocol()) {
			case Constants.REGISTER -> {
				handleRegister((UserInfo) protocol.getExtras());
			}
			// TODO login from multiple clients CANNOT HAPPEN
			case Constants.LOGIN -> {
				UserInfo userInfo = (UserInfo) protocol.getExtras();
				login(userInfo.getUsername(), userInfo.getPassword());
			}
			
			case Constants.DISCONNECTING -> {
				disconnect();
			}
			
			default -> {
				if (isLoggedIn()) {
					switch (protocol.getProtocol()) {
						case Constants.CHANNEL_GET_ALL -> {
							protocolChannelGetALl();
						}
						
						case Constants.CHANNEL_GET_MESSAGES -> {
							Ids ids = (Ids) protocol.getExtras();
							protocolChannelGetMessages(ids);
							//TODO test get messages before certain message
						}
						
						case Constants.CHANNEL_ADD -> {
							ChannelInfo info = (ChannelInfo) protocol.getExtras();
							protocolChannelAdd(info);
							//TODO test channel add
						}
						
						case Constants.CHANNEL_REMOVE -> {
							int channelId = (int) protocol.getExtras();
							protocolChannelRemove(channelId);
							//TODO test channel remove
						}
						
						case Constants.CHANNEL_EDIT -> {
							ChannelInfo info = (ChannelInfo) protocol.getExtras();
							protocolChannelEdit(info);
							//TODO test channel edition
						}
						
						case Constants.ADD_MESSAGE -> {
							MessageInfo message = (MessageInfo) protocol.getExtras();
							protocolAddMessage(message);
						}
						
						case Constants.ADD_FILE -> {
							MessageInfo message = (MessageInfo) protocol.getExtras();
							protocolAddFile(message);
							//TODO test add file
						}
						
						case Constants.CHANNEL_REGISTER -> {
							ChannelInfo channelInfo = (ChannelInfo) protocol.getExtras();
							protocolChannelRegister(channelInfo);
						}
						//TODO add same shit but for user messages
						
						case Constants.LOGOUT -> {
							logout();
						}
					}
				}
			}
		}
	}
	
	public void protocolAddFile(MessageInfo message) throws IOException, SQLException {
		if (message.getContent().isBlank()) {
			sendCommand(Constants.ERROR, "Empty Content");
			return;
		}
		String filePath = ServerConstants.FILES_PATH + File.separator +
				ServerConstants.TRANSFERRED_FILES + File.separator +
				message.getContent();
		
		File fileFile = new File(filePath);
		Utils.createDirectories(fileFile);
		
		if (fileFile.exists()) {
			throw new IOException("File already exits");
			// TODO fix repeated name
		}
		
		Socket fileSocket = new Socket();
		System.out.println("Went through");
		sendCommand(Constants.SUCCESS, fileSocket.getPort());
		
		FileOutputStream fileStream = new FileOutputStream(fileFile);
		
		InputStream inputStream = fileSocket.getInputStream();
		System.out.println("After get input stream");
		byte[] buffer = new byte[Constants.CLIENT_FILE_CHUNK_SIZE];
		while (true) {
			System.out.println("Waiting for bytes");
			int readAmount = inputStream.read(buffer);
			if (readAmount == 0) { /* Finished transferring file */
				fileStream.close();
				break;
			}
			
			fileStream.write(buffer, 0, readAmount);
		}
		System.out.println("Finished file transfer");
		
		if (message.getRecipientType().equals(MessageInfo.Recipient.CHANNEL)) {
			if (ServerChannelManager.insertMessage(userInfo.getUserId(), message.getRecipientId(), message.getContent()))
				sendCommand(Constants.SUCCESS);
			else
				sendCommand(Constants.ERROR, "Should not happen");
		} else  {
			if (ServerUserManager.insertMessage(userInfo.getUserId(), message.getRecipientId(), message.getContent()))
				sendCommand(Constants.SUCCESS);
			else
				sendCommand(Constants.ERROR, "Should not happen");
		}
	}
	
	public void protocolChannelEdit(ChannelInfo info) throws IOException, SQLException, NoSuchAlgorithmException {
		if (!Utils.checkPasswordFollowsRules(info.getPassword())) {
			sendCommand(Constants.ERROR, "Invalid Password");
			return;
		}
		boolean success = ServerChannelManager.updateChannel(info.getId(), info.getName(), info.getPassword(), info.getDescription());
		if (success) sendCommand(Constants.SUCCESS);
		else sendCommand(Constants.ERROR, "Error Updating, channel name might already be in use");
	}
	
	public void protocolChannelRegister(ChannelInfo channelInfo) throws IOException, SQLException, NoSuchAlgorithmException {
		if (ServerChannelManager.isUserPartOf(userInfo.getUserId(), channelInfo.getId()))
			sendCommand(Constants.SUCCESS, "User is already part of channel");
		else {
			if (ServerChannelManager.isChannelPassword(channelInfo.getId(), channelInfo.getPassword())) {
				if (ServerChannelManager.registerUserToChannel(userInfo.getUserId(), channelInfo.getId()))
					sendCommand(Constants.SUCCESS);
				else
					sendCommand(Constants.ERROR, "Should never happen. Pls fix");
			} else sendCommand(Constants.ERROR, "Wrong password");
		}
	}
	
	public void protocolAddMessage(MessageInfo message) throws IOException, SQLException {
		if (message.getContent().isBlank()) {
			sendCommand(Constants.ERROR);
			return;
		}
		
		if (message.getRecipientType().equals(MessageInfo.Recipient.CHANNEL)) {
			if (ServerChannelManager.insertMessage(userInfo.getUserId(), message.getRecipientId(), message.getContent()))
				sendCommand(Constants.SUCCESS);
			else
				sendCommand(Constants.ERROR, "Should not happen");
		} else {
			if (ServerUserManager.insertMessage(userInfo.getUserId(), message.getRecipientId(), message.getContent()))
				sendCommand(Constants.SUCCESS);
			else
				sendCommand(Constants.ERROR, "Should not happen");
		}
	}
	
	public void protocolChannelRemove(int channelId) throws SQLException, IOException {
		if (ServerChannelManager.isUserChannelOwner(userInfo.getUserId(), channelId)) {
			boolean success = ServerChannelManager.deleteChannel(channelId);
			if (success) sendCommand(Constants.SUCCESS);
			else sendCommand(Constants.ERROR, "Error Removing channel"); // Shouldn't happen
		} else sendCommand(Constants.ERROR, "User doesn't have permissions"); // Shouldn't happen
	}
	
	public void protocolChannelAdd(ChannelInfo info) throws IOException, SQLException, NoSuchAlgorithmException {
		boolean success = ServerChannelManager.createChannel(
				userInfo.getUserId(), info.getName(), info.getPassword(), info.getDescription());
		if (success) sendCommand(Constants.SUCCESS);
		else sendCommand(Constants.ERROR);
	}
	
	public void protocolChannelGetMessages(Ids ids) throws IOException, SQLException {
		if (!ServerChannelManager.isUserPartOf(userInfo.getUserId(), ids.getChannelId())) {
			sendCommand(Constants.NO_PERMISSIONS);
			return;
		}
		ArrayList<MessageInfo> channelMessages;
		if (ids.getMessageId() <= 0)
			channelMessages = ServerChannelManager.getChannelMessagesBefore(ids.getChannelId(), ServerConstants.DEFAULT_GET_MESSAGES_AMOUNT);
		else
			channelMessages = ServerChannelManager.getChannelMessagesBefore(ids.getChannelId(), ids.getMessageId(), ServerConstants.DEFAULT_GET_MESSAGES_AMOUNT);
		Utils.printList(channelMessages, "channelMessages");
		sendCommand(Constants.CHANNEL_GET_MESSAGES, channelMessages);
		
		currentChannelId = ids.getChannelId();
	}
	
	public void protocolChannelGetALl() throws SQLException, IOException {
		ArrayList<ChannelInfo> channels = ServerChannelManager.getChannels(userInfo.getUserId());
		sendCommand(Constants.CHANNEL_GET_ALL, channels);
	}
	
	private void logout() throws IOException {
		isLoggedIn = false;
		sendCommand(Constants.LOGOUT);
	}
	
	private void handleRegister(UserInfo userInfo) throws IOException {
		if (isLoggedIn()) {
			System.out.println("Illegal Request\tNot supposed to happen");
			sendCommand(Constants.INVALID_REQUEST);
			return;
		}
		try {
			System.out.println(userInfo);
			if (!Utils.checkUsername(userInfo.getUsername())) {
				sendCommand(Constants.REGISTER_ERROR, "Username doesn't follow rules (Between 3 and 25 characters and have no special characters and )");
				
			} else if (!Utils.checkPasswordFollowsRules(userInfo.getPassword())) {
				sendCommand(Constants.REGISTER_ERROR, "Password doesn't follow rules (needs 8 to 25 characters, a special character, a number and a upper and lower case letter)");
				
			} else if (!Utils.checkNameUser(userInfo.getName())) {
				sendCommand(Constants.REGISTER_ERROR, "Name is invalid (needs to be between 3 and 50 characters long)");
				
			} else if (!ServerUserManager.checkUsernameAvailability(userInfo.getUsername())) {
				sendCommand(Constants.REGISTER_ERROR, "Username already in use");
				
			} else {
				String imagePath = "";
				File imageFile = null;
				if (userInfo.getImageBytes() != null) {
					String folderPath = ServerConstants.FILES_PATH + File.separator +
							ServerMain.getInstance().getDatabaseName() + "_" + ServerConstants.USER_IMAGES_DIRECTORY;
					imagePath = folderPath + File.separator + userInfo.getUsername() + ".jpg";
					imageFile = new File(imagePath);
					Utils.createDirectories(imageFile);
					
					try (FileOutputStream fileOutputStream = new FileOutputStream(imageFile)) {
						fileOutputStream.write(userInfo.getImageBytes());
					}
				}
				if (ServerUserManager.insertUser(userInfo, imagePath)) {
					System.out.println("Added new user");
					sendCommand(Constants.REGISTER_SUCCESS);
				} else {
					if (imageFile != null)
						imageFile.delete();
					System.out.println("No new user added\t Not supposed to happen");
					sendCommand(Constants.REGISTER_ERROR, "No new user added");
				}
			}
		} catch (NoSuchAlgorithmException | SQLException e) {
			System.out.println("Error on User Registration : " + e.getMessage());
			e.printStackTrace();
			sendCommand(Constants.REGISTER_ERROR);
		}
	}
	
	private void login(String username, String password) throws SQLException, IOException, NoSuchAlgorithmException {
		if (isLoggedIn()) {
			sendCommand(Constants.LOGIN_ERROR, "Already Logged In");
			return;
		}
		if (!ServerUserManager.doesUsernameExist(username)) {
			sendCommand(Constants.LOGIN_ERROR, "Username does not exist");
			return;
		}
		if (!ServerUserManager.doesPasswordMatchUsername(username, password)) {
			sendCommand(Constants.LOGIN_ERROR, "Password is incorrect");
			return;
		}
		int userId = ServerUserManager.getUserId(username);
		String nameUser = ServerUserManager.getNameUser(userId);
		userInfo = new UserInfo(userId, username, nameUser);
		sendCommand(Constants.LOGIN_SUCCESS, userInfo);
		System.out.println("Login success : " + userInfo);
		isLoggedIn = true;
	}
	
	public void sendCommand(String command) throws IOException {
		sendCommand(command, null);
	}
	
	public void sendCommand(String command, Object extra) throws IOException {
		Command obj = new Command(command, extra);
		System.out.println(obj);
		oos.writeUnshared(obj);
	}
	
	public String getSocketInformation() {
		return ("local port: " + socket.getInetAddress().getHostName() + ":" + socket.getPort());
	}
	
	public boolean isLoggedIn() {
		return isLoggedIn;
	}
}
