package jcrystal.reflection.annotations.security;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.security.HashSalt;
import jcrystal.reflection.annotations.security.Base;
import jcrystal.reflection.annotations.security.HashAlg;
@SuppressWarnings("all")
public class HashSaltWrapper implements HashSalt{
	private final JAnnotation anotacion;
	public HashSaltWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public Base base(){
		return Base.valueOf((String)anotacion.values.get("base"));
	}
	public void base(Base value){
		anotacion.values.put("base", value == null ? null : value.name());
	}
	public String value(){
		return (String)anotacion.values.get("value");
	}
	public void value(String value){
		anotacion.values.put("value", value);
	}
	public HashAlg alg(){
		return HashAlg.valueOf((String)anotacion.values.get("alg"));
	}
	public void alg(HashAlg value){
		anotacion.values.put("alg", value == null ? null : value.name());
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.security.HashSalt.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.security.HashSalt("+"alg="+alg()+", "+"base="+base()+", "+"value="+value()+")";
	}
}
