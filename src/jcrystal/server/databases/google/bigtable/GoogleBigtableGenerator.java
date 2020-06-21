package jcrystal.server.databases.google.bigtable;

import java.util.List;

import jcrystal.main.data.ClientContext;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.model.server.db.EntityClass;
import jcrystal.server.databases.AbsDBGenerator;

public class GoogleBigtableGenerator extends AbsDBGenerator{
	GeneradorGoogleBigtable googleBigtable;
	public GoogleBigtableGenerator(BackendWrapper back, ClientContext context, List<EntityClass> entities) {
		super(back, context, entities);
		googleBigtable = new GeneradorGoogleBigtable(context);
	}
	@Override
	public void generate() {
		for(EntityClass entity : entities)
			googleBigtable.generate(back, entity, false);
		for(EntityClass entity : getKeysOnlyEntities())
			googleBigtable.generate(back, entity, true);
	}
}
