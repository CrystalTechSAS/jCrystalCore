package jcrystal.clients.android;


import com.google.appengine.api.datastore.Text;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.clients.utils.JsonWrapper;
import jcrystal.entity.types.CreationTimestamp;
import jcrystal.entity.types.LongText;
import jcrystal.entity.types.ModificationTimestamp;
import jcrystal.json.JsonLevel;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.main.data.ClientContext;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JType;
import jcrystal.types.WrapStringJType;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.Post;
import jcrystal.utils.GlobalTypeConverter;
import jcrystal.utils.context.ITypeConverter;
import jcrystal.utils.langAndPlats.AbsICodeBlock;

public class AndroidTypeConverter implements ITypeConverter {
	ClientContext context;
	public AndroidTypeConverter(ClientContext context) {
		this.context = context;
	}

	@Override
	public IJType convert(IJType type) {
		if(type instanceof WrapStringJType)
			return type;
		if(type.is(Text.class, LongText.class))
			return GlobalTypes.STRING;
		else if(type.name().equals("com.google.appengine.api.datastore.GeoPt"))
			return GlobalTypes.ARRAY.DOUBLE;
		else if(type.isArray()){
			if(type.getInnerTypes().get(0).isSubclassOf(MaskedEnum.class))
				return GlobalTypes.LONG;
			return convert(type.getInnerTypes().get(0)).createArrayType();
		}
		else if(type.isIterable()){
			final IJType tipoParamero = type.getInnerTypes().get(0);
			if (tipoParamero.name().equals("com.google.appengine.api.datastore.GeoPt"))
				return GlobalTypes.ARRAY2D.DOUBLE;
			else
				return convert(tipoParamero).createListType();
			/*else if(tipoParamero.isAnnotationPresent(Post.class)) {
				JClass superClase = GeneradorRutas.getEntidadPost(tipoParamero.resolve().getDeclaringClass().getSimpleName()).clase;
				return new JType(context.jClassLoader, AndroidClient.paqueteEntidades+"." +superClase.getSimpleName()).createListType();
			}else if (tipoParamero.isAnnotationPresent(jEntity.class)){
				return new JType(context.jClassLoader, AndroidClient.paqueteEntidades+"." + tipoParamero.getSimpleName()).createListType();
			}else if (tipoParamero.isAnnotationPresent(JsonSerializable.class))
				return new JType(context.jClassLoader, AndroidClient.paqueteResultados+"." + tipoParamero.getSimpleName()).createListType();
			else
				throw new NullPointerException("Unmanaged type " + type);*/
		}else
			return GlobalTypeConverter.INSTANCE.convert(type);
	}
	@Override
	public String $toString(IJType type, AbsICodeBlock parent) {
		if(type.isEnum())
			return AndroidClient.paqueteEntidades+".enums." + type.getSimpleName();
		else if (type.isAnnotationPresent(Post.class)) {
			JClass superClase = context.data.entidades.get(type).clase;
			return AndroidClient.paqueteEntidades+"." + superClase.getSimpleName();
		}
		else if(type.is(CreationTimestamp.class, ModificationTimestamp.class))
			return AndroidClient.paqueteDates+".CrystalDateMilis";
		else if(type.isJAnnotationPresent(InternalEntityKey.class))
			return AndroidClient.paqueteEntidades+"." + type.getJAnnotation(InternalEntityKey.class).simpleKeyName();
		else if(type.isAnnotationPresent(jEntity.class))
			return AndroidClient.paqueteEntidades+"." + type.getSimpleName();
		else if(type.isAnnotationPresent(jSerializable.class))
			return AndroidClient.paqueteResultados+"." + type.getSimpleName();
		
		return null;
	}
}
