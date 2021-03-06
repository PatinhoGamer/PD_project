package pt.Server.DataHolders;

import pt.Common.ServerAddress;

import java.io.Serializable;

public class ServerCommand implements Serializable {
	
	private static final long serialVersionUID = 54307128L;
	
	private String protocol;
	private ServerAddress serverAddress;
	private Object extras;
	
	public ServerCommand(String protocol, ServerAddress serverAddress, Object extras) {
		this.protocol = protocol;
		this.serverAddress = serverAddress;
		this.extras = extras;
	}
	
	public ServerCommand(String protocol, ServerAddress serverAddress) {
		this.protocol = protocol;
		this.serverAddress = serverAddress;
	}
	
	@Override
	public String toString() {
		return "ServerCommand{" +
				"protocol='" + protocol + '\'' +
				", serverAddress=" + serverAddress +
				", extras=" + extras +
				'}';
	}
	
	public ServerAddress getServerAddress() {
		return serverAddress;
	}
	
	public void setServerAddress(ServerAddress serverAddress) {
		this.serverAddress = serverAddress;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	public Object getExtras() {
		return extras;
	}
	
	public void setExtras(Object extras) {
		this.extras = extras;
	}
	
}
