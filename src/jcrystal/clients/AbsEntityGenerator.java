package jcrystal.clients;

import java.util.TreeSet;

import jcrystal.json.JsonLevel;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityClass;

public abstract class AbsEntityGenerator<T extends AbsClientGenerator<?>> {
	final protected T client;
	final protected ClientContext context;
	
	public AbsEntityValidator<T> entityValidatorGenerator;
	
	public AbsEntityGenerator(T client) {
		this.client = client;
		this.context = client.context;
	}
	public abstract void generateEntity(final EntityClass entidad, TreeSet<JsonLevel> levels);
	public abstract void generateEntityKey(final EntityClass entidad);
	//public abstract void generateEntityKey(final ClaseEntidad entidad);
}
