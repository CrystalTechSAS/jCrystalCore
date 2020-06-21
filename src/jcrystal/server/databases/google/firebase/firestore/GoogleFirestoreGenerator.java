package jcrystal.server.databases.google.firebase.firestore;

import java.util.List;

import jcrystal.main.data.ClientContext;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.model.server.db.EntityClass;
import jcrystal.server.databases.AbsDBGenerator;

public class GoogleFirestoreGenerator extends AbsDBGenerator{
	MainEntityGenerator googleFirestore;
	public GoogleFirestoreGenerator(BackendWrapper back, ClientContext context, List<EntityClass> entities) {
		super(back, context, entities);
		googleFirestore = new MainEntityGenerator(context);
	}
	@Override
	public void generate() {
		for(EntityClass entity : entities)
			googleFirestore.generate(back, entity, false);
		for(EntityClass entity : getKeysOnlyEntities())
			googleFirestore.generate(back, entity, true);
	}
}
