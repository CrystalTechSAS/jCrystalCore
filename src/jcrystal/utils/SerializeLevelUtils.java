package jcrystal.utils;

import jcrystal.json.JsonBasic;
import jcrystal.json.JsonDetail;
import jcrystal.json.JsonFull;
import jcrystal.json.JsonID;
import jcrystal.json.JsonLevel;
import jcrystal.json.JsonMin;
import jcrystal.json.JsonNormal;
import jcrystal.reflection.annotations.EntityProperty;
import jcrystal.types.JIAnnotable;

public class SerializeLevelUtils {

	public static JsonLevel getAnnotedJsonLevel(JIAnnotable annotable) {
		if(annotable.isAnnotationPresent(EntityProperty.class))
			return annotable.getAnnotation(EntityProperty.class).json();
		if(annotable.isAnnotationPresent(JsonFull.class))
			return JsonLevel.FULL;
		if(annotable.isAnnotationPresent(JsonNormal.class))
			return JsonLevel.NORMAL;
		if(annotable.isAnnotationPresent(JsonBasic.class))
			return JsonLevel.BASIC;
		if(annotable.isAnnotationPresent(JsonID.class))
			return JsonLevel.ID;
		if(annotable.isAnnotationPresent(JsonMin.class))
			return JsonLevel.MIN;
		if(annotable.isAnnotationPresent(JsonDetail.class))
			return JsonLevel.DETAIL;
		
		return JsonLevel.NONE;
	}
}
