package jcrystal.main.data;

import java.util.TreeSet;

import jcrystal.configs.clients.ClientConfig;
import jcrystal.configs.server.ServerConfig;
import jcrystal.types.loaders.JClassLoader;

public class ClientInput {
	public ServerConfig SERVER;
	public ClientConfig CLIENT;
	public TreeSet<String> CHECKED_CLASSES = new TreeSet<>();
	public JClassLoader jClassResolver;
	
	public boolean contains(Class<?> clase) {
		return CHECKED_CLASSES.contains(clase.getName());
	}
	public boolean contains(String clase) {
		return CHECKED_CLASSES.contains(clase);
	}
	
}
