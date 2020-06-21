package jcrystal.reflection.annotations.entities;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.entities.OnConstruct;
@SuppressWarnings("all")
public class OnConstructWrapper implements OnConstruct{
	private final JAnnotation anotacion;
	public OnConstructWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.entities.OnConstruct.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.entities.OnConstruct()";
	}
}
