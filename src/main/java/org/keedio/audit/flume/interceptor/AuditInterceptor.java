package org.keedio.audit.flume.interceptor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.slf4j.LoggerFactory;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class AuditInterceptor extends ReceiverAdapter implements Interceptor {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AuditInterceptor.class);
	private boolean audit = true;
	private String jgroupsCfgFile;
	private final String JGROUPS_FILE = "jgroups_conf"; 
	private JChannel channel;
	private String clusterName;
	private final String CLUSTER_NAME = "cluster_name";
	
	public AuditInterceptor(Context context) {
		jgroupsCfgFile = context.getString(JGROUPS_FILE);		
		clusterName = context.getString(CLUSTER_NAME);
	}
	
	@Override
	public void initialize() {
		// Connect to jgroups cluster
		try {
			channel = new JChannel("src/main/resources/tcp.xml"/*jgroupsCfgFile*/);
			channel.connect("audit"/*clusterName*/);
			channel.setReceiver(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOG.error("Can't connect to cluster...");;
		}
	}

	@Override
	public Event intercept(Event event) {
		// Enriquecemos el mensaje con el timestamp y su codigo hash 
		try {
			if (audit) {
				Date now = new Date();
				String message = new String(event.getBody());
				HashFunction hf = Hashing.md5();
				HashCode code = hf.hashString(message, Charset.defaultCharset());
				
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("timestamp", "" + now.getTime());
				headers.put("hashcode", "" + code);
				
				event.setHeaders(headers);
				
				
				
				
				
				
				
				
				
				
				
				
				
				// enviamos al cluster y continuamos con la cadena
				StringBuffer buff = new StringBuffer();
				buff.append("NEW_MESSAGE:").append(Inet4Address.getLocalHost()).append(":").append(headers.get("timestamp")).append(":").append(headers.get("hashcode"));

				channel.send(new Message(null, buff.toString()));
			}
			
		} catch (Exception e) {
			LOG.error("Couldn't send message to cluster");
		}
		
		return event;
	}

    @Override
    public List<Event> intercept(List<Event> events) {
        List<Event> out = new LinkedList<Event>();
        for (Event e : events) {
            out.add(intercept(e));
        }
        return out;
    }

	@Override
	public void close() {
		channel.close();
	}

	@Override
	public void viewAccepted(View view) {
	    LOG.debug("New node to cluster joined: " + view);
	}

	@Override
	public void receive(Message msg) {
		switch ((String)msg.getObject()) {
		case "START_AUDIT":
			audit = true;
			break;
		case "STOP_AUDIT":
			audit = false;
			break;
		default:
			break;
		} 
		
	}

    public static class Builder implements Interceptor.Builder {
        private Context ctx;

        @Override
        public Interceptor build() {
            return new AuditInterceptor(ctx);
        }

        @Override
        public void configure(Context context) {
            this.ctx = context;
        }
    }
	
}
