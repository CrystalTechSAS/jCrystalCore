package jcrystal.reflection.annotations.async;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.async.Queues;
@SuppressWarnings("all")
public class QueuesWrapper implements Queues{
	private final JAnnotation anotacion;
	public QueuesWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public Queue[] value(){
		return java.util.Arrays.stream((JAnnotation[])anotacion.values.get("value")).map(f->new jcrystal.reflection.annotations.async.QueueWrapper(f)).toArray(f->new jcrystal.reflection.annotations.async.Queue[f]);
	}
	public void value(Queue[] value){
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.async.Queues.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.async.Queues("+"value="+java.util.Arrays.toString(value())+")";
	}
}
