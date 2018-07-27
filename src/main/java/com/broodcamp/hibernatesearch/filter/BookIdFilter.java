package com.broodcamp.hibernatesearch.filter;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.annotations.Factory;

/**
 * @author czetsuya
 **/
public class BookIdFilter {

	private Long id;

	public void setId(Long id) {
		this.id = id;
	}

	@Factory
	public Query create() {
		return new TermQuery(new Term("id", String.valueOf(id)));
	}

}