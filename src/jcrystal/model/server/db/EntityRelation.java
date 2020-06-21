package jcrystal.model.server.db;

import java.util.List;
import java.util.Map;

import jcrystal.json.JsonLevel;
import jcrystal.main.data.ClientContext;
import jcrystal.reflection.annotations.IndexType;
import jcrystal.reflection.annotations.entities.Rel1to1;
import jcrystal.reflection.annotations.entities.RelMto1;
import jcrystal.reflection.annotations.entities.RelMtoM;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;

public class EntityRelation implements TypedField{
	public final EntityField field;
	public JsonLevel level = JsonLevel.NONE;
	public boolean editable = false;
	public String fieldName, dbName, targetName;
	EntityClass owner;
	EntityClass target;
	/**
	* Only for M2M relations
	*/
	public boolean smallCardinality;
	public EntityRelation(EntityClass definer, EntityField field) {
		owner = definer;
		this.field = field;
		fieldName = field.name();
		dbName = field.dbName;
		if(field.isAnnotationPresent(RelMto1.class)) {
			if(!field.getAnnotation(RelMto1.class).name().isEmpty())
				dbName = field.getAnnotation(RelMto1.class).name();
			targetName = field.getAnnotation(RelMto1.class).target();
			editable = field.getAnnotation(RelMto1.class).editable();
			level = field.getAnnotation(RelMto1.class).keyLevel();
		}
		if(field.isAnnotationPresent(Rel1to1.class)) {
			if(!field.getAnnotation(Rel1to1.class).name().isEmpty())
				dbName = field.getAnnotation(Rel1to1.class).name();
			targetName = field.getAnnotation(Rel1to1.class).target();
		}
		if(field.isAnnotationPresent(RelMtoM.class)) {
			dbName = field.getAnnotation(RelMtoM.class).value();
			targetName = field.getAnnotation(RelMtoM.class).target();
			smallCardinality = field.getAnnotation(RelMtoM.class).small();
			level = field.getAnnotation(RelMtoM.class).keyLevel();
		}
		if(targetName==null || targetName.isEmpty())
			targetName = null;
	}
	public IJType type(){
		return field.type();
	}
	public EntityClass getOwner() {
		return owner;
	}
	public EntityClass loadTarget(ClientContext context){
		if(target == null) {
			if(field.type().isSubclassOf(List.class)) {
				final IJType tipoParamero = field.type().getInnerTypes().get(0);
				target = context.data.entidades.get(tipoParamero);
			}else
				target = context.data.entidades.get(field.type());
			if(target == null)
				throw new NullPointerException("Unrecognized target entity ("+owner.name()+"): " + field.type());
		}return target;
	}
	public EntityClass getTarget() {
		return target;
	}
	public IndexableField getOwnerIndexableField() {
		return new IndexableField() {
			@Override
			public IndexType indexType() {
				return field.isAnnotationPresent(Rel1to1.class) ? IndexType.UNIQUE : IndexType.NONE;
			}
			@Override
			public IJType getIndexType() {
				return target.key.getSingleKeyType();
			}
			
			@Override
			public String fieldName() {
				return fieldName;
			}
			
			@Override
			public String getDBName() {
				return dbName;
			}
			
			@Override
			public IJType type() {
				return target.clase;
			}

			@Override
			public String name() {
				return fieldName;
			}
			@Override
			public Map<String, JAnnotation> getAnnotations() {
				return field.getAnnotations();
			}
			@Override
			public JAnnotation getJAnnotationWithAncestorCheck(String name) {
				throw new NullPointerException();
			}
		};
	}
	
	@Override
	public String name() {
		return fieldName;
	}
	public String fieldName() {
		return fieldName;
	}
}
