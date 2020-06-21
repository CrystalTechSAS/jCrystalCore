package jcrystal.model.clients;

import java.util.Map;
import java.util.TreeMap;

import jcrystal.configs.clients.Client;
import jcrystal.configs.clients.IInternalConfig;
import jcrystal.types.IJType;

public class ClientData {

	public Client client;
	
	public Map<String, IInternalConfig> configs = new TreeMap<>();
	
	public IJType annotationClass;
}
