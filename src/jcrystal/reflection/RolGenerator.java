package jcrystal.reflection;

import jcrystal.utils.langAndPlats.JavaCode;
import jcrystal.main.data.ClientContext;
import jcrystal.types.JClass;
import jcrystal.types.JEnum.EnumValue;
import jcrystal.utils.StringUtils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.TreeMap;

/**
* Created by G on 11/8/2016.
*/
public class RolGenerator {
	public static class Rol{
		public String valueAccessor;
		public String annotationClass;
		public Rol(String valueAccessor) {
			this.valueAccessor = valueAccessor;
		}
	}
	JClass enumRoles;
	private ClientContext context;
	public RolGenerator(ClientContext context, JClass enumRoles) {
		this.enumRoles = enumRoles;
		this.context = context;
	}
	public static final TreeMap<String, Rol> ROL_MAPPER = new TreeMap<>();
	public void generar() throws FileNotFoundException {
		for(EnumValue value : enumRoles.enumData.valores){
			String name = "Rol"+ StringUtils.camelizar(value.name);
			Rol rol = new Rol(enumRoles.name()+"."+value.name);
			ROL_MAPPER.put(name, rol);
			rol.annotationClass = enumRoles.packageName+".gen."+name;
			new JavaCode() {{
				$("package "+enumRoles.getPackageName()+".gen;");
				$("import java.lang.annotation.Retention;");
				$("import java.lang.annotation.RetentionPolicy;");
				$("@Retention(RetentionPolicy.RUNTIME)");
				$("public @interface "+name,()->{
				});
				context.output.send(this, enumRoles.getPackageName().replace(".","/")+"/gen/"+name+".java");
			}};
		}
	}
	
}
