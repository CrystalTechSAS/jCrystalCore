package jcrystal.json;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.json.JsonFull;
@SuppressWarnings("all")
public class JsonFullWrapper implements JsonFull{
	private final JAnnotation anotacion;
	public JsonFullWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String[] value(){
		return (String[])anotacion.values.get("value");
	}
	public void value(String[] value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.json.JsonFull.class;
	}
	@Override public String toString(){
		return "@jcrystal.json.JsonFull("+"value="+java.util.Arrays.toString(value())+")";
	}
}
