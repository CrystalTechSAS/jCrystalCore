package jcrystal.main.data;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.annotations.server.Exposed;
import jcrystal.clients.utils.JsonWrapper;
import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JType;

public class DataEntidades {
	public final Map<IJType, EntityClass> list = new TreeMap<>();
	public String MetaClassName = "Meta";
	void doPreprocess(List<JClass> entityClasses, ClientContext context) {
		for (JClass clase : entityClasses) {
			if(clase.getSimpleName().equals("Meta"))
				MetaClassName = "Metadata";
			if(!list.containsKey(clase)){
				loadEntity(context, clase);
			}
		}
		for(EntityClass entidad : list.values()) {
			entidad.doPostCreateProcessing(context);
			entidad.clase.ifAnnotation(Exposed.class, exp->{
				for(String id : exp.value())
					context.data.BACKENDS.get(id).register(entidad);
			});
		}
		new java.util.ArrayList<EntityClass>(list.values()).forEach(entidad->{
			for(JsonLevel level : JsonLevel.managedValues) {
				list.put(new JType(context.input.jClassResolver, entidad.postClassName() + "$" + level.baseName()), entidad);
				list.put(new JType(context.input.jClassResolver, entidad.clase.prefixName("Post") + "$" + level.baseName()), entidad);
				if(entidad.isSecurityToken())
					list.put(JsonWrapper.wrapEntity(entidad, level), entidad);
			}
		});
	}
	private EntityClass loadEntity(ClientContext context, JClass clase){
		IJType superClase = clase.getSuperClass();
		EntityClass padre = null;
		if (superClase.isAnnotationPresent(jEntity.class)){
			padre = list.get(superClase);
			if(padre == null) {
				JClass c = (JClass)context.data.map_clases.get(superClase.name());
				padre = loadEntity(context, c);
			}
			if(padre == null)
				throw new NullPointerException("Parent not found");
		}
		EntityClass entidad = new EntityClass(context, clase, padre);
		list.put(clase, entidad);
		context.data.BACKENDS.MAIN.register(entidad);
		return entidad;
	}
	public IJType targetEntity(IJType type) {
		if(type instanceof JsonWrapper)
			return ((JsonWrapper)type).wrappedType();
		return get(type).clase;
	}
	public EntityClass get(IJType type) {
		if(type.isJAnnotationPresent(InternalEntityKey.class))
			return list.get(type.getJAnnotation(InternalEntityKey.class).parentEntity());
		return list.get(type);
	}
	public boolean contains(IJType type) {
		return list.containsKey(type);
	}
}
