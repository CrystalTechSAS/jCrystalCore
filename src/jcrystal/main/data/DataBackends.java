package jcrystal.main.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jcrystal.configs.server.Backend;
import jcrystal.model.server.db.EntityClass;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JType;

public class DataBackends {
	private final Map<String, BackendWrapper> BACKENDS = new TreeMap<>();
	public final BackendWrapper MAIN;
	public final List<BackendWrapper> LIST;
	public DataBackends(ClientInput input) {
		MAIN = new BackendWrapper();
		input.SERVER.BACKENDS.values().forEach(b->{
			BACKENDS.put(b.id, new BackendWrapper(b));
		});
		LIST = Collections.unmodifiableList(Arrays.asList(BACKENDS.values().toArray(new BackendWrapper[BACKENDS.size()])));
	}
	public BackendWrapper get(String id) {
		return BACKENDS.get(id);
	}
	public static class BackendWrapper{
		Backend back;
		public final String id;
		public final Set<IJType> entities = new TreeSet<>();
		public final List<EntityClass> entitiesList = new ArrayList<>();
		public final Set<JClass> enums = new TreeSet<>();
		private BackendWrapper() {
			this.id = null;
			this.back = null;
		}
		public BackendWrapper(Backend back) {
			this.id = back.id;
			this.back = back;
		}
		public void register(EntityClass entity) {
			if(entities.add(entity.clase))
				entitiesList.add(entity);

			entity.fields.stream().forEach(f->{
				if(f.type().isEnum())
					addEnum(f.type());
				else f.type().getInnerTypes().stream().filter(k->k.isEnum()).forEach(k->addEnum(k));
			});
		}
		public boolean checkExposed(EntityClass entity) {
			return entities.contains(entity.clase);
		}
		public void addEnum(IJType type) {
			JClass enumClass = type.tryResolve();
			if(enumClass != null)
				enums.add(enumClass);
			
		}
		
		
	}
}
