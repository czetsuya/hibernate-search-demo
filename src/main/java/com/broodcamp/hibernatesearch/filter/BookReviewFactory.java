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
public class BookReviewFactory {

	private Integer stars;

	@Factory
	public Filter getFilter() {
		Query query = new TermQuery(new Term("bookReviews.stars", stars.toString()));
		return new CachingWrapperFilter(new QueryWrapperFilter(query));
	}

	public void setStars(Integer stars) {
		this.stars = stars;
	}

}
