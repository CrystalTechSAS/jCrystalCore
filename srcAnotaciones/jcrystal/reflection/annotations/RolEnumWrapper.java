package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.RolEnum;
@SuppressWarnings("all")
public class RolEnumWrapper implements RolEnum{
	private final JAnnotation anotacion;
	public RolEnumWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.RolEnum.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.RolEnum()";
	}
}
