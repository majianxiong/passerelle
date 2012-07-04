/**
 * 
 */
package com.isencia.passerelle.process.model.impl;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.apache.commons.lang.builder.CompareToBuilder;

import com.isencia.passerelle.process.model.Context;
import com.isencia.passerelle.process.model.ContextEvent;

/**
 * @author "puidir"
 *
 */
@Entity
@Table(name = "PAS_CONTEXTEVENT")
public class ContextEventImpl implements ContextEvent {

	private static final long serialVersionUID = 1L;
	
	@Id
	@Column(name = "ID", nullable = false, unique = true, updatable = false)
	@GeneratedValue(generator = "pas_contextevent")
	private Long id;

	@SuppressWarnings("unused")
	@Version
	private int version;
	
	@Column(name = "TOPIC", nullable = false, unique = false, updatable = false)
	private String topic;
	
	@Column(name = "MESSAGE", nullable = true, unique = false, updatable = false)
	private String message;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CREATION_TS", nullable = false, unique = false, updatable = false)
	private Date creationTS;

	// Remark: need to use the implementation class instead of the interface
	// here to ensure jpa implementations like EclipseLink will generate setter methods	
	@ManyToOne(targetEntity = ContextImpl.class, optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "CONTEXT_ID", nullable = true, updatable = true)
	private ContextImpl context;

	public ContextEventImpl() {
	}
	
	public ContextEventImpl(Context context, String topic) {
		this.creationTS = new Date();
		this.context = (ContextImpl)context;
		this.topic = topic;
	}
	
	public ContextEventImpl(Context context, String topic, String message) {
		this(context, topic);
		this.message = message;
	}
	
	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.Identifiable#getId()
	 */
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.Event#getTopic()
	 */
	public String getTopic() {
		return topic;
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.Event#getMessage()
	 */
	public String getMessage() {
		return message;
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.Event#getCreationTS()
	 */
	public Date getCreationTS() {
		return creationTS;
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.Event#getDuration()
	 */
	public Long getDuration() {
		// Irrelevant
		return 0L;
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.ContextEvent#getContext()
	 */
	public Context getContext() {
		return context;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(ContextEvent rhs) {
		ContextEventImpl rhsImpl = (ContextEventImpl)rhs;
		return new CompareToBuilder()
			.append(creationTS, rhsImpl.creationTS)
			.append(topic, rhsImpl.topic).toComparison();
	}

}
