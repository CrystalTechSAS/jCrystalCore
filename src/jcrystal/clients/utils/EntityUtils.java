package jcrystal.clients.utils;

import java.util.List;

import jcrystal.lang.Language;
import jcrystal.lang.elements.TypeChangeAccessor;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityClass;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JType;
import jcrystal.types.JVariable;
import jcrystal.types.vars.IAccessor;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.reflection.MainGenerator;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.EntityKey;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.entities.Rel1to1;
import jcrystal.reflection.annotations.entities.RelMto1;
import jcrystal.reflection.annotations.entities.RelMtoM;

public class EntityUtils {
	private ClientContext context;
	public EntityUtils(ClientContext context) {
		this.context = context;
	}
	public static boolean changesRawType(JVariable f) {
		if(f.isAnnotationPresent(RelMto1.class)) {
			return true;
		}else if(f.isAnnotationPresent(RelMtoM.class)) {
			return true;
		}else if(f.isAnnotationPresent(Rel1to1.class)) {
			return true;
		}else
			return false;
	}
	public IJType convertToRawType(JVariable f) {
		if(f.isAnyAnnotationPresent(RelMto1.class, Rel1to1.class) || (f.isAnnotationPresent(EntityKey.class) && context.data.entidades.contains(f.type()))) {
			EntityClass entidad = context.data.entidades.get(f.type());
			return entidad.key.getSingleKeyType();
		}else if(f.isAnnotationPresent(RelMtoM.class)) {
			EntityClass entidad = context.data.entidades.get(f.type().getInnerTypes().get(0));
			if(entidad.key.isSimple()) {
				IJType type = entidad.key.getLlaves().get(0).type();
				if(CodeGeneratorContext.is(Language.SWIFT))
					return type.getPrimitiveType().createListType(false);
				return type.createListType(false);
			}else
				throw new NullPointerException(f.type().name());
		}else if(f.type().isIterable() && f.type().getInnerTypes().get(0).isEnum())
			return ((JClass)f.type().getInnerTypes().get(0)).enumData.propiedades.get("id").createListType();
		else
			return f.type();
	}
	public IAccessor changeToKeys(IAccessor accesor) {
		if(accesor.type().isAnnotationPresent(Post.class))
			return accesor;
		if(accesor.isAnnotationPresent(RelMto1.class) || accesor.isAnnotationPresent(Rel1to1.class)) {
			EntityClass entidad = context.data.entidades.get(accesor.type());
			if(entidad.key.isSimple())
				return new TypeChangeAccessor(entidad.key.getLlaves().get(0).type(), accesor);
			else
				throw new NullPointerException(accesor.type().name());
		}else if(accesor.isAnnotationPresent(RelMtoM.class)) {
			EntityClass entidad = context.data.entidades.get(accesor.type().getInnerTypes().get(0));
			if(entidad.key.isSimple()) {
				IJType type = entidad.key.getLlaves().get(0).type();
				if(CodeGeneratorContext.is(Language.SWIFT))
					return new TypeChangeAccessor(type.getPrimitiveType().createListType(false), accesor);
				return new TypeChangeAccessor(entidad.key.getLlaves().get(0).type().createListType(), accesor);
			}else
				throw new NullPointerException(accesor.type().name());
		}
		{
			EntityClass entidad = context.data.entidades.get(accesor.type());
			if(entidad != null && entidad.key != null)
				return new TypeChangeAccessor(entidad.key.getSingleKeyType(), accesor);
		}
		if(accesor.type().isSubclassOf(List.class)) {
			if(accesor.type().firstInnerType().isAnnotationPresent(Post.class))
				return accesor;
			EntityClass entidad = context.data.entidades.get(accesor.type().getInnerTypes().get(0));
			if(entidad != null && entidad.key != null)
				return new TypeChangeAccessor(entidad.key.getSingleKeyType().createListType(), accesor);
		}
		return accesor;
	}
}
