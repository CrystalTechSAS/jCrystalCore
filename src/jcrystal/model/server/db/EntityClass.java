package jcrystal.model.server.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jcrystal.configs.server.dbs.DBInstance;
import jcrystal.json.JsonLevel;
import jcrystal.main.data.ClientContext;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.JClass;
import jcrystal.types.JMethod;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.entities.CarbonCopy;
import jcrystal.reflection.annotations.entities.OnConstruct;
import jcrystal.reflection.annotations.entities.PreWrite;
import jcrystal.reflection.annotations.entities.Rel1to1;
import jcrystal.reflection.annotations.entities.RelMto1;
import jcrystal.reflection.annotations.entities.RelMtoM;
import jcrystal.utils.StringSeparator;

public class EntityClass implements Comparable<EntityClass>{
	
	
	public final EntityClass padre;
	public final JClass clase;
	public List<EntityClass> hijos = new ArrayList<>();
	
	public EntityKeyModel key;
	/*
	 * All fields, properties, keys and relations
	 */
	public List<EntityField> fields = new ArrayList<>();
	public ArrayList<EntityField> properties = new ArrayList<>();
	/**
	 * Fields thar are not DB related. Eg: client attributes
	 */
	public ArrayList<EntityField> otherFields = new ArrayList<>();
	
	public ArrayList<JMethod> preWriteMethods = new ArrayList<>();
	public ArrayList<JMethod> onConstructMethods = new ArrayList<>();
	
	
	public jEntity entidad;
	public final List<EntityIndexModel> indices;
	public EntityField campoSelector;
	/**
	* Those relation in which this entity defines the relation. Definer side is the owner
	*/
	public List<EntityRelation> manyToOneRelations= new ArrayList<>();
	/**
	* Those relation in which this entity defines a oneToOne relation. This side is the owner
	*/
	public List<EntityRelation> ownedOneToOneRelations= new ArrayList<>();
	/**
	* Many to many relations. There is no owner side.
	*/
	public List<EntityRelation> manyToManyRelations= new ArrayList<>();
	
	/**
	* Those relation in which this entity is pointed by an OneToOne relation. Owner is the other side.
	*/
	public List<EntityRelation> targetedOneToOneRelations= new ArrayList<>();
	/**
	* Those relation in which this entity is pointed by an manyToOne relation. Owner is the many side.
	*/
	public List<EntityRelation> oneToManyRelations= new ArrayList<>();
	
	public DBInstance mainDBType;
	
	public final List<DBInstance> allDBType = new ArrayList<DBInstance>();
	
	private EnumSet<JsonLevel> usedLevels;
	/**
	 * Determina si esta entidad tiene campos que sirven para autenticacion
	 */
	public boolean hasAccountFields;
	private final boolean isSecurityToken;
	public EntityClass(ClientContext context, JClass clase, EntityClass padre) {
		this.padre = padre;
		if(this.padre != null)
			this.padre.hijos.add(this);
		this.clase = clase;
		
		entidad = clase.getAnnotation(jEntity.class);
		isSecurityToken = context.data.tokens.containsKey(clase);
		
		calculateDBReference:{
			JAnnotation dbreference = clase.getJAnnotation("jcrystal.annotations.db.DBReference");
			if(dbreference != null) {
				String mainDB = (String)dbreference.values.get("main");
				mainDBType = context.data.databases.get(mainDB.equals("DEFAULT") ? null : mainDB);
				String[] allDBS = (String[])dbreference.values.get("dbs");
				Stream.concat(Arrays.stream(allDBS), Stream.of(mainDB)).distinct().map(db -> context.data.databases.get(db.equals("DEFAULT") ? null : db)).forEach(allDBType::add);
			}else
				mainDBType = context.data.databases.get(null);
		}
		
		fields = clase.attributes.stream().map(f->new EntityField(f, mainDBType.type)).collect(Collectors.toList());
		key = new EntityKeyModel(this, fields);
		clase.methods.stream().filter(f->f.isAnnotationPresent(PreWrite.class)).forEach(preWriteMethods::add);
		clase.methods.stream().filter(f->f.isAnnotationPresent(OnConstruct.class)).forEach(onConstructMethods::add);
		preWriteMethods.stream().filter(f->!f.params.isEmpty()).forEach(f->{throw new NullPointerException("Invalid parameters for "+f.name());});
		
		for (EntityField campo : fields) {
			boolean used = false;
			if (campo.isEntityProperty) {
				used = true;
				properties.add(campo);
			}
			if (campo.f.isAnnotationPresent(RelMto1.class)) {
				used = true;
				manyToOneRelations.add(new EntityRelation(this, campo));
			}
			if (campo.f.isAnnotationPresent(Rel1to1.class)) {
				used = true;
				ownedOneToOneRelations.add(new EntityRelation(this, campo));
			}
			if (campo.f.isAnnotationPresent(RelMtoM.class)) {
				used = true;
				manyToManyRelations.add(new EntityRelation(this, campo));
			}
			if(campo.keyData == null && !used)
				otherFields.add(campo);
		}
		hasAccountFields = properties.stream().anyMatch(f->f.isAccountField) || (padre != null && padre.hasAccountFields);
		
		for(EntityField f : fields)
			if(f.isSelector)
				campoSelector = f;
		indices = EntityIndexModel.getEntityIndexes(this);
	
		if(!entidad.internal())
			key.configureParentKey(padre);
		if(key.llaves.isEmpty())
			key = null;
			
		validate();
	}
	public EnumSet<JsonLevel> getUsedLevels() {
		if(usedLevels == null) {
			usedLevels = EnumSet.noneOf(JsonLevel.class);
			for(EntityField field : fields)
				if(field.level != null && field.level.autoManaged)
					usedLevels.add(field.level);
			if(padre != null)
				usedLevels.addAll(padre.getUsedLevels());
		}
		return usedLevels;
	}
	private final void validate(){
		TreeSet<String> validador = new TreeSet<String>() {
			@Override
			public boolean add(String e) {
				if(!super.add(e))
					throw new NullPointerException("Identificador duplicado " + e +" en entidad " + entidad.name());
				return true;
			}
		};
		properties.stream().map(f->f.dbName).forEach(validador::add);
		properties.forEach(f->{
			IJType tipo = f.type();
			if(f.type().is(Map.class)) {
				IJType keyType = tipo.getInnerTypes().get(0);
				IJType valueType = tipo.getInnerTypes().get(1);
				if(!keyType.is(String.class))
					throw new NullPointerException(tipo.name() + ": " + tipo+" : Invalid key type for Map property");
				
				if(!valueType.is(String.class) && !valueType.isPrimitiveObjectType())
					throw new NullPointerException(tipo.name() + ": " + tipo+" : Invalid value type for Map property");
			}
			
		});
		manyToOneRelations.stream().map(f->f.dbName).forEach(validador::add);
		ownedOneToOneRelations.stream().map(f->f.dbName).forEach(validador::add);
		manyToManyRelations.stream().map(f->f.dbName).forEach(validador::add);
		if(entidad.internal() && key != null)
			throw new NullPointerException("An internal entity can't have defined keys: " + key.llaves.stream().map(f->f.fieldName()).collect(Collectors.joining(", ")));
	}
	public final boolean isSecurityToken(){
		return isSecurityToken;
	}
	
	public String name() {
		if (entidad.useParentName() && padre != null && padre.clase.isAnnotationPresent(jEntity.class)) {
			return padre.name();
		}
		return (entidad.name().isEmpty() ? clase.getSimpleName() : entidad.name());
	}
	
	public String getTipo() {
		return clase.getSimpleName();
	}
	public void doPostCreateProcessing(ClientContext context) {
		fields.stream().filter(f->f.type().isAnnotationPresent(jEntity.class)).forEach(f->{
			EntityClass target = context.data.entidades.get(f.type());
			f.setTargetEntity(target);
		});
		fields.stream().filter(f->f.type().isIterable() && f.type().getInnerTypes().get(0).isAnnotationPresent(jEntity.class)).forEach(f->{
			EntityClass target = context.data.entidades.get(f.type().getInnerTypes().get(0));
			f.setTargetEntity(target);
		});
		clase.ifAnnotation(CarbonCopy.class, carbon->{
			EntityClass entidadSrc = context.data.entidades.get(context.input.jClassResolver.forName(carbon.value()));
			properties.addAll(entidadSrc.properties);
			fields.addAll(entidadSrc.properties);
		});
		for(EntityRelation rel : ownedOneToOneRelations) {
			EntityClass target = rel.loadTarget(context);
			target.targetedOneToOneRelations.add(rel);
		}
		for(EntityRelation rel : manyToOneRelations) {
			EntityClass target = rel.loadTarget(context);
			target.oneToManyRelations.add(rel);
		}
		for(EntityRelation rel : manyToManyRelations) {
			if(rel.smallCardinality) {
				if(rel.field.getAnnotation(RelMtoM.class).keyLevel() != JsonLevel.NONE && !key.isSimple())
					throw new NullPointerException("Invalid json level for compound entity key ("+entidad.name()+":"+rel.fieldName+")");
			}else if(rel.field.getAnnotation(RelMtoM.class).keyLevel() != JsonLevel.NONE)
				throw new NullPointerException("Cannot have a non small M2M relation with json level ("+entidad.name()+":"+rel.fieldName+")");
			EntityClass target = rel.loadTarget(context);
			if(target != this)
				target.manyToManyRelations.add(rel);
			if(!rel.smallCardinality) {
				throw new NullPointerException("Not supported relation on: ("+entidad.name()+":"+rel.fieldName+")");
				//TODO:, crear una clase por cada relacion
			}
		}
	}
	public void iterateAncestorKeys(Consumer<List<EntityField>> proc) {
		if(this.key == null)
			return;
		List<EntityField> key = new ArrayList<EntityField>(this.key.llaves);
		if(key.get(key.size() - 1).getTargetEntity() == null || key.get(key.size() - 1).getTargetEntity().key.isSimple())
			key.remove(key.size() - 1);
		while(!key.isEmpty()) {
			proc.accept(key);
			EntityField last = key.remove(key.size() - 1);
			if(last.getTargetEntity() != null && key.isEmpty()) {
				List<EntityField> targetKey = last.getTargetEntity().key.llaves;
				key.addAll(targetKey);
				if(key.get(key.size() - 1).getTargetEntity() == null || last.getTargetEntity().key.isSimple())
					key.remove(key.size() - 1);
			}
		}
	}
	public Stream<EntityField> streamFinalFields(){
		Stream<EntityField> ret = key == null ? Stream.empty() : key.llaves.stream().filter(f->!f.isAutogenerated() && !f.isConstant);
		for(EntityClass ce = this; ce != null; ce = ce.padre)
			ret = Stream.concat(ret ,ce.properties.stream().filter(f->f.keyData == null && f.isFinal && !f.isAutoNow));
		return ret;
	}
	public StringSeparator getFinalFields() {
		return FieldSeparator.buildForNames(streamFinalFields());
	}
	
	public final Stream<EntityField> iterateKeysAndProperties(){
		Stream<EntityField> ret = fields.stream().filter(f->f.isEntityProperty || f.keyData != null);
		if(padre != null)
			ret = Stream.concat(padre.iterateKeysAndProperties(), ret);
		return ret;
	}
	@Override
	public int compareTo(EntityClass arg0) {
		return clase.compareTo(arg0.clase);
	}
	public final String queryClassName() {
		if(padre != null || !hijos.isEmpty())
			return clase.name()+".Q.Query";
		else
			return clase.name()+".Query";
	}
	public final String batchClassName() {
		if(padre != null || !hijos.isEmpty())
			return clase.name()+".Q.Batch";
		else
			return clase.name()+".Batch";
	}
	public final String serializerClassName() {
		if(padre != null || !hijos.isEmpty())
			return clase.name()+".Q.Serializer";
		else
			return clase.name()+".Serializer";
	}
	public final String postClassName() {
		if(padre != null || !hijos.isEmpty())
			return clase.name()+".Q.Post";
		else
			return clase.name()+".Post";
	}
}
