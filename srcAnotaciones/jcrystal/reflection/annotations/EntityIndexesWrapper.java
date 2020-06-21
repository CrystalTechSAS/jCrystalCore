package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.EntityIndexes;
@SuppressWarnings("all")
public class EntityIndexesWrapper implements EntityIndexes{
	private final JAnnotation anotacion;
	public EntityIndexesWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public EntityIndex[] value(){
		return java.util.Arrays.stream((JAnnotation[])anotacion.values.get("value")).map(f->new jcrystal.reflection.annotations.EntityIndexWrapper(f)).toArray(f->new jcrystal.reflection.annotations.EntityIndex[f]);
	}
	public void value(EntityIndex[] value){
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.EntityIndexes.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.EntityIndexes("+"value="+java.util.Arrays.toString(value())+")";
	}
}
