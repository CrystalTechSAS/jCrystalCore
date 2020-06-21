package jcrystal.reflection.annotations;

import jcrystal.types.JAnnotation;

import java.lang.annotation.Annotation;

import jcrystal.reflection.annotations.jEntity;

@SuppressWarnings("all")
public class jEntityImpl extends JAnnotation implements jEntity{
	
	private static final long serialVersionUID = 3689595730243259359L;
	
	public jEntityImpl() {
		super(jEntity.class);
		values.put("name", "");
	}
	
	@Override public String name(){
		return (String)values.get("name");
	}
	@Override public boolean useParentName(){
		return Boolean.parseBoolean((String)values.get("useParentName"));
	}

	@Override public boolean internal() {
		return Boolean.parseBoolean((String)values.get("internal"));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.jEntity.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.Entidad("+"name="+name()+", "+"useParentName="+useParentName()+")";
	}
}
