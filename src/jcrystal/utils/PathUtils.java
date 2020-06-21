package jcrystal.utils;

public class PathUtils {

	public static String root(String path) {
		if(path.startsWith("/"))
			return path;
		return "/"+path;
	}
	public static String concat(String path1,String path2) {
		if(path2.startsWith("/") || path1.endsWith("/"))
			return path1 + path2;
		return path1 + "/" + path2;
	}
}
