package jcrystal.server.databases;

import java.util.List;
import java.util.TreeSet;

import jcrystal.main.data.ClientContext;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.model.server.db.EntityClass;

public abstract class AbsDBGenerator {
	protected final ClientContext context;
	protected final List<EntityClass> entities;
	protected final BackendWrapper back;
	public AbsDBGenerator(BackendWrapper back, ClientContext context, List<EntityClass> entities) {
		this.context = context;
		this.entities = entities;
		this.back = back;
	}
	public abstract void generate();
	protected TreeSet<EntityClass> getKeysOnlyEntities(){
		TreeSet<EntityClass> llaves = new TreeSet<>();
		for(EntityClass entidad : entities)
			if(entidad.key != null)
				entidad.key.stream().map(f->f.getTargetEntity()).filter(f->f != null).forEach(llaves::add);
		for(EntityClass entidad : entities)
			llaves.remove(entidad);
		return llaves;
	}
}
