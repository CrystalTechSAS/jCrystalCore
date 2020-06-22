package jcrystal.main.data;

import java.util.Random;

import jcrystal.types.loaders.IJClassLoader;

public class ClientContext {
	
	public final ClientInput input; 
	public final ClientData data;
	public final ClientUtils utils;
	public final IClientOutput output;
	public final Random random = new Random();
	public final IJClassLoader jClassLoader;
	
	public int ADMIN_ID_GENERATOR = 1;
	
	public ClientContext(ClientInput clientInput, IClientOutput clientOutput) {
		this.input = clientInput;
		this.output = clientOutput;
		this.utils = new ClientUtils(this);
		this.jClassLoader = input.jClassResolver;
		this.data = new ClientData(clientInput);
	}
	
}
