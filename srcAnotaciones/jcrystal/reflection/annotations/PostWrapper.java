package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.Post;
import jcrystal.json.JsonLevel;
@SuppressWarnings("all")
public class PostWrapper implements Post{
	private final JAnnotation anotacion;
	public PostWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public JsonLevel level(){
		return JsonLevel.valueOf((String)anotacion.values.get("level"));
	}
	public void level(JsonLevel value){
		anotacion.values.put("level", value == null ? null : value.name());
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.Post.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.Post("+"level="+level()+")";
	}
}
