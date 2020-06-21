package jcrystal.reflection.annotations.entities;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.entities.OnCreate;
@SuppressWarnings("all")
public class OnCreateWrapper implements OnCreate{
	private final JAnnotation anotacion;
	public OnCreateWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.entities.OnCreate.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.entities.OnCreate()";
	}
}
