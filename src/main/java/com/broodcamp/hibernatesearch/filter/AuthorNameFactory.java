package com.broodcamp.hibernatesearch.filter;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.annotations.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author czetsuya
 **/
public class AuthorNameFactory {

	private Logger log = LoggerFactory.getLogger(AuthorNameFactory.class);

	private String authorName;

	@Factory
	public Filter getFilter() {
		log.debug("AuthorNameFactory.name={}", authorName);
		Query query = new TermQuery(new Term("authors.name", authorName));
		return new CachingWrapperFilter(new QueryWrapperFilter(query));
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

}
