package jcrystal.reflection.annotations.entities;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.entities.OnUpdate;
@SuppressWarnings("all")
public class OnUpdateWrapper implements OnUpdate{
	private final JAnnotation anotacion;
	public OnUpdateWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.entities.OnUpdate.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.entities.OnUpdate()";
	}
}
