package jcrystal.reflection.annotations.entities;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.entities.PreWrite;
@SuppressWarnings("all")
public class PreWriteWrapper implements PreWrite{
	private final JAnnotation anotacion;
	public PreWriteWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.entities.PreWrite.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.entities.PreWrite()";
	}
}
