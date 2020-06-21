package jcrystal.clients.ios;

import java.util.Date;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;


import com.google.appengine.api.datastore.Text;

import jcrystal.entity.types.LongText;
import jcrystal.main.data.ClientContext;
import jcrystal.reflection.annotations.Post;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JType;
import jcrystal.types.WrapStringJType;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.GlobalTypeConverter;
import jcrystal.utils.context.ITypeConverter;

public class IOSTypeConverter implements ITypeConverter {
	
	private final ClientContext context;
	public IOSTypeConverter(ClientContext context) {
		this.context = context;
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public IJType convert(IJType type) {
		if(type instanceof WrapStringJType)
			return type;
		if(type.is(Text.class, LongText.class))
			return GlobalTypes.STRING;
		else if(type.is(JSONObject.class, JSONArray.class))
			return type;
		else if (type.isAnnotationPresent(Post.class)) {
			JClass superClase = context.data.entidades.get(type).clase;
			return new JType(context.jClassLoader, superClase.getSimpleName());
		}
		else if(type.name().equals("com.google.appengine.api.datastore.GeoPt"))
			return GlobalTypes.ARRAY.DOUBLE;
		else if(type.isIterable()){
			if(type.getInnerTypes().isEmpty())
				throw new NullPointerException("Unmanaged type " + type);
			final IJType tipoParamero = type.getInnerTypes().get(0);
			if (tipoParamero.name().equals("com.google.appengine.api.datastore.GeoPt"))
				return GlobalTypes.ARRAY2D.DOUBLE;
			else
				return convert(tipoParamero).createListType(type.nullable());
		}else
			return GlobalTypeConverter.INSTANCE.convert(type);
	}
}
