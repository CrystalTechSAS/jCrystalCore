package jcrystal.server;

import jcrystal.main.data.ClientContext;
import jcrystal.reflection.annotations.Post;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.utils.GlobalTypeConverter;
import jcrystal.utils.context.ITypeConverter;
import jcrystal.utils.langAndPlats.AbsICodeBlock;

public class ServerTypeFormatter implements ITypeConverter{
	ClientContext context;
	public ServerTypeFormatter(ClientContext context) {
		this.context = context;
	}
	@Override
	public IJType convert(IJType type) {
		return GlobalTypeConverter.INSTANCE.convert(type);
	}
	@Override
	public String $toString(IJType type, AbsICodeBlock parent) {
		if (type.isAnnotationPresent(Post.class)) 
			return context.data.entidades.get(type).postClassName() + "." + type.getSimpleName();
		return null;
	}

}
