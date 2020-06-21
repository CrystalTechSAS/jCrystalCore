package jcrystal.clients.utils;

import java.util.function.Consumer;

import jcrystal.json.JsonLevel;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityClass;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JTypeWrapper;
import jcrystal.reflection.annotations.jEntity;

public class JsonWrapper extends JTypeWrapper{

	private static final long serialVersionUID = 1L;
	
	public final JsonLevel level;
	
	public static IJType wrap(ClientContext context, IJType type, ClassJsonLevelProvider levelProvider) {
		if(type instanceof JsonWrapper)
			return type;
		if(type.isAnnotationPresent(jEntity.class)) {
			EntityClass entidad = context.data.entidades.get(type);
			JsonLevel level = levelProvider.getJsonLevel(type);
			return wrapEntity(entidad, level);
		}if(!type.getInnerTypes().isEmpty()) {
			//TODO Wrapper solo si hay cambios internos
			IJType ret = new JTypeWrapper(type);
			for(int e = 0; e < type.getInnerTypes().size(); e++)
				ret.getInnerTypes().set(e, wrap(context, type.getInnerTypes().get(e), levelProvider));
			return ret;
		}
		return type;
	}
	public static IJType wrapEntity(EntityClass entidad, JsonLevel level) {
		JsonWrapper ret = new JsonWrapper(level, entidad.clase);
		ret.setName(entidad.postClassName() + "$" + level.baseName());
		ret.setSimpleName(entidad.clase.getSimpleName() + level.baseName());
		return ret;
	}
	@Override
	public JClass resolve() {
		return wrappedType.resolve();
	}
	public JsonWrapper(JsonLevel level, IJType type) {
		super(type);
		this.level = level;
	}
	@Override
	public String toString() {
		return level+"("+super.toString()+")";
	}
	@Override
	public void iterate(Consumer<IJType> consumer) {
		consumer.accept(this);
		wrappedType.iterate(consumer);
	}
}
