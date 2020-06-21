package jcrystal.main.data;

import jcrystal.types.loaders.IJClassLoader;

public class ClientContext {
	
	public final ClientInput input; 
	public final ClientData data;
	public final ClientUtils utils;
	public final ClientOutput output;
	public final ClientBackData back;
	
	public final IJClassLoader jClassLoader;
	public int ADMIN_ID_GENERATOR = 1;
	public ClientContext(ClientBackData back, ClientInput clientInput, ClientOutput clientOutput) {
		this.back = back;
		this.input = clientInput;
		this.output = clientOutput;
		this.utils = new ClientUtils(this);
		this.jClassLoader = input.jClassResolver;
		this.data = new ClientData(clientInput);
	}
	
}
