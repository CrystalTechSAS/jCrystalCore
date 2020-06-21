package jcrystal.reflection.annotations.async;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.async.Queue;
@SuppressWarnings("all")
public class QueueWrapper implements Queue{
	private final JAnnotation anotacion;
	public QueueWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String name(){
		return (String)anotacion.values.get("name");
	}
	public void name(String value){
		anotacion.values.put("name", value);
	}
	public String rate(){
		return (String)anotacion.values.get("rate");
	}
	public void rate(String value){
		anotacion.values.put("rate", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.async.Queue.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.async.Queue("+"name="+name()+", "+"rate="+rate()+")";
	}
}
