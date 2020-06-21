package jcrystal.clients;

import java.util.TreeSet;

import jcrystal.json.JsonLevel;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityClass;
import jcrystal.types.JClass;

public abstract class AbsEntityValidator<T extends AbsClientGenerator<?>> {
	protected ClientContext context;
	protected T client;
	public AbsEntityValidator(T client) {
		this.context = client.context;
		this.client = client;
	}
	public abstract void create(EntityClass entidad, TreeSet<JsonLevel> levels);
	public void validateResult(JClass clase) {
		
	}
}
