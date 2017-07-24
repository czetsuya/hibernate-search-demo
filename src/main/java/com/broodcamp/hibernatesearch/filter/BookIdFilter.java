package com.broodcamp.hibernatesearch.filter;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;

/**
 * @author czetsuya
 **/
public class BookIdFilter extends QueryWrapperFilter {

	public BookIdFilter() {
		super(new TermQuery(new Term("id", "1")));
	}

}