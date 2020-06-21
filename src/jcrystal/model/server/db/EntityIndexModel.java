package jcrystal.model.server.db;

import jcrystal.reflection.annotations.EntityIndex;
import jcrystal.reflection.annotations.EntityIndexes;
import jcrystal.reflection.annotations.IndexType;
import jcrystal.types.JAnnotation;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
* Created by gasotelo on 2/7/17.
*/
public class EntityIndexModel {
	EntityClass entidad;
	private String nombre = null;
	public final IndexableField[] camposIndice;
	
	public EntityIndexModel(String name, EntityClass entidad, IndexableField[] camposIndice) {
		this.nombre = name;
		this.entidad = entidad;
		this.camposIndice = camposIndice;
	}
	public String name(){
		if(nombre != null)
			return nombre;
		StringSeparator builder = new StringSeparator("_");
		for(IndexableField f : camposIndice){
			builder.add(StringUtils.capitalize(f.fieldName()));
		}
		return builder.toString();
	}
	public static List<EntityIndexModel> getEntityIndexes(EntityClass entidad){
		List<EntityIndexModel> indices = new ArrayList<>();
		if(entidad.clase.isAnnotationPresent(EntityIndexes.class))
			for(EntityIndex index : entidad.clase.getAnnotation(EntityIndexes.class).value())
				indices.add(getIndex(entidad, index.name(), index.value()));
		else if(entidad.clase.isAnnotationPresent(EntityIndex.class)) {
			EntityIndex index = entidad.clase.getAnnotation(EntityIndex.class);
			indices.add(getIndex(entidad, index.name(), index.value()));
		}
		if(entidad.clase.ifJAnnotation(entidad.clase.prefixName("Meta")+"$Indexes", indexes -> {
			for(JAnnotation index : (JAnnotation[])indexes.values.get("value"))
				indices.add(getIndex(entidad, (String)index.values.get("name"), (String[])index.values.get("fields")));
		}));
		else if(entidad.clase.ifJAnnotation(entidad.clase.prefixName("Meta")+"$Index", index -> {
			indices.add(getIndex(entidad, (String)index.values.get("name"), (String[])index.values.get("fields")));
		}));
		return indices;
	}
	private static EntityIndexModel getIndex(EntityClass entidad, String name, String[] fields) {
		List<IndexableField> campos = new ArrayList<>();
		ciclo:for(String c : fields){
			for(EntityField f : entidad.properties)
				if(c.equals(f.fieldName())) {
					if(f.indexType == IndexType.NONE)
						throw new NullPointerException("Field " + c +" must be indexed to be included on index " + name + " on entity " + entidad.name());
					campos.add(f);
					continue ciclo;
				}
			for(EntityRelation r : entidad.ownedOneToOneRelations)if(c.equals(r.fieldName)) {
				campos.add(r.getOwnerIndexableField());
				continue ciclo;
			}
			for(EntityRelation r : entidad.manyToOneRelations)if(c.equals(r.fieldName)) {
				campos.add(r.getOwnerIndexableField());
				continue ciclo;
			}
			throw new NullPointerException("Undefined field " + c);
		}
		return new EntityIndexModel(name == null || name.isEmpty() ? null : name, entidad, campos.toArray(new IndexableField[0]));
	}
}
