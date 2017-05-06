package com.broodcamp.hibernatesearch.filter;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;

/**
 * @author czetsuya
 **/
public class BookReviewFilter extends QueryWrapperFilter {

	public BookReviewFilter() {
		super(new TermQuery(new Term("bookReviews.stars", "5")));
	}

}