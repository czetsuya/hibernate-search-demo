package com.broodcamp.hibernatesearch.filter;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.annotations.Factory;

/**
 * @author czetsuya
 **/
public class BookNameFactory {

	private String bookName;

	public void setBookName(String bookName) {
		this.bookName = bookName;
	}

	@Factory
	public Filter getFilter() {
		Query query = new TermQuery(new Term("title", bookName));
		return new CachingWrapperFilter(new QueryWrapperFilter(query));
	}

}
