package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.Selector;
@SuppressWarnings("all")
public class SelectorWrapper implements Selector{
	private final JAnnotation anotacion;
	public SelectorWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String valor(){
		return (String)anotacion.values.get("valor");
	}
	public void valor(String value){
		anotacion.values.put("valor", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.Selector.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.Selector("+"valor="+valor()+")";
	}
}
