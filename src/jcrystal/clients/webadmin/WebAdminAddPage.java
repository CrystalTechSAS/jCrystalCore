package jcrystal.clients.webadmin;

import java.util.function.Consumer;

import jcrystal.entity.types.Email;
import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.TypedField;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.types.JClass;
import jcrystal.types.JEnum.EnumValue;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.Post;
import jcrystal.results.Tupla3;
import jcrystal.utils.langAndPlats.HTMLCode;
import jcrystal.utils.langAndPlats.JavascriptCode;

public class WebAdminAddPage {
	
	WebAdminClient client;
	public WebAdminAddPage(WebAdminClient client) {
		this.client = client;
	}
	protected void create(AdminPageDescriptor pageDescriptor) {
		final EntityClass entidad = pageDescriptor.getEntidad();
		JCrystalWebServiceParam param = pageDescriptor.add.ws.parametros.stream().filter(f->f.type().isAnnotationPresent(Post.class)).findFirst().orElse(null);
		JsonLevel level = param.type().getAnnotation(Post.class).level();
		new HTMLCode() {{
				$("<h5 class=\"modal-title\">Agregar "+entidad.name()+"</h5>");
				Consumer<Tupla3<TypedField, String, Boolean>> fieldCreator = tupla ->{
					TypedField f = tupla.v0;
					String prefix = tupla.v1;
					boolean relacion = tupla.v2;
					DIV("class=\"form-group\"",()->{
						$("<label for=\""+prefix+f.name()+"\">"+f.name()+"</label>");
						if(relacion)
							$("select","class=\"custom-select\" id=\""+prefix+f.name()+"\" placeholder=\"-\"",()->{
								$("<option disabled selected value>Choose...</option>");
							});
						else if(f.type().isJAnnotationPresent(CrystalDate.class)) {
							$("<input id=\""+prefix+f.name()+"\" type=\"text\" class=\"form-control datepicker\">");
						}
						else if(f.type().isJAnnotationPresent(Email.class))
							$("<input type=\"email\" class=\"form-control\" id=\""+prefix+f.name()+"\" placeholder=\"name@example.com\">");
						else if(f.type().is(Long.class, Integer.class, long.class, int.class))
							$("<input type=\"number\" class=\"form-control\" id=\""+prefix+f.name()+"\" placeholder=\"0\">");
						else if(f.type().isEnum())
							$("select","class=\"custom-select\" id=\""+prefix+f.name()+"\" placeholder=\"-\"",()->{
								$("<option disabled selected value>Choose...</option>");
								for (EnumValue o : ((JClass)f.type()).enumData.valores) {
									if(o.propiedades.containsKey("name"))
										$("<option value=\""+o.propiedades.get("id")+"\">"+o.propiedades.get("name")+"</option>;");
									else
										$("<option value=\""+o.propiedades.get("id")+"\">"+o.name+"</option>;");
								}
							});
						else if(f.type().isAnnotationPresent(jEntity.class))
							$("select","class=\"custom-select\" id=\""+prefix+f.name()+"\" placeholder=\"-\"",()->{
								$("<option disabled selected value>Choose...</option>");
							});
						else $("<input class=\"form-control\" id=\"" + prefix + f.name()+"\" placeholder=\"-\">");
					});
				};
				client.descriptor.getFields(entidad).get(level).stream().filter(f->f.editable && f.keyData == null).sorted(client.sorterCampos).forEach(f->
					fieldCreator.accept(new Tupla3<TypedField, String, Boolean>(f, "fieldAdd", false))
				);
				entidad.manyToOneRelations.stream().filter(f->f.level.level <= level.level && f.editable).forEach(f->
					fieldCreator.accept(new Tupla3<TypedField, String, Boolean>(f, "fieldAdd", true))
				);
				pageDescriptor.add.ws.parametros.stream().filter(f->!f.type().isAnnotationPresent(Post.class) && !f.securityToken).forEach(f->
					fieldCreator.accept(new Tupla3<TypedField, String, Boolean>(f, "fieldAdd2", false))
				);
				$("<button id=\"btn_save_add\" type=\"button\" class=\"btn btn-primary\">Guardar</button>");
				$("<button type=\"button\" class=\"btn btn-secondary\" data-dismiss=\"modal\">Cancelar</button>");
				$(new JavascriptCode() {{
					$("$( \".datepicker\").datepicker();");
					if(pageDescriptor.add.source!=null)
						$("var updateAddSelects = function()",()->{
							client.createDataPull(this, pageDescriptor, pageDescriptor.add.source, "fieldAdd", "onError", null);
						},";");
					
					client.descriptor.getFields(entidad).get(level).stream().filter(f->f.editable && f.keyData == null).forEach(f->
						$("$('#fieldAdd"+f.name()+"').val('');")
					);
					pageDescriptor.add.ws.parametros.stream().filter(f->!f.type().isAnnotationPresent(Post.class) && !f.securityToken).forEach(f->
						$("$('#fieldAdd2"+f.name()+"').val('');")
					);
					if(pageDescriptor.add.source != null)
						$("updateAddSelects();");
					$("$(\"#btn_save_add\").off('click').click(function()",()->{
						client.createDataPull(this, pageDescriptor, pageDescriptor.add.ws, "fieldAdd", "onError", ()->{
							$("window.history.back();");
							/*if(pageDescriptor.add.ws.m.isVoid)
								$("updateList();");
							else
								throw new NullPointerException("Ussuported return type for 'add' on " + pageDescriptor.definerClass.name);*/
						});
					},");");
				}});
				client.exportFile(this, ""+pageDescriptor.path+"/add.html");
		}};
		
		
	}
}
