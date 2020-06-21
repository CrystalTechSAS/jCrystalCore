package jcrystal.utils;

public class NativeTypeUtils {
	public static Class<?> getObjectClass(Class<?> c){
		if(c == int.class)
			return Integer.class;
		if(c == long.class)
			return Long.class;
		if(c == boolean.class)
			return Boolean.class;
		if(c == double.class)
			return Double.class;
		return c;
	}
}
