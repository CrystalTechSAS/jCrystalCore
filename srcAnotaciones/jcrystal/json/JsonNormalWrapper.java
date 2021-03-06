package jcrystal.json;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.json.JsonNormal;
@SuppressWarnings("all")
public class JsonNormalWrapper implements JsonNormal{
	private final JAnnotation anotacion;
	public JsonNormalWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String[] value(){
		return (String[])anotacion.values.get("value");
	}
	public void value(String[] value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.json.JsonNormal.class;
	}
	@Override public String toString(){
		return "@jcrystal.json.JsonNormal("+"value="+java.util.Arrays.toString(value())+")";
	}
}
