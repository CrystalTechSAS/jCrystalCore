package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.Delete;
@SuppressWarnings("all")
public class DeleteWrapper implements Delete{
	private final JAnnotation anotacion;
	public DeleteWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.Delete.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.Delete()";
	}
}
