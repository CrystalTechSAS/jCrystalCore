package jcrystal.server;

import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityClass;

public class GeneradorJSF {
	
	private ClientContext context;
	
	private GeneradorJSFjCrystalSession generadorJSFjCrystalSession;
	
	private GeneradorJSFValidators generadorJSFValidators;
	
	public GeneradorJSF(ClientContext context) {
		this.context = context;
		generadorJSFjCrystalSession = new GeneradorJSFjCrystalSession(context);
		generadorJSFValidators = new GeneradorJSFValidators(context);
	}

	public void gen(){
		if(context.input.SERVER.WEB.isEnableJSF()){
			System.out.println("Generando JSFs");
			generadorJSFjCrystalSession.gen();
			for(EntityClass entidad : context.data.entidades.list.values())
				generadorJSFValidators.gen(entidad);
		}
	}
}
