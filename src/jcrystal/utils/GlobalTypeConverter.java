package jcrystal.utils;

import com.google.appengine.api.datastore.Text;

import jcrystal.entity.types.LongText;
import jcrystal.entity.types.security.EmailAccount;
import jcrystal.entity.types.security.FacebookAccount;
import jcrystal.entity.types.security.FirebaseAccount;
import jcrystal.entity.types.security.GoogleAccount;
import jcrystal.entity.types.security.UsernameAccount;
import jcrystal.types.IJType;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.types.WrapStringJType;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.context.ITypeConverter;

public class GlobalTypeConverter implements ITypeConverter {
	public static final GlobalTypeConverter INSTANCE = new GlobalTypeConverter();
	private GlobalTypeConverter() {
	}
	@Override
	public IJType convert(IJType type) {
		if(type instanceof WrapStringJType)
			return type;
		if(type.is(FacebookAccount.class, GoogleAccount.class, EmailAccount.class, UsernameAccount.class, FirebaseAccount.class))
			return GlobalTypes.STRING;
		if(type.is(Text.class, LongText.class))
			return GlobalTypes.STRING;
		if(!type.getInnerTypes().isEmpty()) {
			if (type.isIterable()) {
				IJType subType = type.getInnerTypes().get(0);
				IJType subTypeConvert = CodeGeneratorContext.get().typeConverter.convert(subType);
				if(subTypeConvert == subType)
					return type;
				else
					return subTypeConvert.createListType();
			}
		}
		return type;
	}
}
