package org.keedio.audit.agent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.input.SwappedDataInputStream;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.keedio.audit.agent.entity.AuditEntity;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.impetus.client.cassandra.common.CassandraConstants;

public class AuditAgent extends ReceiverAdapter {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AuditAgent.class);
	private static JChannel channel;
	private static ServerSocket ss;
	
	public AuditAgent(String fileConf) {
		try {
			// Nos conectamos el cluster
			channel = new JChannel(fileConf);
			channel.setReceiver(this);
			channel.connect("audit");
			
			// Y levantamos el servidor de sockets para manager
			ss = new ServerSocket(1111);
						
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("Couldn't connect to cluster...");
		}
	}
	
	@Override
	public void viewAccepted(View view) {
	    System.out.println("New node joined to cluster: " + view);
	}

	@Override
	public void receive(Message msg) {
		System.out.println(msg.getObject());
		String message = (String)msg.getObject();
		String command = new StringTokenizer(message, ":").nextToken();
		
		switch (command) {
		case "NEW_MESSAGE":
			List<String> items = IteratorUtils.toList(Splitter.on(":").trimResults().omitEmptyStrings().split(message).iterator());
			AuditEntity entity = new AuditEntity();
			entity.setMessageFrom(items.get(1));
			entity.setTimestamp(Long.parseLong(items.get(2)));
			entity.setHashCode(items.get(3).toString());
			
			// Esto es importante de poner para que funcione
			Map<String, String> propertyMap = new HashMap<String, String>();
		    propertyMap.put(CassandraConstants.CQL_VERSION, CassandraConstants.CQL_VERSION_3_0);
			EntityManagerFactory emf = Persistence.createEntityManagerFactory("cassandra_pu", propertyMap);
	        EntityManager em = emf.createEntityManager();

	        em.persist(entity);
	        em.close();    
	        emf.close();    
			
			LOG.debug(message + " Have to persist ASAP. Done!!!");
		default:
			break;
		} 
		
	}
	
	public static void main(String[] args) throws Exception {
		AuditAgent ag = new AuditAgent("src/main/resources/tcp.xml");
		
		while (true) {
			Socket clientSocket = ss.accept();
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		    String sWhatever = in.readLine();
			//while ((sWhatever = in.readLine()) != null) {
				
		    	System.out.println(sWhatever);
				channel.send(new Message(null, sWhatever));
				clientSocket.close();
			//}

		}
	}
	
}
