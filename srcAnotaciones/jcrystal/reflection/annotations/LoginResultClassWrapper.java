package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.LoginResultClass;
@SuppressWarnings("all")
public class LoginResultClassWrapper implements LoginResultClass{
	private final JAnnotation anotacion;
	public LoginResultClassWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.LoginResultClass.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.LoginResultClass()";
	}
}
