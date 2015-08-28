package org.keedio.audit.agent.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "messages", schema = "audit@cassandra_pu")
public class AuditEntity implements Serializable{

	@Column(name="timestamp")
	private long timestamp;
	@Id
	@Column(name="hashcode")
	private String hashCode;
	@Column(name="messagefrom")
	private String messageFrom;
	 
	public String getMessageFrom() {
		return messageFrom;
	}
	public void setMessageFrom(String messageFrom) {
		this.messageFrom = messageFrom;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public String getHashCode() {
		return hashCode;
	}
	public void setHashCode(String hashCode) {
		this.hashCode = hashCode;
	}
	
	
}
