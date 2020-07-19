package jcrystal.server.databases.google.firebase.realtimedb;

import java.util.List;

import jcrystal.main.data.ClientContext;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.model.server.db.EntityClass;
import jcrystal.server.databases.AbsDBGenerator;

public class GoogleRealtimeDBGenerator extends AbsDBGenerator{
	MainEntityGenerator googleRealtime;
	public GoogleRealtimeDBGenerator(BackendWrapper back, ClientContext context, List<EntityClass> entities) {
		super(back, context, entities);
		googleRealtime = new MainEntityGenerator(context);
	}
	@Override
	public void generate() {
		for(EntityClass entity : entities)
			googleRealtime.generate(back, entity, false);
		for(EntityClass entity : getKeysOnlyEntities())
			googleRealtime.generate(back, entity, true);
	}
}
