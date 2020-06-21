package jcrystal.clients.flutter;

import org.json.JSONArray;
import org.json.JSONObject;


import com.google.appengine.api.datastore.Text;

import jcrystal.clients.utils.JsonWrapper;
import jcrystal.entity.types.LongText;
import jcrystal.json.JsonLevel;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.main.data.ClientContext;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JType;
import jcrystal.types.convertions.IImportConverter;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.Post;
import jcrystal.utils.GlobalTypeConverter;
import jcrystal.utils.context.ITypeConverter;

public class FlutterTypeConverter implements ITypeConverter, IImportConverter {
	
	private final ClientContext context;
	public FlutterTypeConverter(ClientContext context) {
		this.context = context;
	}

	@Override
	public IJType convert(IJType type) {
		if(type.is(Text.class, LongText.class))
			return GlobalTypes.STRING;
		else if(type.name().equals("com.google.appengine.api.datastore.GeoPt"))
			return GlobalTypes.ARRAY.DOUBLE;
		else if(type.isEnum())
			return new JType(context.jClassLoader, type.getSimpleName());
		else if (type.isAnnotationPresent(Post.class)) {
			JClass superClase = context.data.entidades.get(type).clase;
			return new JType(context.jClassLoader, FlutterClient.paqueteEntidades+"." + superClase.getSimpleName());
		}else if(type.isAnnotationPresent(jEntity.class)) {
			return new JType(context.jClassLoader, FlutterClient.paqueteEntidades+"." + type.getSimpleName());
		}else if(type.isAnnotationPresent(jSerializable.class))
			return new JType(context.jClassLoader, type.getSimpleName());
		else if(type.isArray()){
			if(type.getInnerTypes().get(0).isSubclassOf(MaskedEnum.class))
				return GlobalTypes.LONG;
			return convert(type.getInnerTypes().get(0)).createArrayType();
		}
		else if(type.is(JSONObject.class, JSONArray.class))
			return type;
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
	public String getImportLocation(String pathToRoot, IJType type) {
		if (type.isAnnotationPresent(Post.class)) {
			JsonLevel level = type.getAnnotation(Post.class).level();
			String className = type.getSimpleName()+level.baseName();
			return "import '" + pathToRoot + "entities/" + className + ".dart';";
		}
		else if(type.isAnnotationPresent(jEntity.class)) {
			return "import '" + pathToRoot + "entities/" + type.getSimpleName() + ".dart';";
		}else if(type.isAnnotationPresent(jSerializable.class))
			return "import '" + pathToRoot + "results/" + type.getSimpleName() + ".dart';";
		else if(type.isEnum()) {
			return "import '" + pathToRoot + "enums/" + type.getSimpleName() + ".dart';";
		}
		return null;
	}
}
