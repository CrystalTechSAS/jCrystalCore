package jcrystal.clients.webadmin;

import jcrystal.model.web.JCrystalWebService;

public class AdminWsDescriptor {
	
	public JCrystalWebService ws;
	public JCrystalWebService source;
	public AdminWsDescriptor() {
		
	}
	public AdminWsDescriptor setSource(JCrystalWebService source) {
		this.source = source;
		return this;
	}
	public AdminWsDescriptor setWs(JCrystalWebService ws) {
		this.ws = ws;
		return this;
	}
}
