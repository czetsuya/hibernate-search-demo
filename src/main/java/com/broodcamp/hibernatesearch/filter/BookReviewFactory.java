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
public class BookReviewFactory {

	private Logger log = LoggerFactory.getLogger(BookReviewFactory.class);

	private Integer stars;

	public void setStars(Integer stars) {
		this.stars = stars;
	}

	@Factory
	public Filter getFilter() {
		log.debug("bookReviews.stars={}", stars);
		Query query = new TermQuery(new Term("bookReviews.stars", stars.toString()));
		return new CachingWrapperFilter(new QueryWrapperFilter(query));
	}

}
