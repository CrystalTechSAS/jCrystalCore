package jcrystal.main.data;

import java.io.InputStream;

import jcrystal.configs.clients.ResourceType;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.preprocess.responses.ClassOperationType;
import jcrystal.preprocess.responses.OutputFile;
import jcrystal.types.JClass;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.AbsICodeBlock;

public interface IClientOutput {

	void send(OutputFile file);
	
	void send(AbsCodeBlock resource, String path);

	void send(String resource, String path);

	void send(ResourceType type, String resource, String path);

	void add(ClassOperationType type, JClass clase, String content);

	void addSection(JClass clase, String id, AbsICodeBlock content);

	void addGlobalSection(JClass clase, String id, AbsICodeBlock content);

	void addResource(InputStream resource, String path) throws Exception;

	void exportFile(BackendWrapper back, AbsCodeBlock codigo, String path);

	void exportFile(AbsCodeBlock codigo, String path);

}