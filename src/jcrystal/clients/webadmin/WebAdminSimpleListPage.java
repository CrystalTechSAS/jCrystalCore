package jcrystal.clients.webadmin;

import java.util.Map;
import java.util.TreeMap;

import jcrystal.configs.clients.admin.ListOption;
import jcrystal.configs.clients.admin.SubListOption;
import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityClass;
import jcrystal.types.JType;
import jcrystal.utils.langAndPlats.HTMLCode;
import jcrystal.utils.langAndPlats.JavascriptCode;

public class WebAdminSimpleListPage {
	
	WebAdminClient client;
	public WebAdminSimpleListPage(WebAdminClient client) {
		this.client = client;
	}
	protected void create(AdminPageDescriptor pageDescriptor, AdminWsDescriptor listService) {
		SubListOption optionDescription = listService.ws.getAnnotation(SubListOption.class);
		JType listType = new JType(client.context.jClassLoader, optionDescription.sublistClass());
		JsonLevel level = listService.ws.getJsonLevel(listType);
		final EntityClass entidad = client.context.data.entidades.get(client.context.jClassLoader.forName(optionDescription.sublistClass()));
		new HTMLCode() {{
			$("<h5 id=\"get_" + entidad.name()+"_title\" class=\"modal-title\">Detalles "+entidad.name()+"</h5>");
			$("table","id=\"tbl\" class=\"table\" style=\"background: white;\"",()->{
				$("<col width=\"20\">");
				  
				
				$("<thead><tr>");
				$("<th>Id</th>");
				client.descriptor.getFields(entidad).get(level).stream().sorted(client.sorterCampos).map(f->f.fieldName()).forEach(line->{
					$("<th>"+line+"</th>");
				});
				$("</tr></thead>");
				$("tbody","",()->{
				});
			});
			$("<button type=\"button\" class=\"btn btn-secondary\" onClick=\"window.history.back();\">Cerrar</button>");
			$(new JavascriptCode() {{
				$("var showList" + listService.ws.name() + " = function(id)",()->{
					Map<String, String> entityToMap = new TreeMap<>();//TODO: llenar este map, ver ejemplo lista principal
					client.createDataPull(this, pageDescriptor, listService.ws, "fieldExtraList_", "onError", ()->{
						String posArgument = "$"+client.getTypePositionOnResponse(listService.ws, listType.createListType());
						$("var body = \"\";");
						$("for(var i in " + posArgument + ")",()->{
							$("body += \"<tr>\";");
							//Agrega la columna del id
							if(pageDescriptor.get != null)
								$("body += \"<td><a role='button' class='btn btn-warning btn-sm' title='Detalles' href='#/"+pageDescriptor.path+"/detail/\" + "+posArgument+"[i].id + \"'><i class='fas fa-search'></i></a></td>\";");
							else 
								$("body += \"<td><button type='button' class='btn btn-warning btn-sm' title='Ver id' onClick='alert(\"+" + posArgument + "[i].id+\")'><i class='fas fa-search'></i></button></td>\"");
							//Agrega las columnas con información
							client.descriptor.getFields(entidad).get(level).forEach(f->{
								$("body += \"<td>\"+"+client.transformToShow(f, posArgument + "[i]."+f.fieldName(), entityToMap)+"+\"</td>\";");
							});

							if(pageDescriptor.delete != null || pageDescriptor.update != null || !pageDescriptor.listOptions.isEmpty()) {
								$("body += \"<td class='tdTools'>\";");
								if(pageDescriptor.update != null)
									$("body += \"<a role='button' class='btn btn-warning btn-sm' title='Editar' href='#/"+pageDescriptor.path+"/update/\" + "+posArgument+"[i].id + \"'><i class='fas fa-edit'></i></a>\";");
								if(pageDescriptor.delete != null)
									$("body += \"<button type='button' class='btn btn-warning btn-sm' data-id=\\\"\"+" + posArgument + "[i].id+\"\\\" title='Eliminar'><i class='fas fa-trash'></i></button>\";");
								for(int e = 0; e < pageDescriptor.listOptions.size(); e++){
									AdminWsDescriptor ws = pageDescriptor.listOptions.get(e);
									if(ws.ws != null) {
										ListOption data = ws.ws.getAnnotation(ListOption.class);
										$("body += \"<button type='button' class='btn btn-warning btn-sm btnExtra"+e+"' data-id=\\\"\"+" + posArgument + "[i].id+\"\\\" title='"+data.name()+"'><i class='fas fa-"+data.icon()+"'></i></button>\";");
									}
								}
								$("body += \"</td>\";");
							}
							$("body += \"</tr>\";");
						});
						$("$('#tbl > tbody').html(body);");
					});
				},";");
			}});
			client.exportFile(this, ""+pageDescriptor.path+"/" + listService.ws.name() + ".html");
		}};
	}
}
