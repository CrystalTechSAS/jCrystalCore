package jcrystal.server;

import java.util.stream.Collectors;

import jcrystal.main.data.ClientContext;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.reflection.MaskedEnum;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.utils.langAndPlats.JavaCode;

public class GeneradorEnums {
	private ClientContext context;
	
	private GeneradorMaskedEnums generadorMaskedEnums;
	
	public GeneradorEnums(ClientContext context) {
		super();
		this.context = context;
		generadorMaskedEnums = new GeneradorMaskedEnums(context);
	}
	public void generate(BackendWrapper back) {
		for (JClass clase : context.data.BACKENDS.MAIN.enums)
			generate(back, clase);
	}
	private void generate(BackendWrapper back, JClass clase) {
		generateMainClass(back, clase);
		if(clase.isSubclassOf(MaskedEnum.class)) {
			generadorMaskedEnums.generar(clase);
		}else {
			new JavaCode(){{
				$("package "+clase.getPackageName()+";");
				$("public class Utils" + clase.getSimpleName(), ()->{
					$("public static java.util.List<String> toIds(java.util.List<"+clase.simpleName+"> values)",()->{
						$("java.util.List<String> ret = new java.util.ArrayList<>();");
						$("for("+clase.simpleName+" v : values)if(v!=null)ret.add(v.name());");
						$("return ret;");
					});
					IJType id = clase.enumData.propiedades.get("id");
					if(id != null && id.isPrimitive()) {
						$("public static " + clase.name + " fromId(" + $(id) + " id)",()->{
							if(id.is(long.class)) {
								clase.enumData.valores.forEach(val->{
									$("if(id == " + val.propiedades.get("id")+")return " + clase.name + "." + val.name+";");
								});
							}else {
								$("switch(id)",()->{
									clase.enumData.valores.forEach(val->{
										$("case " + val.propiedades.get("id")+" : return " + clase.name + "." + val.name+";");
									});
								});
							}
							$("return null;");
						});
					}else {
						$("public static " + clase.name + " fromId(String id)",()->{
							$ifNull("id",()->{
								$("return null;");
							});
							$("switch(id)",()->{
								clase.enumData.valores.forEach(val->{
									$("case \"" + val.name+"\" : return " + clase.name + "." + val.name+";");
								});
							});
							$("return "+clase.name + ".valueOf(id);");
						});
						$("public static java.util.List<" + clase.simpleName + "> fromString(java.util.List<String> values)",()->{
							$if("values == null",()->{
								$("return null;");
							});
							$("java.util.List<"+ clase.simpleName +"> ret = new java.util.ArrayList<>();");
							$("for(String v : values)if(ret!=null)ret.add(fromId(v));");
							$("return ret;");
						});
						$("public static java.util.List<" + clase.simpleName + "> from(org.json.JSONArray $Array)",()->{
							$("java.util.List<"+ clase.simpleName +"> ret = new java.util.ArrayList<>();");
							$("for(int i = 0; i < $Array.length(); i++)", ()->{
								$("ret.add(fromId($Array.getString(i)));");
							});
							$("return ret;");
						});
						$("public static String jsonQuote(java.util.List<"+clase.simpleName+"> values)",()->{
							$("java.util.Iterator<"+clase.simpleName+"> it = values.iterator();");
							$("String ret = \"[\";");
							$("if(it.hasNext())ret += jcrystal.JSONUtils.jsonQuote(it.next().name());");
							$("while(it.hasNext())",()->{
								$("ret += \",\" + jcrystal.JSONUtils.jsonQuote(it.next().name());");
							});
							$("return ret+\"]\";");
						});
						$("public static void jsonQuote(java.io.PrintWriter _pw, java.util.List<"+clase.simpleName+"> values)",()->{
							$("java.util.Iterator<"+clase.simpleName+"> it = values.iterator();");
							$("_pw.print(\"[\");");
							$("if(it.hasNext())_pw.print(jcrystal.JSONUtils.jsonQuote(it.next().name()));");
							$("while(it.hasNext())",()->{
								$("_pw.print(\",\");");
								$("_pw.print(jcrystal.JSONUtils.jsonQuote(it.next().name()));");
							});
							$("_pw.print(\"]\");");
						});
					}
				});
				context.output.exportFile(back, this, clase.getPackageName().replace(".", "/")+"/Utils"+clase.getSimpleName()+".java");
			}};
		}
	}
	private void generateMainClass(BackendWrapper back, JClass clase) {
		if(back.id != null) {
			new JavaCode(){{
				$("package "+clase.getPackageName()+";");
				$("public enum " + clase.getSimpleName(), ()->{
					$(clase.enumData.valores.stream().map(f->f.name).collect(Collectors.joining(", ")));
				});
				context.output.exportFile(back, this, clase.getPackageName().replace(".", "/")+"/"+clase.getSimpleName()+".java");
			}};
		}
	}
	
}
