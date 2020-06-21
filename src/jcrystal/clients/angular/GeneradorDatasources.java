package jcrystal.clients.angular;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.web.IWServiceEndpoint;
import jcrystal.reflection.annotations.ws.DataSource;
import jcrystal.types.IJType;
import jcrystal.utils.StreamUtils;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.AbsCodeBlock.PL;
import jcrystal.utils.langAndPlats.TypescriptCode;

public class GeneradorDatasources {
	WebClientTypescript parent;
	public GeneradorDatasources(WebClientTypescript parent) {
		this.parent = parent;
	}
	private void generarUtils(String name, IWServiceEndpoint ws, PL params, IJType type, TypescriptCode code) {
		code.new B() {{
			$("observe" + StringUtils.capitalize(name) + "(observer : (data : "+$($convert(type))+")=>void, onError : (error : any)=>void = null) : Subscription",()->{
				$if("onError",()->{
					$("return this._"+name+".asObservable().subscribe(",()->{ 
						$("next: x => observer(x),"); 
						$("error: err => onError(err),"); 
					},");");								
				});
				$else(()->{
					$("return this._"+name+".asObservable().subscribe(",()->{ 
						$("next: x => observer(x),"); 
					},");");
				});
			});
			$(name + "("+params.collect(f->f.name()+":"+f.type(), "observer : (data : "+$($convert(ws.getReturnType()))+")=>void") + ", onError : (error : any)=>void = null) : Subscription",()->{
				$if("!this."+ws.name()+"_initiated",()->{
					$("this.refresh"+StringUtils.capitalize(ws.name())+"("+params.collect(f->f.name())+");");
				});
				$if("onError",()->{
					$("return this._" + name + ".asObservable().subscribe(",()->{ 
						$("next: x => observer(x),"); 
						$("error: err => onError(err),"); 
					},");");								
				});
				$else(()->{
					$("return this._" + name + ".asObservable().subscribe(",()->{ 
						$("next: x => observer(x),"); 
					},");");
				});
			});
			$("set"+StringUtils.capitalize(name)+"(val : "+$($convert(type))+")",()->{
				$("this._"+name+".next(val);");
			});
			
			if(type.isIterable()) {
				$("addTo"+StringUtils.capitalize(name)+"(val : "+$($convert(type.getInnerTypes().get(0)))+")",()->{
					$("var subscription = this._" + name + ".asObservable().pipe(take(1)).subscribe(",()->{ 
						$("next: x => ",()->{
							$("var copy : any = Object.assign([], x);");
							$("copy.push(val);");
							$("this._"+name+".next(copy);");
						},",");  
					},");");
					$("subscription.unsubscribe();");
				});
			}
		}};
	}
	public void generar(String name, Set<IWServiceEndpoint> endpoints) {
		if(!endpoints.stream().anyMatch(f->f.isAnnotationPresent(DataSource.class)))
			return;
		String prefixName = StringUtils.capitalize(StringUtils.camelizarSoft(name.contains("/") ? name.substring(name.lastIndexOf('/') + 1) : name));
		new TypescriptCode(){{
			
			$("import { Injectable } from '@angular/core';");
			$("import { Observable, ReplaySubject, Observer, Subscription } from 'rxjs';");
			$("import { take } from 'rxjs/operators';");
			$("import { HttpClient } from '@angular/common/http';");
			$("import { Manager"+prefixName+" } from './Manager"+prefixName+"';");
			$("@Injectable({ providedIn: 'root',})");
			$("export class " + prefixName + "Service", ()->{
				$("constructor(public http : HttpClient) { }");
				endpoints.stream().filter(f->f.isAnnotationPresent(DataSource.class)).forEach(ws->{
					DataSource annotation = ws.getAnnotation(DataSource.class);
					
					if (ws.getReturnType().is(Void.TYPE))
						throw new NullPointerException("Un datasource no puede ser de retorno void") ;
					else if (ws.getReturnType().isTupla()) {
						if(annotation.value().length == 0)
							throw new NullPointerException("Una tupla no puede ser un DataSource si no se le colocan nombres a sus elementos eg: DataSource({\"e1\",\"e2\",...})") ;
						if(annotation.value().length != ws.getReturnType().getInnerTypes().size())
							throw new NullPointerException("Una datasource de tipo tupla debe tener definidos todos sus nombres expected: " +ws.getReturnType().getInnerTypes().size()+", got: " + annotation.value().length + ")") ;
							
						StreamUtils.forEachWithIndex(ws.getReturnType().getInnerTypes(), (i,p)-> {
							if(annotation.value()[i] != null && !annotation.value()[i].trim().isEmpty()) {
								$("private " + annotation.value()[i]+ "_initiated : boolean = false;");
								$("private _" + annotation.value()[i]+ " : ReplaySubject<" + $($convert(p))+"> = new ReplaySubject();");
							}
						});
					}else if(ws.getReturnType().is(jcrystal.server.FileDownloadDescriptor.class, File.class))
						throw new NullPointerException("Invalid type FileDownloadDescriptor for datasource") ;
					else {
						$("private "+ws.name() + "_initiated : boolean = false;");
						$("private _"+ws.name() + " : ReplaySubject<" + $($convert(ws.getReturnType()))+"> = new ReplaySubject();");
					}
				});
				endpoints.stream().filter(f->f.isAnnotationPresent(DataSource.class)).forEach(ws->{
					
					DataSource annotation = ws.getAnnotation(DataSource.class);
					PL params = new PL();
					ws.getParameters().stream().filter(f->f.tipoRuta.isSentByClient()).forEach(f->{
						if(parent.context.data.entidades.contains(f.p.type())) {
							EntityClass entidad = parent.context.data.entidades.get(f.p.type());
							if(entidad.key.isSimple()) {
								EntityField key = entidad.key.getLlaves().get(0); 
								String nombre = key.getWebServiceName(entidad, f);
								if(key.type().is(Long.class, long.class))
									params.add(P(key.type(), nombre));
								else if(key.type().is(String.class))
									params.add(P(key.type(), nombre));
								else
									throw new NullPointerException("Invalid type " + key.type()) ;
							}else
								params.add(P(entidad.key.getSingleKeyType(), f.nombre));
						}else
							params.add(P(f.p.type(), f.nombre));
					});
					
					if (ws.getReturnType().isTupla()) {
						StreamUtils.forEachWithIndex(ws.getReturnType().getInnerTypes(), (i,p)-> {
							if(annotation.value()[i] != null && !annotation.value()[i].trim().isEmpty())
								generarUtils(annotation.value()[i], ws, params, p, this);
						});
						$("refresh"+StringUtils.capitalize(ws.name())+"("+params.collect(f->f.name()+":"+f.type())+")",()->{
							String vals = IntStream.range(0, ws.getReturnType().getInnerTypes().size()).mapToObj(f->"p"+f).collect(Collectors.joining(", "));
							$("Manager" + prefixName+"."+ws.name()+"(" + params.collect("this", f->f.name()) + ", ("+vals+") => ",()->{
								StreamUtils.forEachWithIndex(ws.getReturnType().getInnerTypes(), (i,p)-> {
									$("this." + annotation.value()[i] + "_initiated = true;");
								});
								StreamUtils.forEachWithIndex(ws.getReturnType().getInnerTypes(), (i,p)-> {
									if(annotation.value()[i] != null && !annotation.value()[i].trim().isEmpty()) {
										$("this._" + annotation.value()[i] + ".next(p"+i+");");
									}
								});
							},", error =>{})");
						});
					}else {
						generarUtils(ws.name(), ws, params, ws.getReturnType(), this);
						$("refresh"+StringUtils.capitalize(ws.name())+"("+params.collect(f->f.name()+":"+f.type())+")",()->{
							$("this."+ws.name()+"_initiated = true;");
							String ps = params.collect("this", f->f.name());
							$("Manager" + prefixName+"."+ws.name()+"(" + ps + ", result => {this._"+ws.name()+".next(result);}, error =>{})");
						});
					}
				});
				
			});
			
			for(final IWServiceEndpoint endPoint : endpoints)
				endPoint.gatherRequiredTypes(imports);
			
			int numSlash =  name.length() - name.replace("/", "").length();
			$imports(numSlash + 1);
			
			String paquete = WebClientTypescript.paqueteServicios;
			if(name.contains("/"))
				paquete+= "."+name.substring(0, name.lastIndexOf('/')).replace("/", ".");
			parent.exportFile(this, paquete.replace(".", File.separator) + File.separator + prefixName + "Service.ts");
		}};
	}
}
