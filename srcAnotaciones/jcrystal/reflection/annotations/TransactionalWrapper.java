package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.Transactional;
@SuppressWarnings("all")
public class TransactionalWrapper implements Transactional{
	private final JAnnotation anotacion;
	public TransactionalWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public int retries(){
		return Integer.parseInt((String)anotacion.values.get("retries"));
	}
	public void retries(int value){
		anotacion.values.put("retries", Integer.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.Transactional.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.Transactional("+"retries="+retries()+")";
	}
}
