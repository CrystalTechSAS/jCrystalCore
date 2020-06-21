package jcrystal.utlis.generators;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jcrystal.utils.langAndPlats.JavaCode;

public class GeneradorAnotaciones {
	private static File srcAnotaciones = new File("C:\\Users\\G\\Documents\\GitHub\\jcrystal\\jCrystalServer\\srcAnotaciones");
	private static List<String> exclude = new ArrayList<>();
	private static List<Class<?>> annotationTypes = new ArrayList<>();
	private static Map<String, String> sustTypes = new TreeMap<>();
	public static void main(String[] args) throws IOException {
		procesarFolder(new File("C:\\Users\\G\\Documents\\GitHub\\jcrystal\\jCrystalUtils\\src"));
		procesarFolder(new File("C:\\Users\\G\\Documents\\GitHub\\jcrystal\\jCrystalServer\\srcAnotaciones"));
		new JavaCode() {{
			$("package jcrystal.preprocess.resolvers;");
			$("import java.lang.annotation.Annotation;");
			$("import jcrystal.types.JAnnotation;");
			$("import jcrystal.types.JIAnnotable;");
			$("import jcrystal.types.convertions.IAnnotationResolver;");
			annotationTypes.stream().map(f->f.getPackage().getName()).distinct().forEach(f->{
				$("import "+f+".*;");
			});
			$("public class AnnotationResolver implements IAnnotationResolver",()->{
				$("public <A extends Annotation> A resolveAnnotation(Class<A> annotationClass, JIAnnotable element)",()->{
					$("JAnnotation anotacion = element.getAnnotations().get(annotationClass.getName());");
					$if("anotacion != null", ()->{
						$("switch(annotationClass.getName())",()->{
							annotationTypes.forEach(f->{
								$("case \""+f.getName()+"\":",()->{
									$("return (A)new "+f.getName()+"Wrapper(anotacion);");
								});
							});	
						});
					});
					$("return null;");
				});
			});
			Files.write(new File(srcAnotaciones, "jcrystal.preprocess.resolvers.".replace(".", "/")+"AnnotationResolver.java").toPath(), getCode());
		}};
	}
	private static void procesarFolder(File path){
		SourceFolderVisitor.preCargarClases(path, (paquete, f)->{
			try {
				Class<?> clase = Class.forName(paquete);
				if(clase.isAnnotation() && !sustTypes.containsKey(clase.getName())) {
					procesarAnnotacion(clase);
				}
			} catch (Throwable e) {
				System.err.println(paquete);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	private static void procesarAnnotacion(Class<?> anotacion) throws Exception{
		System.out.println("procesarAnnotacion " + anotacion.getName());
		sustTypes.put(anotacion.getName(), anotacion.getSimpleName());
		for(Method m : anotacion.getDeclaredMethods()) {
			if(m.getReturnType().isArray() && m.getReturnType().getComponentType() == Class.class)
				sustTypes.put(anotacion.getName(), anotacion.getSimpleName()+"Sust");
			else if(m.getReturnType() == Class.class)
				sustTypes.put(anotacion.getName(), anotacion.getSimpleName()+"Sust");
		}
		annotationTypes.add(anotacion);
		JavaCode code = new JavaCode() {{
			$("package "+anotacion.getPackage().getName()+";");
			$("import java.lang.annotation.Annotation;");
			$("import jcrystal.types.JAnnotation;");
			$("import "+anotacion.getName()+";");
			for(Method m : anotacion.getDeclaredMethods())
				if(m.getReturnType().isEnum())
					$("import "+m.getReturnType().getName()+";");
			$("@SuppressWarnings(\"all\")");
			$("public class "+anotacion.getSimpleName()+"Wrapper implements "+anotacion.getSimpleName(),()->{
				$("private final JAnnotation anotacion;");
				$("public "+anotacion.getSimpleName()+"Wrapper(JAnnotation anotacion)",()->{
					$("this.anotacion = anotacion;");
				});
				for(Method m : anotacion.getDeclaredMethods()) {
					String tipo = m.getReturnType().getSimpleName();
					if(m.getReturnType().isArray() && m.getReturnType().getComponentType() == Class.class)
						tipo = "String[]";
					else if(m.getReturnType() == Class.class)
						tipo = "String";
					$("public "+tipo+" "+m.getName()+"()",()->{
						if(m.getReturnType() == String.class)
							$("return (String)anotacion.values.get(\""+m.getName()+"\");");
						else if(m.getReturnType() == int.class)
							$("return Integer.parseInt((String)anotacion.values.get(\""+m.getName()+"\"));");
						else if(m.getReturnType() == boolean.class)
							$("return Boolean.parseBoolean((String)anotacion.values.get(\""+m.getName()+"\"));");
						else if(m.getReturnType() == double.class)
							$("return Double.parseDouble((String)anotacion.values.get(\""+m.getName()+"\"));");
						else if(m.getReturnType() == long.class)
							$("return Long.parseLong((String)anotacion.values.get(\""+m.getName()+"\"));");
						else if(m.getReturnType().isEnum())
							$("return "+m.getReturnType().getSimpleName()+".valueOf((String)anotacion.values.get(\""+m.getName()+"\"));");
						else if(m.getReturnType().isArray()) {
							if(m.getReturnType().getComponentType().isEnum()) {
								$("String[] vals = (String[])anotacion.values.get(\""+m.getName()+"\");");
								$(m.getReturnType().getComponentType().getSimpleName()+"[] ret = new " + m.getReturnType().getComponentType().getSimpleName()+ "[vals.length];");
								$("for(int e = 0; e < vals.length; e++)",()->{
									$("ret[e] = "+m.getReturnType().getComponentType().getSimpleName()+".valueOf(vals[e]);");
								});
								$("return ret;");
							}
							else if(m.getReturnType().getComponentType() == String.class)
								$("return (String[])anotacion.values.get(\""+m.getName()+"\");");
							else if(m.getReturnType().getComponentType() == Class.class)
								$("return (String[])anotacion.values.get(\""+m.getName()+"\");");
							else if(m.getReturnType().getComponentType().isAnnotation())
								$("return java.util.Arrays.stream((JAnnotation[])anotacion.values.get(\""+m.getName()+"\")).map(f->new "+m.getReturnType().getComponentType().getName() +"Wrapper(f)).toArray(f->new "+m.getReturnType().getComponentType().getName()+"[f]);");
						}
						else if(m.getReturnType() == Class.class)
							$("return (String)anotacion.values.get(\""+m.getName()+"\");");
					});
					$("public void "+m.getName()+"(" + tipo + " value)",()->{
						if(m.getReturnType() == String.class)
							$("anotacion.values.put(\"" + m.getName()+"\", value);");
						else if(m.getReturnType() == int.class)
							$("anotacion.values.put(\"" + m.getName()+"\", Integer.toString(value));");
						else if(m.getReturnType() == boolean.class)
							$("anotacion.values.put(\"" + m.getName()+"\", Boolean.toString(value));");
						else if(m.getReturnType() == double.class)
							$("anotacion.values.put(\"" + m.getName()+"\", Double.toString(value));");
						else if(m.getReturnType() == long.class)
							$("anotacion.values.put(\"" + m.getName()+"\", Long.toString(value));");
						else if(m.getReturnType().isEnum())
							$("anotacion.values.put(\"" + m.getName()+"\", value == null ? null : value.name());");
						else if(m.getReturnType().isArray()) {
							if(m.getReturnType().getComponentType().isEnum()) {
								$("String[] vals = new String[value.length];");
								$("for(int e = 0; e < value.length; e++)",()->{
									$("vals[e] = value[e].name();");
								});
								$("anotacion.values.put(\"" + m.getName()+"\", vals);");
							}
							else if(m.getReturnType().getComponentType() == String.class)
								$("anotacion.values.put(\"" + m.getName()+"\", value);");
							else if(m.getReturnType().getComponentType() == Class.class) {
								$("String[] vals = new String[value.length];");
								$("for(int e = 0; e < value.length; e++)",()->{
									$("vals[e] = value[e].getName();");
								});
								$("anotacion.values.put(\"" + m.getName()+"\", vals);");
							}
						}
						else if(m.getReturnType() == Class.class)
							$("anotacion.values.put(\"" + m.getName()+"\" , value.getName());");
					});
				}
				$("@Override public Class<? extends Annotation> annotationType()",()->{
					$("return " + anotacion.getName()+".class;");
				});
				$("@Override public String toString()",()->{
					if(anotacion.getDeclaredMethods().length==0)
						$("return \"@" + anotacion.getName()+"()\";");
					else
						$("return \"@" + anotacion.getName()+"(\"+"+Arrays.stream(anotacion.getDeclaredMethods()).sorted((a,b)->a.getName().compareTo(b.getName())).map(f->{
							if(f.getReturnType().isArray())
								return "\""+f.getName()+"=\"+java.util.Arrays.toString("+f.getName()+"())";
							return "\""+f.getName()+"=\"+"+f.getName()+"()";
						}).collect(Collectors.joining("+\", \"+"))+"+\")\";");
				});
			});
		}};
		new File(srcAnotaciones, anotacion.getName().replace(".", "/")).getParentFile().mkdirs();
		Files.write(new File(srcAnotaciones, anotacion.getName().replace(".", "/")+"Wrapper.java").toPath(), code.getCode());
		if(!sustTypes.get(anotacion.getName()).equals(anotacion.getSimpleName())) {
			exclude.add(anotacion.getName());
			code = new JavaCode() {{
				$("package "+anotacion.getPackage().getName()+";");
				$("import java.lang.annotation.Retention;");
				$("import java.lang.annotation.RetentionPolicy;");
				for(Method m : anotacion.getDeclaredMethods())
					if(m.getReturnType().isEnum())
						$("import "+m.getReturnType().getName()+";");
				$("@Retention(RetentionPolicy.RUNTIME)");
				$("public @interface "+anotacion.getSimpleName(),()->{
					for(Method m : anotacion.getDeclaredMethods()) {
						if(m.getReturnType().isArray() && m.getReturnType().getComponentType() == Class.class)
							$("String[] "+m.getName()+"();");
						else if(m.getReturnType() == Class.class)
							$("String "+m.getName()+"();");
						else
							$(m.getReturnType().getSimpleName()+" "+m.getName()+"();");
					}
				});
			}};
			new File(srcAnotaciones, anotacion.getName().replace(".", "/")).getParentFile().mkdirs();
			Files.write(new File(srcAnotaciones, anotacion.getName().replace(".", "/")+".java").toPath(), code.getCode());
		}
	}
	
}
