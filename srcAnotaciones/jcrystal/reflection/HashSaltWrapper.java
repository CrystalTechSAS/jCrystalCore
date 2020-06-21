package jcrystal.reflection;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.security.Base;
import jcrystal.reflection.annotations.security.HashAlg;
import jcrystal.reflection.annotations.security.HashSalt;

@SuppressWarnings("all")
public class HashSaltWrapper implements HashSalt{
	private final JAnnotation anotacion;
	public HashSaltWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String value(){
		return (String)anotacion.values.get("value");
	}
	@Override public Class<? extends Annotation> annotationType(){
		return HashSalt.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.HashSalt("+"value="+value()+")";
	}
	@Override
	public Base base() {
		String value = (String)anotacion.values.get("base");
		return value==null ? Base.BASE64 : Base.valueOf(value);
	}
	@Override
	public HashAlg alg() {
		String value = (String)anotacion.values.get("alg");
		return value == null ? HashAlg.SHA256 : HashAlg.valueOf(value);
	}
}
