package jcrystal.clients;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.types.IJType;
import jcrystal.utils.DefaultTreeMap;

public class FieldTreeMap{
	
	public final TreeMap<JsonLevel, List<EntityField>> fields = new TreeMap<>(JsonLevel.nullComparator);
	
	public FieldTreeMap() {
		fields.put(null, new ListSet());
	}
	public void add(JsonLevel antLevel, EntityClass entity, EntityField field) {
		if(antLevel != null)
			for (final JsonLevel level : entity.getUsedLevels()) {
				if (antLevel.level <= level.level) {
					List<EntityField> list = fields.computeIfAbsent(level, l->new ListSet()); 
					list.add(field);
				}
			}
		else
			fields.get(null).add(field);
	}
	public void mergeFrom(FieldTreeMap source) {
		for (Map.Entry<JsonLevel, List<EntityField>> ent : source.fields.entrySet()) {
			fields.computeIfAbsent(ent.getKey(), l->new ListSet());
		}
		if(source.fields.containsKey(null))
			fields.get(null).addAll(source.fields.get(null));
		List<EntityField> last = Collections.EMPTY_LIST;
		for (JsonLevel level : JsonLevel.managedValues) {
			if(source.fields.containsKey(level))
				fields.get(level).addAll(last = source.fields.get(level));
			else if(fields.containsKey(level))
				fields.get(level).addAll(last);
		}
	}
	
	public List<EntityField> get(JsonLevel level){
		return fields.get(level);
	}
	public List<EntityField> last(){
		return fields.lastEntry().getValue();
	}
	
	private static class ListSet extends ArrayList<EntityField>{
		private static final long serialVersionUID = 5617839716587790979L;
		private TreeSet<String> elements = new TreeSet<>();
		@Override
		public boolean add(EntityField e) {
			if(!elements.contains(e.name()))
				super.add(e);
			return elements.add(e.name());
		}
		@Override
		public boolean addAll(Collection<? extends EntityField> c) {
			for(EntityField f : c)
				add(f);
			return true;
		}
	}
}
