package pt.Common;

import pt.Server.ServerConstants;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	public static String hashStringBase36(String str) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(str.getBytes());
		return new BigInteger(1, hash).toString(36);
	}
	
	public static boolean checkPasswordFollowsRules(String password) {
		Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,25}$");
		Matcher matcher = pattern.matcher(password);
		return matcher.find();
	}
	
	public static boolean checkNameUser(String name) {
		return name.length() > 3 && name.length() < 50;
	}
	
	public static boolean checkUsername(String username) {
		Pattern pattern = Pattern.compile("^[a-zA-Z\\d_]{3,25}$");
		Matcher matcher = pattern.matcher(username);
		return matcher.find();
	}
	
	public static BufferedImage getCompressedImage(BufferedImage img, int newW, int newH) {
		/*if (img.getColorModel().hasAlpha()) {
			BufferedImage newImage = new BufferedImage(
					img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
			newImage.createGraphics().drawImage(img, 0, 0, Color.WHITE, null);
			img = newImage;
		}*/
		//TODO make this work
		Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		BufferedImage bufferedImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g2d = bufferedImage.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();
		return bufferedImage;
	}
	
	public static void printList(ArrayList list) {
		printList(list, "");
	}
	
	public static void printList(ArrayList list, String prefix) {
		System.out.println(prefix + "----------------------------");
		for (Object asd : list) System.out.println(asd);
		System.out.println("----------------------------");
	}
	
	public static InetAddress getPublicIp() {
		HttpURLConnection connection = null;
		BufferedReader in = null;
		try {
			URL url = new URL(ServerConstants.PUBLIC_IP_ADDRESS_API);
			connection = (HttpURLConnection) url.openConnection();
			int responseCode = connection.getResponseCode();
			
			if (responseCode == HttpURLConnection.HTTP_OK) {
				in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String publicIp = in.readLine();
				in.close();
				return InetAddress.getByName(publicIp);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) connection.disconnect();
		}
		return null;
	}
}