package jcrystal.clients.webadmin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import jcrystal.clients.AbsClientGenerator;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.clients.js.JQueryClient;
import jcrystal.configs.clients.Client;
import jcrystal.configs.clients.admin.ListOption;
import jcrystal.configs.clients.admin.SubListOption;
import jcrystal.entity.types.Email;
import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.TypedField;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceManager;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.server.FileDownloadDescriptor;
import jcrystal.server.databases.datastore.FileData;
import jcrystal.types.IJType;
import jcrystal.types.JAnnotation;
import jcrystal.types.JClass;
import jcrystal.types.JVariable;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.HTMLCode;
import jcrystal.utils.langAndPlats.JavascriptCode;

public class WebAdminClient extends AbsClientGenerator<Client>{
	
	TreeMap<String, AdminPageDescriptor> PAGES = new TreeMap<>();
	JQueryClient jQueryC = new JQueryClient(new ClientGeneratorDescriptor<Client>(context, descriptor.client, descriptor.clientAnnotationClass));
	
	public WebAdminClient(ClientGeneratorDescriptor<Client> descriptor){
		super(descriptor);
		jQueryC.setOutputPath("js/");
	}
	@Override
	protected void setupEnviroment() throws Exception {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean registrar(JCrystalWebServiceManager controller, JCrystalWebService metodo) {
		if(super.registrar(controller, metodo)) {
			if(!PAGES.containsKey(controller.clase.name()))
				PAGES.put(controller.clase.name(), new AdminPageDescriptor(context, controller.clase));
			PAGES.get(controller.clase.name()).register(metodo);
			jQueryC.registrar(controller, metodo);
			return true;
		}return false;
	}
	
	@Override
	protected void generarCliente() throws Exception {
		if(PAGES.isEmpty())
			return;
		
		if(context.data.adminData.login == null)
			throw new NullPointerException("Invalid configurarion. Must be at least one login method");
		for(AdminPageDescriptor pageDesc : PAGES.values()) {
			crearSorter(pageDesc);
			generatePage(pageDesc);
			if(pageDesc.add != null)
				new WebAdminAddPage(this).create(pageDesc);
			if(pageDesc.update != null)
				new WebAdminUpdatePage(this).create(pageDesc);
			if(pageDesc.get != null)
				new WebAdminDetailPage(this).create(pageDesc);
			for(AdminWsDescriptor subListOption : pageDesc.subListOptions)
				new WebAdminSimpleListPage(this).create(pageDesc, subListOption);
			
		}
			
		generateLogin();
		generateMenu();
		jQueryC.generar();
	}
	private void generateLogin() {
		new HTMLCode() {{
			$("html",()->{
				$("head",()->{
					$("<title>Admin Panel</title>");
					putHead(this);
					$("<script src=\"js/" + context.data.adminData.login.padre.clase.getSimpleName() + ".js\"></script>");
				});
				$("body style=\"background: #000;\"",()->{
					DIV("class=\"container\"",()->{
						DIV("id=\"gblErrorAlert\" style=\"visibility: hidden;padding-top: 30px;\"",()->{ });
						DIV("class=\"modal\" style=\"display: block;\" tabindex=\"-1\" role=\"dialog\"",()->{
							DIV("class=\"modal-dialog modal-dialog-centered\" role=\"document\"",()->{
								DIV("class=\"modal-content\"",()->{
									DIV("class=\"modal-content\"",()->{
										DIV("class=\"modal-header\"",()->{
											$("<h5 class=\"modal-title\">Admin login</h5>");
										});
										DIV("class=\"modal-body\"",()->{
											for(JCrystalWebServiceParam param : context.data.adminData.login.parametros) {
												DIV("class=\"form-group\"",()->{
													$("<label for=\""+param.nombre+"_login\">"+param.nombre+"</label>");
													if(param.type().isJAnnotationPresent(Email.class) || param.nombre.equalsIgnoreCase("user") || param.nombre.equalsIgnoreCase("usuario") || param.nombre.equalsIgnoreCase("email") || param.nombre.equalsIgnoreCase("mail") || param.nombre.equalsIgnoreCase("login"))
														$("<input type=\"email\" class=\"form-control\" id=\""+param.nombre+"_login\" placeholder=\"name@example.com\">");
													else $("<input type=\"password\" class=\"form-control\" id=\""+param.nombre+"_login\" placeholder=\"-\">");
												});
											}
										});
										DIV("class=\"modal-footer\"",()->{
											$("<button type=\"submit\" class=\"btn btn-warning\" onclick=\"doLogin();\">Enter</button>");
										});
									});
								});
								
							});
							
						});
					});
					$(new JavascriptCode() {{
						JCrystalWebServiceParam lastParam = context.data.adminData.login.parametros.get(context.data.adminData.login.parametros.size() - 1); 
						$("document.getElementById(\"" + lastParam.nombre + "_login\").addEventListener(\"keyup\", function(event)",()->{
							$("event.preventDefault();");
							$if("event.keyCode === 13",()->{
								$("doLogin();");
							});
						},");");
						$("let doLogin = function()",()->{
							StringSeparator params = new StringSeparator(", ");
							for(JCrystalWebServiceParam param : context.data.adminData.login.parametros)
								params.add("$('#"+param.nombre+"_login').val()");
							$(context.data.adminData.login.padre.clase.getSimpleName()+"."+context.data.adminData.login.name()+"("+params+", function(token)",()->{
								$("window.location.replace(\"/admin/menu.html\");");
							},", onError);");
						},";");
						putOnError(this);
					}});
				});
			});
			exportFile(this, "index.html");
		}};
	}
	private void generateMenu() {
		new HTMLCode() {{
			$("html",()->{
				$("head",()->{
					$("<title>Admin Panel</title>");
					putHead(this);
					$("style","",()->{
						$("th{text-align: left;}");
						$(".tdTools button:not(:first-child){");
						$("margin-left: 3px;");
						$("}");
						for(int e = 0; e < 10; e++) {
							$(".padUlListLevel"+e+"{padding-left: "+(16+e*5)+"px;}");
						}
					});
					$("<script src=\"js/" + context.data.adminData.logout.padre.clase.getSimpleName() + ".js\"></script>");
				});
				$("body","style=\"background-color: #EEEEEE;\"",()->{
					$("nav","class=\"navbar navbar-expand-lg navbar-dark\" style=\"background-color: #000;\"",()->{
						$("<a id=\"topNavBarTitle\" class=\"navbar-brand\" href=\"#\">Admin Panel</a>");
						$("button","class=\"navbar-toggler\" type=\"button\" data-toggle=\"collapse\" data-target=\"#navbarSupportedContent\" aria-controls=\"navbarSupportedContent\" aria-expanded=\"false\" aria-label=\"Toggle navigation\"",()->{
							$("<span class=\"navbar-toggler-icon\"></span>");
						});
						DIV("class=\"collapse navbar-collapse\" id=\"navbarSupportedContent\"",()->{
							$("ul","class=\"navbar-nav mr-auto\"",()->{});
							$("ul","class=\"navbar-nav\"",()->{
								$("li","class=\"nav-item active\"",()->{
									$("<a class=\"nav-link\" href=\"#\" onclick=\"logout()\">Salir</a>");
								});
							});
						});
					});
					
					DIV("class=\"container-fluid\"",()->{
						DIV("class=\"row flex-xl-nowrap\"",()->{
							DIV("class=\"col-12 col-md-3 col-xl-2 bd-sidebar\" style=\"background-color: #000000;padding-left: 0;padding-right: 0;\"",()->{
								
									class Node{
										int level = -1;
										String key;
										AdminPageDescriptor value;
										Map<String, Node> childs = new TreeMap<String, Node>();
										Node(String k, int level){
											this.key = k;
											this.level = level;
										}
										public void add(AdminPageDescriptor page) {
											Node act = this;
											for(String k : page.path.split("/")) {
												Node next = act.childs.get(k);
												if(next == null)
													act.childs.put(k, next = new Node(k, act.level + 1));
												act = next;
											}
											act.value = page;
										}
										public void preorden() {
											if(childs.isEmpty()) {
												$("li","class=\"nav-item\"",()->{
													$("<a id=\"navBtn"+value.id+"\" class=\"nav-link mainMenuBtn padUlListLevel"+level+"\" href=\"#/"+value.path+"\" style=\"color: white;\">"+key+"</a>");												
												});
											}else {
												if(key == null)
													$("ul","id=\"sidemenu\" class=\"nav flex-column nav-pills\"",()->{
														childs.values().forEach(f->f.preorden());
													});
												else { 
													$("li","class=\"nav-item\"",()->{
														if(value != null)
															$("<a  id=\"navBtn"+value.id+"\" class=\"nav-link mainMenuBtn padUlListLevel"+level+"\" href=\"#/"+value.path+"\" onclick=\"\" style=\"color: white;\">"+key+"</a>");
														else
															$("<a class=\"nav-link mainMenuBtn padUlListLevel"+level+"\" style=\"color: #CCCCCC;\">"+key+"</a>");
													});
													childs.values().forEach(f->f.preorden());
												}
											}
										}
									}
									Node raiz = new Node(null, -1);
									PAGES.values().forEach(raiz::add);
									raiz.preorden();
							});
							$("main", "class=\"col-12 col-md-9 col-xl-10 py-md-3 pl-md-3 bd-content\"",()->{
								DIV("id=\"gblErrorAlert\" style=\"visibility: hidden;\"",()->{ });
								DIV("id=\"content\"",()->{
									
								});
							});
						});
					});
					$(new JavascriptCode() {{
						$("var logout = function()",()->{
							if(context.data.adminData.logout != null){
								$(context.data.adminData.logout.padre.clase.getSimpleName()+"."+context.data.adminData.logout.name()+"(function()",()->{
									$("var cookies = document.cookie.split(\";\");"); 
									$("for (var i = 0; i < cookies.length; i++)",()->{
										$("var cookie = cookies[i];");
										$("var eqPos = cookie.indexOf(\"=\");");
										$("var name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie;");
										$("document.cookie = name + \"=;expires=Thu, 01 Jan 1970 00:00:00 GMT\";");
									});
									$("window.location.replace(\"/admin/index.html\");");
								},",");
								$("function()",()->{
									$("var cookies = document.cookie.split(\";\");"); 
									$("for (var i = 0; i < cookies.length; i++)",()->{
										$("var cookie = cookies[i];");
										$("var eqPos = cookie.indexOf(\"=\");");
										$("var name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie;");
										$("document.cookie = name + \"=;expires=Thu, 01 Jan 1970 00:00:00 GMT\";");
									});
									$("window.location.replace(\"/admin/index.html\");");
								},");");
							}
						},";");
						$("var jcParseInt = function(val)",()->{
							$("var ret = parseInt(val);");
							$if("isNaN(val)",()->{
								$("return null;");
							});
							$("return val;");
						},";");
						$("var getPickerDate = function(val, format)",()->{
							$("try", ()->{
								$("return $.datepicker.parseDate(format || \"mm/dd/yy\", val );");
							});
							$("catch(error)",()->{ });
							$("return null;");
						},";");
						putOnError(this);
						$("//Routing");
						$("var putContent = function(url, btnId, success)",()->{
							$("clearError();");
							$("$( \".mainMenuBtn\" ).removeClass( \"active\" );");
							$("$(btnId).addClass( \"active\" );");
							$("$.ajax(",()->{
								$("url: url,");
								$("success: function(result)",()->{
									$("$(\"#content\").html(result);");
									$("if(success)",()->{
										$("success();");
									});
								});
							},");");
						},";");
						$("var app = $.sammy('#content', function()",()->{
							$("this.get('#/', function(context)",()->{
								
							},");");
							PAGES.values().forEach(page->{
								$("this.get('#/"+page.path+"', function(context)",()->{
									$("putContent(\"./"+page.path+"/list.html\", \"#navBtn"+page.id+"\");");
								},");");
								if(page.add != null)
									$("this.get('#/"+page.path+"/add', function(context)",()->{
										$("putContent(\"./"+page.path+"/add.html\", \"#navBtn"+page.id+"\");");
									},");");
								if(page.get != null)
									$("this.get('#/"+page.path+"/detail/:id', function(context)",()->{
										$("var id = this.params['id'];");
										$("putContent(\"./"+page.path+"/detail.html\", \"#navBtn"+page.id+"\", function()",()->{
											$("showDetail(id);");
										},");");
									},");");
								if(page.update != null)
									$("this.get('#/"+page.path+"/update/:id', function(context)",()->{
										$("var id = this.params['id'];");
										$("putContent(\"./"+page.path+"/update.html\", \"#navBtn"+page.id+"\", function()",()->{
											$("showUpdate(id);");
										},");");
									},");");
								for(AdminWsDescriptor subListOption : page.subListOptions)
									$("this.get('#/"+page.path+"/"+subListOption.ws.name()+"/:id', function(context)",()->{
										$("var id = this.params['id'];");
										$("putContent(\"./"+page.path+"/" + subListOption.ws.name() + ".html\", \"#navBtn"+page.id+"\", function()",()->{
											$("showList" + subListOption.ws.name() + "(id);");
										},");");
									},");");
								
							});
						},");");
						$("app.run('#/');");
					}});
				});
			});
			exportFile(this, "menu.html");
		}};
		
	}
	private void putHead(HTMLCode code){
		code.$("<meta charset=\"UTF-8\">");
		code.$("<link rel=\"stylesheet\" href=\"js/bootstrap.min.css\"/>");
		code.$("<link rel=\"stylesheet\" href=\"js/jquery-ui.css\"/>");
		code.$("<link rel=\"stylesheet\" href=\"js/primeui.min.css\"/>");
		code.$("<link rel=\"stylesheet\" href=\"js/theme.css\"/>");
		
		code.$("<script src=\"js/x-tag-core.min.js\"></script>");
		code.$("<script src=\"js/jquery.js\"></script>");
		code.$("<script src=\"js/sammy-latest.min.js\"></script>");
		code.$("<script src=\"js/jquery-ui.js\"></script>");
		code.$("<script src=\"js/primeui.min.js\"></script>");
		code.$("<script defer src=\"js/fawsome.js\"></script>");
		code.$("<script src=\"js/bootstrap.min.js\"></script>");
		code.$("<script src=\"js/primeelements.min.js\"></script>");
		code.$("<script src=\"js/jquery.form.js\"></script>");
		code.$("<script src=\"js/date.format.js\"></script>");
		code.$("<script src=\"js/model.js\"></script>");
		
	}
	
	protected Comparator<EntityField> sorterCampos; 
	
	private void crearSorter(AdminPageDescriptor pageDescriptor) {
		if(!pageDescriptor.type.isAnnotationPresent(jEntity.class))
			throw new NullPointerException("Invalid type " + pageDescriptor.type);
		
		sorterCampos = EntityField::compareTo;
		if(pageDescriptor.definerClass.isJAnnotationPresent(pageDescriptor.type.getPackageName() + ".Meta"+pageDescriptor.type.getSimpleName() + "$Order")) {
			JAnnotation ann = pageDescriptor.definerClass.getJAnnotation(pageDescriptor.type.getPackageName()+".Meta"+pageDescriptor.type.getSimpleName() + "$Order");
			String[] array = (String[])ann.values.get("value");
			if(array != null)
				sorterCampos = (c1,c2) ->{ 
					int p1 = 0, p2=0;
					while(p1<array.length && !Objects.equals(array[p1], c1.fieldName()))p1++;
					while(p2<array.length && !Objects.equals(array[p2], c2.fieldName()))p2++;
					if(p1 == p2)
						return c1.compareTo(c2);
					else
						return Integer.compare(p1, p2);
				};
		}
	}
	
	private void generatePage(AdminPageDescriptor pageDescriptor) {
		
		final AdminWsDescriptor list = pageDescriptor.list;
		final EntityClass entidad = pageDescriptor.getEntidad();
		new HTMLCode() {{
			JsonLevel levelList = list.ws.getJsonLevel(pageDescriptor.type);
			HTMLCode htmlCode = this;
			$("<script src=\"js/" + list.ws.padre.clase.getSimpleName() + ".js\"></script>");
			$("div",()->{
				$("<h1>Administrador "+pageDescriptor.label+"</h1>");
				if(pageDescriptor.add != null) {
					$("<h4>Opciones:</h4>");
					$("<a role=\"button\" class=\"btn btn-warning\" href=\"#/"+pageDescriptor.path+"/add\">Agregar</a>");
				}
				JavascriptCode selectListeners = new JavascriptCode();
				selectListeners.new B() {{
					$("$( \".datepicker\").datepicker();");
				}};
				
				boolean[] titleFiltros = {false};
				list.ws.parametros.stream().filter(f->!f.securityToken && !f.type().is(pageDescriptor.type)).forEach(wsp->{
					if(!titleFiltros[0])
						$("<h4>Filtros:</h4>");
					titleFiltros[0]=true;
					DIV("class=\"input-group mb-3\"",()->{
						$("div","class=\"input-group-prepend\"",()->{
							$(" <label class=\"input-group-text\" for=\"fieldExtra"+wsp.nombre+"\">"+wsp.nombre+"</label>");
						});
						if(wsp.type().isAnnotationPresent(jEntity.class))
							$("select","class=\"custom-select\" name=\""+wsp.nombre+"\" id=\"fieldExtraList_2"+wsp.nombre+"\" placeholder=\"-\"",()->{
							$("<option disabled selected value>Choose...</option>");
						});
						else if(wsp.type().isJAnnotationPresent(Email.class))
							$("<input type=\"email\" name=\""+wsp.nombre+"\" class=\"form-control\" id=\"fieldExtraList_2" + wsp.nombre + "\" placeholder=\"name@example.com\">");
						else if(wsp.type().is(Long.class, Integer.class, long.class, int.class))
							$("<input type=\"number\" name=\""+wsp.nombre+"\" class=\"form-control\" id=\"fieldExtraList_2"+wsp.nombre+"\" placeholder=\"0\">");
						else if(wsp.type().isJAnnotationPresent(CrystalDate.class)) {
							CrystalDate date = wsp.type().getJAnnotation(CrystalDate.class);
							$("<input class=\"form-control datepicker\" name=\""+wsp.nombre+"\"  id=\"fieldExtraList_2"+wsp.nombre+"\"  placeholder=\""+date.value().userFormat+"\">");
							selectListeners.$("$( \"#fieldExtraList_2"+wsp.nombre+"\" ).datepicker( \"option\", \"dateFormat\", CrystalDates."+StringUtils.capitalize(StringUtils.camelizar(date.value().name()))+ ".prototype.CLIENT_FORMAT);");
						}
						else 
							$("<input class=\"form-control\" name=\""+wsp.nombre+"\"  id=\"fieldExtraList_2"+wsp.nombre+"\" placeholder=\"-\">");
					});
					selectListeners.new B() {{
						$("$('#fieldExtraList_2"+wsp.nombre+"').change(function()",()->{
							$("updateList();");
						},");");
					}};
				});
				$("<div id=\"tbl\" class=\"table\"></div>");
				JavascriptCode buttonsCode = new JavascriptCode() {{
					for(int e = 0; e < pageDescriptor.listOptions.size(); e++){
						AdminWsDescriptor ws = pageDescriptor.listOptions.get(e);
						extraDialog(htmlCode, this, pageDescriptor, e, ws, entidad);
					}
					if(pageDescriptor.get != null) {
						$("var searchEntityById = function(id)",()->{
							$("var id = prompt(\"Ingrese el id a buscar\", \"id\");");
							$("document.location = '#/"+pageDescriptor.path+"/detail/' + id;");
						},";");
					}
					if(pageDescriptor.delete != null) {
						$("var mostrarEliminar = function(id)",()->{
							if(!entidad.key.isSimple())
								$ifNotNull("id", ()->$("id = JSON.parse(atob(id));"));
							$("if (confirm('¿Seguro de eliminar este registro?'))",()->{
								$(pageDescriptor.definerClass.getSimpleName()+"."+pageDescriptor.delete.name()+"(id, function(item)",()->{
									$("reload();");
								},", onError);");
							});
						},";");
					}
				}};
				
				
				$(new JavascriptCode() {{
					$append(buttonsCode);
					$append(selectListeners);
					if(list.source!=null)
						$("var updateListSelects = function()",()->{
							createDataPull(this, pageDescriptor, list.source, "fieldExtraList_", "onError", null);
						},";");
					$("var updateList = function()",()->{
						$("var $map = {};");
						$("$('#tbl').puidatatable(",()->{
							JavascriptCode entityMapCode = new JavascriptCode();
							Map<String, String> entityToMap = obtenerMapRespuesta(pageDescriptor, entityMapCode, list.ws);
							$("columns: [");
							{
								$("{field: 'id', headerText: 'Id', headerStyle: \"width: 50px;\", content: function(data)",()->{
									if(pageDescriptor.get != null)
										$("return $(\"<a role='button' class='btn btn-warning btn-sm' title='Detalles' href='#/"+pageDescriptor.path+"/detail/\" + data.id + \"'><i class='fas fa-search'></i></a>\");");
									else 
										$("return $(\"<button type='button' class='btn btn-warning btn-sm' title='Ver id' onClick='alert(\"+ data.id+\")'><i class='fas fa-search'></i></button>\");");
								},"},");
								descriptor.getFields(entidad).get(levelList).stream().filter(f->f.keyData == null).sorted(sorterCampos).forEach(f->{
									String campo = "{field: '" + f.fieldName() + "', headerText: '"+StringUtils.deCamelizeWithSpaces(f.fieldName())+"'"; 
									if(f.type().isAnnotationPresent(jEntity.class) && !f.type().is(FileData.class))
										campo += ", filter: true";
									else if(f.type().isJAnnotationPresent(CrystalDate.class))
										campo += ", sortable: true";
									$(campo + ", content: function(data)",()->{
										$("return "+transformToShow(f, "data."+f.fieldName(), entityToMap)+";");
									},"},");
								});
								if(pageDescriptor.delete != null || pageDescriptor.update != null || !pageDescriptor.listOptions.isEmpty() || !pageDescriptor.subListOptions.isEmpty()){
									$("{field: 'operaciones', headerText: 'Operaciones', content: function(data)",()->{
										$("var btns = '';");
										if(pageDescriptor.update != null)
											$("btns += \"<a role='button' class='button btn btn-warning btn-sm' style='display: inline;' title='Editar' href='#/"+pageDescriptor.path+"/update/\" + data.id + \"'><i class='fas fa-edit'></i></a>\";");
										if(pageDescriptor.delete != null)
											$("btns += \"<button type='button' class='btn btn-warning btn-sm' onclick='mostrarEliminar(\"+data.id+\");' title='Eliminar'><i class='fas fa-trash'></i></button>\";");
										for(int e = 0; e < pageDescriptor.listOptions.size(); e++){
											AdminWsDescriptor ws = pageDescriptor.listOptions.get(e);
											if(ws.ws != null) {
												ListOption data = ws.ws.getAnnotation(ListOption.class);
												if(entidad.key.isSimple())
													$("btns += \"<button type='button' onclick='btnExtra"+e+"Click(\" + data.id + \");' class='btn btn-warning btn-sm btnExtra"+e+"' data-id=\\\"\" + data.id + \"\\\" title='"+data.name()+"'><i class='fas fa-"+data.icon()+"'></i></button>\";");
												else
													$("btns += \"<button type='button' onclick='btnExtra"+e+"Click(\\\"\" + data.getKeyBase64() + \"\\\");' class='btn btn-warning btn-sm btnExtra"+e+"' data-id=\\\"\" + data.getKeyBase64() + \"\\\" title='"+data.name()+"'><i class='fas fa-"+data.icon()+"'></i></button>\";");
											}
										}
										pageDescriptor.subListOptions.forEach(ws->{
											if(ws.ws != null) {
												SubListOption data = ws.ws.getAnnotation(SubListOption.class);
												$("btns += \"<a role='button' class='btn btn-warning btn-sm title='"+data.name()+"' href='#/"+pageDescriptor.path+"/"+ws.ws.name()+"/\" + data.id + \"'><i class='fas fa-"+data.icon()+"'></i></a>\";");
											}
										});
										$("return $(btns);");
									},"},");
								}
							}
							$("],");
							$("paginator: {rows: 25},");
							$("datasource: function(callback)",()->{
								$("var context = this;");
								createDataPull(this, pageDescriptor, list.ws, "fieldExtraList_", "onError", ()->{
									$append(entityMapCode);
									String posArgument = "$"+getTypePositionOnResponse(list.ws, pageDescriptor.type.createListType());
									$("callback.call(context, " + posArgument + ");");
								});
							});
						},");");
					},";");
					if(list.source!=null)
						$("updateListSelects();");
					$("updateList();");
				}});
			});
			exportFile(this, pageDescriptor.path+"/list.html");
		}};
	}
	
	@Override
	protected <X extends AbsCodeBlock> void processGetParams(X METODO, List<JVariable> params) {
		METODO.new B() {{
			$("var params;");
			for(JVariable param : params) {
				if(param.type().isAnnotationPresent(jEntity.class)) {
					EntityClass entidad = context.data.entidades.get(param.type());
					for(EntityField key : entidad.key.getLlaves()) {
						String nombreParam = entidad.key.getLlaves().size()==1 ? ("id"+StringUtils.capitalize(param.name())) : (key.fieldName()+"_"+param.name());
						if(key.type().is(Long.class, long.class)){
							$("var "+nombreParam+" = $(this).data(\"id\");");
						}else if(key.type().is(String.class)){
							$("var "+nombreParam+" = $(this).data(\"id\");");
						}else
							throw new NullPointerException("Entidad no reconocida 2 "+param.type().getSimpleName());
					}
				}
			}
		}};
	}
	
	@Override
	protected <X extends AbsCodeBlock> void processPostParams(X METODO, ContentType contentType, List<JVariable> params) {
		throw new NullPointerException();
	}
	
	private boolean extraDialog(HTMLCode code, JavascriptCode jsCode, AdminPageDescriptor pageDescriptor, int indexWs, AdminWsDescriptor ws, EntityClass entidad) {
		if(!ws.ws.isMultipart() && ws.ws.parametros.stream().filter(f->!f.securityToken && !f.type().is(pageDescriptor.type)).count() == 0) {
			jsCode.new B() {{
				$("var btnExtra"+indexWs+"Click = function(id)",()->{
					if(!entidad.key.isSimple())
						$ifNotNull("id", ()->$("id = JSON.parse(atob(id));"));
					if(ws.ws.unwrappedMethod.isVoid)
						$(pageDescriptor.definerClass.getSimpleName()+"."+ws.ws.name()+"(id, function()",()->{
							$("alert('hecho');");
						},");");
					else if(ws.ws.getReturnType().is(FileDownloadDescriptor.class)) {
						$(pageDescriptor.definerClass.getSimpleName()+"."+ws.ws.name()+"(id);");
					}
					else {
						$(pageDescriptor.definerClass.getSimpleName()+"."+ws.ws.name()+"(id, function(result)",()->{
							if(ws.ws.getReturnType().is(String.class)){
								$("updateList();");
								$("alert(result);");
							}else{
								$("alert('hecho');");
							}
						},");");
					}
				},";");
			}};
			return false;
		}
		final ListOption optionAnn = ws.ws.getAnnotation(ListOption.class);
		List<String> aditionals = new ArrayList<>();
		code.new B() {{
				DIV("id=\"dlg-extra"+indexWs+"\" class=\"modal\" tabindex=\"-1\" role=\"dialog\"",()->{
					DIV("class=\"modal-dialog\" role=\"document\"",()->{
						DIV("class=\"modal-content\"",()->{
							DIV("class=\"modal-header\"",()->{
								$("<h5 class=\"modal-title\">"+optionAnn.name()+"</h5>");
								$("button","type=\"button\" class=\"close\" data-dismiss=\"modal\" aria-label=\"Close\"",()->{
									$("<span aria-hidden=\"true\">&times;</span>");
								});
							});
							DIV("class=\"modal-body\"",()->{
								DIV("id=\"localExtraError"+indexWs+"\" style=\"visibility: hidden;\"",()->{ });
								if(ws.ws.isMultipart()) {
									$("<form id=\"form-extra"+indexWs+"\" action=\""+ws.ws.getPath(descriptor)+"\" method=\"post\" enctype=\"multipart/form-data\">");
								}
								ws.ws.parametros.stream().filter(f->!f.securityToken && !f.type().is(pageDescriptor.type)).forEach(wsp->{
									DIV("class=\"form-group\"",()->{
										$("<label for=\"fieldExtra"+wsp.nombre+"\">"+wsp.nombre+"</label>");
										if(wsp.type().isAnnotationPresent(jEntity.class))
											$("select","class=\"custom-select\" name=\""+wsp.nombre+"\" id=\"fieldExtra"+indexWs+"_2"+wsp.nombre+"\"",()->{
												$("<option disabled selected value>Choose...</option>");
											});
										if(wsp.type().isEnum())
											$("select","class=\"custom-select\" name=\""+wsp.nombre+"\" id=\"fieldExtra"+indexWs+"_2"+wsp.nombre+"\"",()->{
												((JClass)wsp.type()).enumData.valores.forEach(v->{
													String id = v.propiedades.containsKey("id") ? v.propiedades.get("id").toString() : v.name; 
													$("<option value=\"" + id + "\">"+v.name+"</option>");
												});
												$("<option disabled selected value>Choose...</option>");
											});
										else if(wsp.type().isJAnnotationPresent(Email.class))
											$("<input type=\"email\" name=\""+wsp.nombre+"\" class=\"form-control\" id=\"fieldExtra"+indexWs+"_2"+wsp.nombre+"\" placeholder=\"name@example.com\">");
										else if(wsp.type().is(Long.class, Integer.class, long.class, int.class))
											$("<input type=\"number\" name=\""+wsp.nombre+"\" class=\"form-control\" id=\"fieldExtra"+indexWs+"_2"+wsp.nombre+"\" placeholder=\"0\">");
										else if(wsp.type().isJAnnotationPresent(CrystalDate.class)) {
											CrystalDate date = wsp.type().getJAnnotation(CrystalDate.class);
											$("<input class=\"form-control datepicker\" name=\""+wsp.nombre+"\"  id=\"fieldExtra"+indexWs+"_2"+wsp.nombre+"\" placeholder=\""+date.value().userFormat+"\">");
											aditionals.add("$( \"#fieldExtra"+indexWs+"_2"+wsp.nombre+"\" ).datepicker( \"option\", \"dateFormat\", CrystalDates."+StringUtils.capitalize(StringUtils.camelizar(date.value().name()))+ ".prototype.CLIENT_FORMAT);");

										}else $("<input class=\"form-control\" name=\""+wsp.nombre+"\"  id=\"fieldExtra"+indexWs+"_2"+wsp.nombre+"\" placeholder=\"-\">");
									});
								});
								if(ws.ws.isMultipart()) {
									$("<label for=\"fieldExtra"+indexWs+"file_3\">Archivo</label>");
									DIV("class=\"custom-file\"",()->{
										$("<input type=\"file\" name=\"file\" class=\"custom-file-input\" id=\"fieldExtra"+indexWs+"file_3\">");
										$("<label class=\"custom-file-label\" for=\"fieldExtra"+indexWs+"file_3\">Choose file</label>");										
									});
									$("</form>");
								}
									
							});
							DIV("class=\"modal-footer\"",()->{
								$("<button id=\"btn_save_extra"+indexWs+"\" type=\"button\" class=\"btn btn-primary\">Aceptar</button>");
								$("<button type=\"button\" class=\"btn btn-secondary\" data-dismiss=\"modal\">Cancelar</button>");
							});
						});
					});
				});
		}};
		
		
		jsCode.new B() {{
			$("$( \".datepicker\").datepicker();");
			aditionals.forEach(line->$(line));
			$("let onExtraError"+indexWs+" = createOnError(\"#localExtraError"+indexWs+"\");");
			if(ws.source != null)
				$("var updateExtra"+indexWs+"Selects = function(id)",()->{
					if(!entidad.key.isSimple())
						$ifNotNull("id", ()->$("id = JSON.parse(atob(id));"));
					createDataPull(jsCode, pageDescriptor, ws.source, "fieldExtra"+indexWs+"_", "onExtraError"+indexWs, null);
				},";");
			$("$('#btn_save_extra"+indexWs+"').off('click').click(function()",()->{
				$("var id = $(this).data(\"id\");");
				if(!entidad.key.isSimple())
					$ifNotNull("id", ()->$("id = JSON.parse(atob(id));"));
				if(ws.ws.isMultipart()) {
					$("$(\"#form-extra"+indexWs+"\").attr('action', "+pageDescriptor.definerClass.getSimpleName()+"."+ws.ws.name()+"(id).URL());");
					$("$(\"#form-extra"+indexWs+"\").ajaxSubmit({success: function()",()->{
						$("$('#dlg-extra"+indexWs+"').modal('hide');");
					},"});");
				}else {
					createDataPull(jsCode, pageDescriptor, ws.ws, "fieldExtra"+indexWs+"_", "onExtraError"+indexWs, ()->{
						$("$('#dlg-extra"+indexWs+"').modal('hide');");
					});
				}
			},");");
			$("var btnExtra"+indexWs+"Click = function(id)",()->{
				$("$('#btn_save_extra"+indexWs+"').data(\"id\", id);");
				if(ws.ws.isMultipart()) {
					JCrystalWebServiceParam param = ws.ws.parametros.stream().filter(f->f.type().is(pageDescriptor.type)).findFirst().orElse(null);
					$("$('#fieldExtra"+indexWs+"_"+param.nombre+"_2').val(id);");
				}
				if(ws.source != null)
					$("updateExtra"+indexWs+"Selects(id);");
				$("$('#dlg-extra"+indexWs+"').modal('show');");
			},";");
			$("function bytesToSize(bytes)",()->{
				$("var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];");
				$("if (bytes == 0) return '0 Byte';");
				$("var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));");
				$("return Math.round(bytes / Math.pow(1024, i), 2) + ' ' + sizes[i];");
			});
		}};
		return true;
	}
	
	protected void createDataPull(JavascriptCode code, AdminPageDescriptor pageDescriptor, JCrystalWebService ws, String fieldPrefix, String onError, Runnable onSuccess) {
		final EntityClass entidad = pageDescriptor.getEntidad();
		JCrystalWebServiceParam param = ws.parametros.stream().filter(f->f.type().isAnnotationPresent(Post.class)).findFirst().orElse(null);
		JsonLevel level = param == null ? JsonLevel.ID : param.type().getAnnotation(Post.class).level();
		code.new B() {{
				if(param != null)
					$("var $postbody = ",()->{
						descriptor.getFields(entidad).get(level).stream().filter(f->f.editable).forEach(f->{
								if(f.type().is(Long.class, Integer.class, long.class, int.class) || f.type().isEnum())
								$(f.fieldName()+ ": jcParseInt($('#"+fieldPrefix+f.fieldName()+"').val()),");
							else if(f.type().isJAnnotationPresent(CrystalDate.class)) {
								CrystalDate date = f.type().getJAnnotation(CrystalDate.class);
								$(f.fieldName()+ ": new CrystalDates."+StringUtils.capitalize(StringUtils.camelizar(date.value().name()))+"(getPickerDate($('#"+fieldPrefix+f.fieldName()+"').val())),");
							}else
								$(f.fieldName()+ ": $('#"+fieldPrefix+f.fieldName()+"').val(),");
						});
						entidad.manyToOneRelations.stream().filter(f->f.level.level <= level.level && f.editable).forEach(f->{
							$(f.fieldName+ ": $('#"+fieldPrefix+f.fieldName+"').val(),");
						});
					},";");
				StringSeparator params = new StringSeparator(", ");
				ws.parametros.stream().filter(f-> !f.securityToken).forEach(f->{
					if(f.type().isAnnotationPresent(Post.class))
						params.add("$postbody");
					else if(f.type().is(pageDescriptor.type))
						params.add("id");
					else if(f.type().is(Long.class, Integer.class, long.class, int.class))
						params.add("jcParseInt($('#"+fieldPrefix +"2"+ f.nombre + "').val())");
					else if(f.type().isJAnnotationPresent(CrystalDate.class)) {
						CrystalDate date = f.type().getJAnnotation(CrystalDate.class);
						params.add("CrystalDates."+StringUtils.capitalize(StringUtils.camelizar(date.value().name()))+".prototype.format(getPickerDate($('#"+fieldPrefix+"2"+f.nombre+"').val(), CrystalDates."+StringUtils.capitalize(StringUtils.camelizar(date.value().name()))+ ".prototype.CLIENT_FORMAT))");
					}else if(f.type().isAnnotationPresent(jEntity.class)) {
						EntityClass ent = context.data.entidades.get(f.type());
						if(ent.key.isSimple() && ent.key.getKeyTypes().get(0).is(Long.class, long.class))
							params.add("jcParseInt($('#"+fieldPrefix +"2"+ f.nombre + "').val())");
						else throw new NullPointerException("Unssuported key type " + f.type());
					}else
						params.add("$('#"+fieldPrefix +"2"+ f.nombre + "').val()");
				});
				String returnType = WebAdminUtils.getReturnFromReturnType(ws);
				$(pageDescriptor.definerClass.getSimpleName()+"."+ws.name()+"("+(params.isEmpty()?"":(params+", "))+"function("+returnType+")",()->{
					if(onSuccess!=null) {
						onSuccess.run();
					}else {
						final List<IJType> tipos = ws.getReturnType().getInnerTypes();
						BiConsumer<String, TypedField> filler = (prefix, f)->{
							if(ws.getReturnType().isIterable()){
								if(f.type().is(tipos.get(0))) {
									$("var $html = '<option disabled selected value>Choose...</option>';");
									$("for(var i in $0)",()->{
										JsonLevel level = ws.getJsonLevel(f.type());
										if(level == JsonLevel.TOSTRING)
											$("$html += '<option value=\"'+$0[i].id+'\">'+$0[i].tostring+'</option>';");
										else
											$("$html += '<option value=\"'+$0[i].id+'\">'+$0[i].id+'</option>';");
									});
									$("$(\"#"+prefix+f.name()+"\").html($html);");
								}
							}else if(ws.getReturnType().isTupla()) {
								IntStream.range(0, tipos.size()).forEach(e->{
									final IJType tipoLista = tipos.get(e).getInnerTypes().get(0);
									if(f.type().is(tipoLista)) {
										$("var $html = '<option disabled selected value>Choose...</option>';");
										$("for(var i in $"+e+")",()->{
											JsonLevel level = ws.getJsonLevel(f.type());
											if(level == JsonLevel.TOSTRING)
												$("$html += '<option value=\"'+$"+e+"[i].id+'\">'+$"+e+"[i].tostring+'</option>';");
											else
												$("$html += '<option value=\"'+$"+e+"[i].id+'\">'+$"+e+"[i].id+'</option>';");
										});
										$("$(\"#"+prefix+f.name()+"\").html($html);");
									}
								});
							}else throw new NullPointerException("Unsuportted type");
						};
						entidad.manyToOneRelations.stream().filter(f->f.level.level <= level.level && f.editable).forEach(f->filler.accept(fieldPrefix, f));
						ws.parametros.stream().filter(f->!f.type().isAnnotationPresent(Post.class) && !f.securityToken && !f.type().is(pageDescriptor.type)).forEach(f->filler.accept(fieldPrefix+"2", f));
					}
				},", "+onError+");");
		}};
	}
	protected String transformToShow(EntityField f, String accessor, Map<String, String> entityToMap) {
		if(f.type().isAnnotationPresent(jEntity.class)) {
			if(entityToMap.containsKey(f.type().getSimpleName()))
				return "("+accessor+" ? "+entityToMap.get(f.type().getSimpleName())+"["+accessor+"] : '-')";
			else if(f.type().is(FileData.class))
				return "("+accessor+" ? " + accessor + ".name + ' (' + " + accessor + ".mimetype + ') ' + bytesToSize(" + accessor + ".length) : '-')";
			else
				return "("+accessor+" ? \"<button type='button' class='btn btn-light btn-sm' title='Ver id' onClick='alert(\"+" + accessor + "+\")'><i class='fas fa-search'></i></button>\" : '-')";
		}
		if(f.type().isJAnnotationPresent(CrystalDate.class)) {
			return "("+accessor+" ? " + accessor + ".formatClient() : '-')";
		}
		if(f.type().is(Boolean.class, boolean.class))
			return "("+accessor+" == true ? '<i class=\"far fa-check-square\"></i>' : '<i class=\"far fa-square\"></i>'"+")";
		if(f.type().is(String.class))
			return "("+accessor+" || '-')";
		if(f.type().isEnum())
			return "("+accessor+" ? "+accessor+".name || "+accessor+".rawName : '-')";
		if(f.type().isArray()) {
			if(f.type().firstInnerType().isEnum())
				return "(Enum."+f.type().firstInnerType().getSimpleName()+".getFromMask("+accessor+").map(function(val){return val.rawName;}).join(','))";
			
		}
		return accessor;
	}
	private void putOnError(JavascriptCode code) {
		code.new B() {{
				$("let clearError = function(errorId)",()->{
					$("$(errorId || \"#gblErrorAlert\").html('');");
					$("$(errorId || \"#gblErrorAlert\").css(\"visibility\", \"hidden\");");
				},";");
				$("let createOnError = function(errorId)", ()->{
					$("return function(error)", ()->{
						$if("error.type == 'UNAUTHORIZED'",()->{
							$("window.location.replace(\"/admin/index.html\");");
						});
						$else(()->{
							$if("error.code < 100",()->{
								$("$(errorId || \"#gblErrorAlert\").html('<div class=\"alert alert-primary\" role=\"alert\" >'+error.msg+'</div>');");
							});
							$else_if("error.code < 200",()->{
								$("$(errorId || \"#gblErrorAlert\").html('<div class=\"alert alert-warning\" role=\"alert\" >'+error.msg+'</div>');");
							});
							$else(()->{
								$("$(errorId || \"#gblErrorAlert\").html('<div class=\"alert alert-danger\" role=\"alert\" >'+error.msg+'</div>');");
							});
							$("$(errorId || \"#gblErrorAlert\").css(\"visibility\", \"visible\");");
							$(" setTimeout(function(){clearError(errorId);}, 6000);");
						});
					},";");
				});
				$("let onError = createOnError();");
		}};
	}
	protected int getTypePositionOnResponse(JCrystalWebService ws, IJType type) {
		final List<IJType> tipos = ws.getReturnType().getInnerTypes();
		if(ws.getReturnType().is(type))
			return 0;
		else if(ws.getReturnType().getSimpleName().startsWith("Tupla")) {
			IntStream stream = IntStream.range(0, tipos.size()).filter(f -> tipos.get(f).is(type)); 
			int pos = stream.findFirst().orElseThrow(()->new NullPointerException());
			return pos;
		}else throw new NullPointerException();
	}
	protected Map<String, String> obtenerMapRespuesta(AdminPageDescriptor pageDescriptor, JavascriptCode code, JCrystalWebService ws) {
		final List<IJType> tipos = ws.getReturnType().getInnerTypes();
		Map<String, String> entityToMap = new TreeMap<>();
		IntStream.range(0, tipos.size())
			.forEach(pos->{
				if(tipos.get(pos).isSubclassOf(List.class) && !tipos.get(pos).getInnerTypes().get(0).is(pageDescriptor.type)) {
					if(tipos.get(pos).getInnerTypes().get(0).isAnnotationPresent(jEntity.class)){
						IJType entidad = tipos.get(pos).getInnerTypes().get(0);
						JsonLevel level = ws.getJsonLevel(entidad);
						if(level == JsonLevel.TOSTRING) {
							entityToMap.put(entidad.getSimpleName(), "$map.$"+entidad.getSimpleName());
							code.$("$map.$"+entidad.getSimpleName()+" = $"+pos+".reduce(function(map, obj) { map[obj.id] = obj.tostring; return map;}, {});");
						}
						
					}
				}else if(tipos.get(pos).isAnnotationPresent(jEntity.class) && !tipos.get(pos).is(pageDescriptor.type)){
					IJType entidad = tipos.get(pos);
					JsonLevel level = ws.getJsonLevel(entidad);
					if(level == JsonLevel.TOSTRING) {
						entityToMap.put(entidad.getSimpleName(), "$map."+entidad.getSimpleName());
						code.$("$map.$"+entidad.getSimpleName()+" = {};");
						code.$if("$"+pos,()->{
							code.$("$map.$"+entidad.getSimpleName()+"[$"+pos+".id] = $"+pos+";");							
						});
						
					}
				}
		});
		return entityToMap;
	}
}


