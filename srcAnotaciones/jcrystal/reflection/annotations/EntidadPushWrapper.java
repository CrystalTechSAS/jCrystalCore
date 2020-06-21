package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.EntidadPush;
@SuppressWarnings("all")
public class EntidadPushWrapper implements EntidadPush{
	private final JAnnotation anotacion;
	public EntidadPushWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.EntidadPush.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.EntidadPush()";
	}
}
