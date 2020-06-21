package jcrystal.reflection.annotations.entities;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.entities.Rel1to1;
import jcrystal.json.JsonLevel;
@SuppressWarnings("all")
public class Rel1to1Wrapper implements Rel1to1{
	private final JAnnotation anotacion;
	public Rel1to1Wrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String name(){
		return (String)anotacion.values.get("name");
	}
	public void name(String value){
		anotacion.values.put("name", value);
	}
	public String target(){
		return (String)anotacion.values.get("target");
	}
	public void target(String value){
		anotacion.values.put("target", value);
	}
	public JsonLevel keyLevel(){
		return JsonLevel.valueOf((String)anotacion.values.get("keyLevel"));
	}
	public void keyLevel(JsonLevel value){
		anotacion.values.put("keyLevel", value == null ? null : value.name());
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.entities.Rel1to1.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.entities.Rel1to1("+"keyLevel="+keyLevel()+", "+"name="+name()+", "+"target="+target()+")";
	}
}
