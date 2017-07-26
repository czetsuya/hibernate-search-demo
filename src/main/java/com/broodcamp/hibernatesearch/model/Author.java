package com.broodcamp.hibernatesearch.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;

@Entity
public class Author {

	@Id
	@Column(name = "id")
	@GeneratedValue
	private Integer id;

	@Column(name = "NAME")
	@Facet(forField = "name_facet")
	@Fields({ @Field(name = "name", index = Index.YES, store = Store.NO, analyze = Analyze.NO),
			@Field(name = "name_facet", store = Store.NO, analyze = Analyze.NO) })
	private String name;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Author [id=" + id + ", name=" + name + "]";
	}

}
