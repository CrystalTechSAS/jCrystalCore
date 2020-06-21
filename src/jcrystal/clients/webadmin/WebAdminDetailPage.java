package jcrystal.clients.webadmin;

import java.util.Map;

import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityClass;
import jcrystal.utils.langAndPlats.HTMLCode;
import jcrystal.utils.langAndPlats.JavascriptCode;

public class WebAdminDetailPage {
	
	WebAdminClient client;
	public WebAdminDetailPage(WebAdminClient client) {
		this.client = client;
	}
	protected void create(AdminPageDescriptor pageDescriptor) {
		final EntityClass entidad = pageDescriptor.getEntidad();
		
		new HTMLCode() {{
			$("<h5 id=\"get_" + entidad.name()+"_title\" class=\"modal-title\">Detalles "+entidad.name()+"</h5>");
			JsonLevel level = pageDescriptor.get.getJsonLevel(pageDescriptor.type);
			client.descriptor.getFields(entidad).get(level).stream().sorted(client.sorterCampos).forEach(f->{
				DIV("class=\"form-group\"",()->{
					$("<label style=\"font-weight: bold;\">"+f.fieldName()+"</label>");
					$("<div id=\""+f.fieldName()+"_detail\"></div>");
				});
			});
			$("<button type=\"button\" class=\"btn btn-secondary\" onClick=\"window.history.back();\">Cerrar</button>");
			$(new JavascriptCode() {{
				$("var showDetail = function(id)",()->{
					String returnType = WebAdminUtils.getReturnFromReturnType(pageDescriptor.get);
					
					$(pageDescriptor.definerClass.getSimpleName()+"."+pageDescriptor.get.name()+"(id, function("+returnType+")",()->{
						String posArgument = "$"+client.getTypePositionOnResponse(pageDescriptor.get, pageDescriptor.type);
						$("var $map = {};");
						Map<String, String> entityToMap = client.obtenerMapRespuesta(pageDescriptor, this, pageDescriptor.get);
						
						JsonLevel level = pageDescriptor.get.getJsonLevel(pageDescriptor.type);
						client.descriptor.getFields(entidad).get(level).stream().sorted(client.sorterCampos).forEach(f->{
							if(f.type().is(String.class))
								$("$(\"#"+f.fieldName()+"_detail\").text("+client.transformToShow(f, posArgument + "."+f.fieldName(), entityToMap)+");");
							else
								$("$(\"#"+f.fieldName()+"_detail\").html("+client.transformToShow(f, posArgument + "."+f.fieldName(), entityToMap)+");");
						});
						$("$('#dlg-detail').modal('show');");
					},", onError);");
				},";");
			}});
			client.exportFile(this, ""+pageDescriptor.path+"/detail.html");
		}};
	}
}
