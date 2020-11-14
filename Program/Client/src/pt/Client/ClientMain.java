package pt.Client;

import javafx.application.Platform;
import pt.Common.*;
import pt.Common.MessageInfo.Recipient;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ClientMain {
	
	private InetAddress serverIPAddress;
	private int portUDPServer;
	private Socket socket;
	private static ObjectOutputStream oOS;
	private static ObjectInputStream oIS;
	private ArrayList<ServerAddress> serversList;
	private static ClientMain instance;
	private ArrayList<ChannelInfo> channels;
	private ArrayList<UserInfo> users;
	private UserInfo userInfo;
	private File userPhoto;
	//private ChannelInfo currentChannel;
	//private UserInfo currentUser;
	private ApplicationController applicationController;
	
	private ArrayList<MessageInfo> messages = null;
	private MessageInfo messageTemplate = null;
	
	
	public static ClientMain getInstance() {
		return instance;
	}
	
	public ClientMain(String ipServer, int port) throws Exception {
		if (instance != null) {
			throw new Exception("Client Main Exists");
		}
		instance = this;
		this.serverIPAddress = InetAddress.getByName(ipServer);
		this.portUDPServer = port;
	}
	
	public void connectToServer() throws Exception {
		DatagramSocket datagramSocket = new DatagramSocket();
		
		while (true) {
			System.out.println(serverIPAddress + ":" + portUDPServer);
			boolean success = tryConnectServer(serverIPAddress, portUDPServer, datagramSocket);
			if (success) {
				
				return;
			} else {
				ServerAddress serverAddress = serversList.get(0);
				serverIPAddress = serverAddress.getAddress();
				portUDPServer = serverAddress.getUDPPort();
			}
		}
	}
	
	private boolean tryConnectServer(InetAddress ipAddress, int port, DatagramSocket udpSocket) throws Exception {
		Command command = new Command(Constants.ESTABLISH_CONNECTION);
		UDPHelper.sendUDPObject(command, udpSocket, ipAddress, port);
		
		udpSocket.setSoTimeout(2000);
		command = (Command) UDPHelper.receiveUDPObject(udpSocket);
		String protocol = command.getProtocol();
		
		if (protocol.equals(Constants.CONNECTION_ACCEPTED)) {
			int socketTCPort = (int) command.getExtras();
			socket = new Socket(ipAddress, socketTCPort);
			oOS = new ObjectOutputStream(socket.getOutputStream());
			oIS = new ObjectInputStream(socket.getInputStream());
			
			
			command = (Command) oIS.readObject();
			if (!command.getProtocol().equals(Constants.SERVERS_LIST)) {
				throw new Exception("Should not happen");
			}
			serversList = (ArrayList<ServerAddress>) command.getExtras();
			
			//----------------------------------------------------------------------------------------------------------
			receiver = new Receiver(oIS);
			receiver.start();
			
			return true;
		} else if (protocol.equals(Constants.CONNECTION_REFUSED)) {
			// TODO Garantir que recebe
			serversList = (ArrayList<ServerAddress>) command.getExtras();
			return false;
		} else {
			throw new IOException("Illegal Connection Protocol");
		}
	}
	
	private Receiver receiver;
	
	public Object sendCommandToServer(String protocol, Object object) throws IOException, InterruptedException {
		Command command = new Command(protocol, object);
		oOS.writeObject(command);
		
		Object ob = receiveCommand();
		System.out.print("Sent : " + command + "\n\t");
		return ob;
	}
	
	public Object receiveCommand() throws InterruptedException {
		Waiter waiter = new Waiter();
		System.out.println("stopped to wait");
		receiver.waitForCommand(waiter);
		System.out.println("Received something");
		
		Command ob = (Command) waiter.getResult();
		//Object ob = oIS.readObject();
		System.out.println("Received : " + ob);
		return ob;
	}
	
	public boolean logout() throws IOException, ClassNotFoundException, InterruptedException {
		Command command = (Command) sendCommandToServer(Constants.LOGOUT, null);
		// wait for response
		return true;
	}
	
	public UserInfo getUserInfo() {
		return userInfo;
	}
	
	public void setUserInfo(UserInfo userInfo) {
		this.userInfo = userInfo;
	}
	
	public ChannelInfo getChannelByName(String name) {
		for (var channel : channels) {
			if (name.equals(channel.getName())) {
				return channel;
			}
		}
		return null;
	}
	
	public UserInfo getUserByUsername(String name) {
		System.out.println(users);
		for (var user : users) {
			if (name.equals(user.getUsername())) {
				return user;
			}
		}
		return null;
	}
	
	public ArrayList<MessageInfo> getMessagesFromChannel(int id) throws IOException, ClassNotFoundException, InterruptedException {
		Command command = (Command) sendCommandToServer(Constants.CHANNEL_GET_MESSAGES, new Ids(-1, id, -1));
		return (ArrayList<MessageInfo>) command.getExtras();
	}
	
	public ArrayList<ChannelInfo> getChannels() {
		return channels;
	}
	
	public void setChannels(ArrayList<ChannelInfo> channels) {
		this.channels = channels;
	}
	
	public File getUserPhoto() {
		return userPhoto;
	}
	
	public void setUserPhoto(File userPhoto) {
		this.userPhoto = userPhoto;
	}
	
	public void sendFile(File file) throws IOException, InterruptedException {
		
		Recipient recipientType = messages.get(0).getRecipientType();
		int recipientId = messages.get(0).getRecipientId();
		
		MessageInfo message = new MessageInfo(recipientType, recipientId, MessageInfo.TYPE_FILE, file.getName());
		
		Command command = (Command) sendCommandToServer(Constants.ADD_FILE, message);
		if (!command.getProtocol().equals(Constants.FILE_ACCEPT_CONNECTION)) {
			System.err.println("Error File not  Success before send");
			return;
		}
		
		Thread thread = new Thread(() -> {
			try {
				int fileTransferPort = (int) command.getExtras();
				Socket socket = new Socket(serverIPAddress, fileTransferPort);
				
				OutputStream outputStream = socket.getOutputStream();
				try (FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath())) {
					
					byte[] buffer = new byte[Constants.CLIENT_FILE_CHUNK_SIZE];
					while (true) {
						int readAmount = fileInputStream.read(buffer);
						if (readAmount == -1) { /* Reached the end of file */
							outputStream.close();
							socket.close();
							break;
						}
						outputStream.write(buffer, 0, readAmount);
					}
				}
				Command newNameCommand = (Command) receiveCommand();
				String newFileName = (String) newNameCommand.getExtras();
				message.setContent(newFileName);
				Platform.runLater(() -> ApplicationController.get().addMessageToScreen(message));
			} catch (IOException  | InterruptedException e) {
			}
		});
		thread.start();
	}
	
	public ArrayList<UserInfo> getUsers() {
		return users;
	}
	
	public void setUsers(ArrayList<UserInfo> users) {
		this.users = users;
	}
	
	public ApplicationController getApplicationController() {
		return applicationController;
	}
	
	public ArrayList<ChannelInfo> getChannelsFromServer() throws IOException, ClassNotFoundException, InterruptedException {
		Command command = (Command) sendCommandToServer(Constants.CHANNEL_GET_ALL, null);
		ArrayList<ChannelInfo> list = (ArrayList<ChannelInfo>) command.getExtras();
		channels = list;
		return channels;
	}
	
	public int getMessagesRecipientId() {
		return messageTemplate.getRecipientId();
	}
	
	public Recipient getMessagesRecipientType() {
		return messageTemplate.getRecipientType();
	}
	
	public ArrayList<MessageInfo> getMessages() {
		return messages;
	}
	
	public void setMessages(ArrayList<MessageInfo> messages) {
		this.messages = messages;
	}
	
	public void defineMessageTemplate(Recipient recipientType, int recipientId) {
		messageTemplate = new MessageInfo(recipientType, recipientId);
	}
	
	public void waitForMessage() {
	
	}
}