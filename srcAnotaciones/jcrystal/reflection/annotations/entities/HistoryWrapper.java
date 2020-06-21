package jcrystal.reflection.annotations.entities;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.entities.History;
@SuppressWarnings("all")
public class HistoryWrapper implements History{
	private final JAnnotation anotacion;
	public HistoryWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.entities.History.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.entities.History()";
	}
}
