package jcrystal.clients.webadmin;

import java.util.ArrayList;
import java.util.List;
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
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.HTMLCode;
import jcrystal.utils.langAndPlats.JavascriptCode;

public class WebAdminUpdatePage {
	
	WebAdminClient client;
	public WebAdminUpdatePage(WebAdminClient client) {
		this.client = client;
	}
	protected void create(AdminPageDescriptor pageDescriptor) {
		final EntityClass entidad = pageDescriptor.getEntidad();
		JCrystalWebServiceParam param = pageDescriptor.update.ws.parametros.stream().filter(f->f.type().isAnnotationPresent(Post.class)).findFirst().orElse(null);
		JsonLevel level = param.type().getAnnotation(Post.class).level();
		List<String> aditionals = new ArrayList<>();
		new HTMLCode() {{
			$("<h5 class=\"modal-title\">Update "+entidad.name()+"</h5>");
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
						CrystalDate date = f.type().getJAnnotation(CrystalDate.class);
						aditionals.add("$( \"#"+prefix+f.name()+"\" ).datepicker( \"option\", \"dateFormat\", CrystalDates."+StringUtils.capitalize(StringUtils.camelizar(date.value().name()))+ ".CLIENT_FORMAT);//-");
						$("<input id=\""+prefix+f.name()+"\" type=\"text\" class=\"form-control datepicker\" placeholder=\""+date.value().userFormat+"\">");
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
			client.descriptor.getFields(entidad).get(level).stream().filter(f->f.editable).sorted(client.sorterCampos).forEach(f->{
				fieldCreator.accept(new Tupla3<TypedField, String, Boolean>(f, "fieldUpdate", false));
			});
			entidad.manyToOneRelations.stream().filter(f->f.level.level <= level.level && f.editable).forEach(f->
				fieldCreator.accept(new Tupla3<TypedField, String, Boolean>(f, "fieldUpdate", true))
			);
			pageDescriptor.update.ws.parametros.stream().filter(f->!f.type().isAnnotationPresent(Post.class) && !f.securityToken && !f.type().is(pageDescriptor.type)).forEach(f->
				fieldCreator.accept(new Tupla3<TypedField, String, Boolean>(f, "fieldUpdate2", false))
			);
			DIV("class=\"modal-footer\"",()->{
				$("<button id=\"btn_save_update\" type=\"button\" class=\"btn btn-primary\">Guardar</button>");
				$("<button type=\"button\" class=\"btn btn-secondary\" onClick=\"window.history.back();\">Cancelar</button>");
			});
			$(new JavascriptCode() {{
				if(pageDescriptor.update.source!=null)
					$("var updateUpdateSelects = function(id)",()->{
						client.createDataPull(this, pageDescriptor, pageDescriptor.update.source, "fieldUpdate", "onError", null);
					},";");
				$("var showUpdate = function(id)",()->{
					$("$( \".datepicker\").datepicker();");
					for(String line : aditionals)
						$(line);
					$("$('#btn_save_update').data(\"id\", id);");
					if(pageDescriptor.update.source != null)
						$("updateUpdateSelects(id);");
					$(pageDescriptor.definerClass.getSimpleName()+"."+pageDescriptor.get.name()+"(id, function(item)",()->{
						client.descriptor.getFields(entidad).get(level).stream().filter(f->f.editable).forEach(f->{
							if(f.type().isJAnnotationPresent(CrystalDate.class)) {
								CrystalDate date = f.type().getJAnnotation(CrystalDate.class);
								$("$(\"#fieldUpdate"+f.fieldName()+"\").val(item."+f.fieldName()+" ? item."+f.fieldName()+".formatClient() : '-');");
							}else
								$("$(\"#fieldUpdate"+f.fieldName()+"\").val(item."+f.fieldName()+");");
						});
					},", onError);");
				},";");
				$("$(\"#btn_save_update\").off('click').click(function()",()->{
					$("var id = $(this).data(\"id\");");
					client.createDataPull(this, pageDescriptor, pageDescriptor.update.ws, "fieldUpdate", "onError", ()->{
						$("window.history.back();");
						/*$("$('#dlg-update').modal('hide');");
						if(pageDescriptor.update.ws.m.isVoid)
							$("updateList();");
						else
							throw new NullPointerException("Ussuported return type for update on " + pageDescriptor.definerClass.name);
						*/
					});
				},");");
			}});
			client.exportFile(this, ""+pageDescriptor.path+"/update.html");
		}};
	}
}
