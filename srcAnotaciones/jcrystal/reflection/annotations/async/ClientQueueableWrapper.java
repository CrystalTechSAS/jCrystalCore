package jcrystal.reflection.annotations.async;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.async.ClientQueueable;
@SuppressWarnings("all")
public class ClientQueueableWrapper implements ClientQueueable{
	private final JAnnotation anotacion;
	public ClientQueueableWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.async.ClientQueueable.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.async.ClientQueueable()";
	}
}
