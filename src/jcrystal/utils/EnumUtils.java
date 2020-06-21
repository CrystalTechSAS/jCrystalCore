package jcrystal.utils;

import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.utils.GlobalTypes;

public class EnumUtils {
	public static IJType getIdType(IJType clase) {
		return getIdType((JClass) clase);
	}
	public static IJType getIdType(JClass clase) {
		if(clase.enumData != null) {
			IJType ret = clase.enumData.propiedades.get("id");
			if(ret != null)
				return ret;
		}
		return GlobalTypes.STRING;
	}
}
