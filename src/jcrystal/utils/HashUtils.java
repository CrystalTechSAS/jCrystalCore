package jcrystal.utils;

import java.security.NoSuchAlgorithmException;

public class HashUtils {

	public static String md5(String value) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(value.getBytes());
			return String.format("%02X", hash).toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public static String shortMD5(String value) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(value.getBytes());
			byte[] result = new byte[hash.length / 4];
			for(int e = 0; e < hash.length; e++)
				result[e / 4] += hash[e];
			String hastStr = "";
			for(byte b : result)
				hastStr += String.format("%02x", b); 
			return hastStr.toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
