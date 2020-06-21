package jcrystal.server;

import jcrystal.main.data.ClientContext;
import jcrystal.types.JEnum.EnumValue;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.JavaCode;

public class GeneradorJSFjCrystalSession {

	private ClientContext context;
	
	public GeneradorJSFjCrystalSession(ClientContext context) {
		this.context = context;
	}

	public void gen(){
		String paquete = context.input.SERVER.WEB.getBasePackage()+".servlets";
		new JavaCode(){{
			$("package "+paquete+";");
			$import("java.io.*");
			$import("javax.servlet.*");
			$("public class JCrystalSession", ()->{
				$("public static void setToken(jcrystal.reflection.SecurityToken token)",()->{
					$("javax.servlet.http.HttpSession session = (javax.servlet.http.HttpSession)javax.faces.context.FacesContext.getCurrentInstance().getExternalContext().getSession(false);");
					$if("session != null", ()->{
						$("session.setAttribute(\"token\", token.token());");
						$("session.setAttribute(\"rol\", token.rol());");
					});
					
				});
				/*GeneradorRutas.entidades.values().stream().filter(f->f.isSecurityToken() && f.clase.clase.isAnnotationPresent(WebClient.class)).forEach(e->{
					$("public static " + e.clase.clase.name() + " get" + e.clase.clase.getSimpleName()+"()",()->{
						$("javax.servlet.http.HttpSession session = (javax.servlet.http.HttpSession)javax.faces.context.FacesContext.getCurrentInstance().getExternalContext().getSession(false);");
						$if("session != null", ()->{
							$("String token = (String)session.getAttribute(\"token\");");
							$if("token != null", ()->{
								$("return " + e.clase.clase.name() + ".tryGet(token);");
							});
						});
						$("return null;");
					});
				});*/
				if(context.data.rolClass != null){
					for(final EnumValue value : context.data.rolClass.enumData.valores) {
						String name = "Rol" + StringUtils.camelizar(value.name);
						$("public static boolean is"+name+"()", ()->{
							$("javax.servlet.http.HttpSession session = (javax.servlet.http.HttpSession)javax.faces.context.FacesContext.getCurrentInstance().getExternalContext().getSession(false);");
							$if("session != null", ()->{
								$("Long rol = (Long)session.getAttribute(\"rol\");");
								$if("rol != null", ()->{
									$("return (rol & " + value.propiedades.get("id") + ") != 0;");
								});
							});
							$("return false;");
						});
					}
				}
			});
			context.output.exportFile(this, paquete.replace(".", "/") + "/JCrystalSession.java");
		}};
		new JavaCode(){{
			$("package "+paquete+";");
			$import("java.io.*");
			$import("javax.servlet.*");
			$import("javax.faces.bean.*");
			$("@ManagedBean(name = \"security\")");
			$("@RequestScoped");
			$("public class JCrystalSecurityManagedBean", ()->{
				if(context.data.rolClass != null){
					for(final EnumValue value : context.data.rolClass.enumData.valores) {
						String name = "Rol" + StringUtils.camelizar(value.name);
						$("public boolean is"+name+"()", ()->{
							$("return JCrystalSession.is"+name+"();");
						});
					}
				}
			});
			context.output.exportFile(this, paquete.replace(".", "/") + "/JCrystalSecurityManagedBean.java");
		}};
	}
}
