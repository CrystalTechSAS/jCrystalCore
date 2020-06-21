package jcrystal.main.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import jcrystal.types.JClass;
import jcrystal.configs.clients.ResourceType;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.preprocess.responses.ClassOperation;
import jcrystal.preprocess.responses.ClassOperationType;
import jcrystal.preprocess.responses.OutputFile;
import jcrystal.preprocess.responses.OutputSection;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.AbsICodeBlock;

public class ClientOutput {

	ObjectOutputStream oos;
	public ClientOutput(ObjectOutputStream oos) {
		super();
		this.oos = oos;
	}


	public void send(Serializable file) {
		try {
			oos.writeObject(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void send(AbsCodeBlock resource, String path){
		send(new OutputFile(null, null, path, resource.getCode()));
	}
	public void send(String resource, String path){
		send(null, resource, path);
	}
	public void send(ResourceType type, String resource, String path){
		send(new OutputFile(null, null, path, resource).setResourceType(type));
	}
	public void add(ClassOperationType type, JClass clase, String content){
		send(new ClassOperation(type, clase.name.replace(".", "/")+".java", content));
	}
	public void addSection(JClass clase, String id, AbsICodeBlock content){
		send(new OutputSection(clase.name.replace(".", "/")+".java", id, content.getCode()));
	}
	public void addGlobalSection(JClass clase, String id, AbsICodeBlock content){
		send(new OutputSection(clase.name.replace(".", "/")+".java", id, content.getCode()).setGlobal(true));
	}
	public void addResource(InputStream resource, String path) throws Exception{
		StringWriter sw = new StringWriter();
		try(BufferedReader br = new BufferedReader(new InputStreamReader(resource)); PrintWriter pw = new PrintWriter(sw)){
			for(String line; (line = br.readLine())!=null; )
				pw.println(line);
		}
		send(new OutputFile(null, null, path, sw.toString()));
	}
	public void exportFile(BackendWrapper back, AbsCodeBlock codigo, String path){
		if(back != null)
			send(new OutputFile(back.id, null, path, codigo.getCode()));
		else
			send(new OutputFile(null, null, path, codigo.getCode()));
	}
	public void exportFile(AbsCodeBlock codigo, String path){
		exportFile(null, codigo, path);
	}
}
