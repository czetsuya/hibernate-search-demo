package com.broodcamp.hibernatesearch.strategy;

import org.hibernate.search.engine.BoostStrategy;

import com.broodcamp.hibernatesearch.model.BookReview;

/**
 * @author czetsuya
 **/
public class FiveStarBoostStrategy implements BoostStrategy {

	public float defineBoost(Object value) {
		if (value == null || !(value instanceof BookReview)) {
			return 1;
		}

		BookReview customerReview = (BookReview) value;
		if (customerReview.getStars() == 5) {
			return 1.5f;
		} else if (customerReview.getStars() == 4) {
			return 1.4f;
		} else if (customerReview.getStars() == 3) {
			return 1.3f;
		} else if (customerReview.getStars() == 2) {
			return 1.2f;
		} else {
			return 1;
		}
	}

}
