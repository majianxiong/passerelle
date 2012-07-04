/**
 * 
 */
package com.isencia.passerelle.process.model.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import com.isencia.passerelle.process.model.Attribute;
import com.isencia.passerelle.process.model.ResultBlock;
import com.isencia.passerelle.process.model.ResultItem;

/**
 * @author "puidir"
 *
 */
@Entity
@Table(name = "PAS_RESULTITEM")
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class ResultItemImpl<V> implements ResultItem<V> {

	private static final long serialVersionUID = 1L;

	@Column(name = "ID")
	@Id
	@GeneratedValue(generator = "pas_resultitem")
	private Long id;

	@SuppressWarnings("unused")
	@Version
	private int version;
	
	@Column(name = "NAME")
	private String name;

	@Column(name = "VALUE")
	protected String value;
	
	@Column(name = "UNIT")
	private String unit;
	
	@OneToMany(targetEntity = ResultItemAttributeImpl.class, mappedBy = "resultItem", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@MapKey(name = "name")
	private Map<String, Attribute> attributes = new HashMap<String, Attribute>();

	@ManyToOne(targetEntity = ResultBlockImpl.class, fetch = FetchType.LAZY)
	@JoinColumn(name = "RESULTBLOCK_ID")
	private ResultBlockImpl resultBlock;

	@Column(name = "COLOUR", nullable = true)
	private String colour;

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.Identifiable#getId()
	 */
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.NamedValue#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.AttributeHolder#getAttribute(java.lang.String)
	 */
	public Attribute getAttribute(String name) {
		return attributes.get(name);
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.AttributeHolder#putAttribute(com.isencia.passerelle.process.model.Attribute)
	 */
	public Attribute putAttribute(Attribute attribute) {
		return attributes.put(attribute.getName(), attribute);
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.AttributeHolder#getAttributeNames()
	 */
	public Iterator<String> getAttributeNames() {
		return attributes.keySet().iterator();
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.AttributeHolder#getAttributes()
	 */
	public Set<Attribute> getAttributes() {
		return new HashSet<Attribute>(attributes.values());
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.Coloured#getColour()
	 */
	public String getColour() {
		return colour;
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.ResultItem#getUnit()
	 */
	public String getUnit() {
		return unit;
	}

	/* (non-Javadoc)
	 * @see com.isencia.passerelle.process.model.ResultItem#getResultBlock()
	 */
	public ResultBlock getResultBlock() {
		return resultBlock;
	}

}