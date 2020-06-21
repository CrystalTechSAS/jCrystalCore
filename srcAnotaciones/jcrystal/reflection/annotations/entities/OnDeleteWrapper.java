package jcrystal.reflection.annotations.entities;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.entities.OnDelete;
@SuppressWarnings("all")
public class OnDeleteWrapper implements OnDelete{
	private final JAnnotation anotacion;
	public OnDeleteWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.entities.OnDelete.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.entities.OnDelete()";
	}
}
