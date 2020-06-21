package jcrystal.clients.generics;

import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JType;
import jcrystal.types.WrapStringJType;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.GlobalTypeConverter;
import jcrystal.utils.context.ITypeConverter;

public class JavaTypeConverter implements ITypeConverter {

	JavaSEClient client;
	public JavaTypeConverter(JavaSEClient client) {
		this.client = client;
	}
	
	@Override
	public IJType convert(IJType type) {
		if(type instanceof WrapStringJType)
			return type;
		if(type.is(com.google.appengine.api.datastore.Text.class))
			return GlobalTypes.STRING;
		/*else if(type.isAnnotationPresent(CrystalDate.class))
			return GlobalTypes.DATE;
		*/else if(type.isArray() && type.getInnerTypes().get(0).isSubclassOf(MaskedEnum.class))
			return GlobalTypes.LONG;
		else if (type.isAnnotationPresent(Post.class)) {
			JClass superClase = client.context.data.entidades.get(type).clase;
			return new JType(client.context.jClassLoader, client.paqueteEntidades+"." + superClase.getSimpleName());
		}else if(type.isAnnotationPresent(jEntity.class)) {
			return new JType(client.context.jClassLoader, client.paqueteEntidades+"." + type.getSimpleName());
		}else if(type.isAnnotationPresent(jSerializable.class))
			return new JType(client.context.jClassLoader, client.paqueteResultados+"." + type.getSimpleName());
		else if(type.isIterable()){
			final IJType tipoParamero = type.getInnerTypes().get(0);
			if (tipoParamero.name().equals("com.google.appengine.api.datastore.GeoPt"))
				return GlobalTypes.ARRAY2D.DOUBLE;
			else
				return convert(tipoParamero).createListType();
		}return GlobalTypeConverter.INSTANCE.convert(type);
	}
}
