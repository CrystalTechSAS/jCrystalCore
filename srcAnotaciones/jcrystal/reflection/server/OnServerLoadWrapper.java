package jcrystal.reflection.server;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.server.OnServerLoad;
@SuppressWarnings("all")
public class OnServerLoadWrapper implements OnServerLoad{
	private final JAnnotation anotacion;
	public OnServerLoadWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.server.OnServerLoad.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.server.OnServerLoad()";
	}
}
