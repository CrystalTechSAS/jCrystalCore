package jcrystal.reflection.annotations.com;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.com.jSerializable;
@SuppressWarnings("all")
public class jSerializableWrapper implements jSerializable{
	private final JAnnotation anotacion;
	public jSerializableWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.com.jSerializable.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.com.jSerializable()";
	}
}
