package jcrystal.main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jcrystal.back.model.ModelSynchronizer;
import jcrystal.configs.clients.ClientConfig;
import jcrystal.configs.server.ServerConfig;
import jcrystal.main.data.ClientBackData;
import jcrystal.main.data.ClientContext;
import jcrystal.main.data.ClientData;
import jcrystal.main.data.ClientInput;
import jcrystal.main.data.ClientOutput;
import jcrystal.reflection.MainGenerator;
import jcrystal.types.loaders.IJClassLoader;
import jcrystal.types.loaders.JClassLoader;
import jcrystal.utils.IJServerClassLoader;

public class JCrystalAttender{
	HttpServletRequest req;
	HttpServletResponse response;
	ClientInput data = new ClientInput();
	final ClientBackData back;
	ClientContext context;
	public JCrystalAttender(ClientBackData back, HttpServletRequest req, HttpServletResponse response) {
		this.req = req;
		this.response = response;
		this.back = back;
	}
	
	public void start() throws IOException {
		try(ObjectInputStream ois = new ObjectInputStream(req.getInputStream())){
			data.SERVER = (ServerConfig)ois.readObject();
			Object clients = ois.readObject();
			if(clients instanceof ClientConfig)
				data.CLIENT = (ClientConfig)clients;
			else
				throw new ClassNotFoundException();
			data.jClassResolver = (JClassLoader)ois.readObject();
			data.CHECKED_CLASSES =  (TreeSet<String>)ois.readObject();
		}catch (ClassNotFoundException|java.io.InvalidClassException e) {
			response.setStatus(500);
			response.getWriter().print("Dependency error. Please try updating your dependencies. Right click on project -> jCrystal -> Add dependencies.");
			return;
		}
		catch (Exception e) {
			e.printStackTrace();
			response.setStatus(500);
			response.getWriter().print("Error");
			return;
		}
		data.jClassResolver.parentClassLoader = new IJServerClassLoader();
		try(ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(response.getOutputStream()))){
			try {
				doProcessing(oos);
			} catch (Exception e) {
				ByteArrayOutputStream sw = new ByteArrayOutputStream();
				e.printStackTrace(new PrintStream(sw));
				oos.writeObject(new String(sw.toByteArray()));
				response.setStatus(500);
				e.printStackTrace();
				return;
			}
			response.setStatus(200);
			oos.writeObject(null);
		}catch (IOException e) {
			response.setStatus(500);
			e.printStackTrace();
			return;
		}
		ModelSynchronizer.synchronize(context);
	}
	private void doProcessing(ObjectOutputStream oos) throws Exception {
		System.out.println("LOADING DONE");
		context = new MainGenerator(back, data, new ClientOutput(oos)).generar();
		System.out.println("PROCESSING DONE");
	}
}
