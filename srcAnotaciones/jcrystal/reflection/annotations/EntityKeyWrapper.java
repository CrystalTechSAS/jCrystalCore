package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.EntityKey;
@SuppressWarnings("all")
public class EntityKeyWrapper implements EntityKey{
	private final JAnnotation anotacion;
	public EntityKeyWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public boolean postable(){
		return Boolean.parseBoolean((String)anotacion.values.get("postable"));
	}
	public void postable(boolean value){
		anotacion.values.put("postable", Boolean.toString(value));
	}
	public boolean indexAsProperty(){
		return Boolean.parseBoolean((String)anotacion.values.get("indexAsProperty"));
	}
	public void indexAsProperty(boolean value){
		anotacion.values.put("indexAsProperty", Boolean.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.EntityKey.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.EntityKey("+"indexAsProperty="+indexAsProperty()+", "+"postable="+postable()+")";
	}
}
