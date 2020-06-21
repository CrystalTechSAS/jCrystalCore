package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.EntityProperty;
import jcrystal.reflection.annotations.IndexType;
import jcrystal.json.JsonLevel;
@SuppressWarnings("all")
public class EntityPropertyWrapper implements EntityProperty{
	private final JAnnotation anotacion;
	public EntityPropertyWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public IndexType index(){
		return IndexType.valueOf((String)anotacion.values.get("index"));
	}
	public void index(IndexType value){
		anotacion.values.put("index", value == null ? null : value.name());
	}
	public String name(){
		return (String)anotacion.values.get("name");
	}
	public void name(String value){
		anotacion.values.put("name", value);
	}
	public boolean editable(){
		return Boolean.parseBoolean((String)anotacion.values.get("editable"));
	}
	public void editable(boolean value){
		anotacion.values.put("editable", Boolean.toString(value));
	}
	public JsonLevel json(){
		return JsonLevel.valueOf((String)anotacion.values.get("json"));
	}
	public void json(JsonLevel value){
		anotacion.values.put("json", value == null ? null : value.name());
	}
	public boolean autoNow(){
		return Boolean.parseBoolean((String)anotacion.values.get("autoNow"));
	}
	public void autoNow(boolean value){
		anotacion.values.put("autoNow", Boolean.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.EntityProperty.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.EntityProperty("+"autoNow="+autoNow()+", "+"editable="+editable()+", "+"index="+index()+", "+"json="+json()+", "+"name="+name()+")";
	}
}
