package com.broodcamp.hibernatesearch.model;

import javax.persistence.Embeddable;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.DynamicBoost;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

import com.broodcamp.hibernatesearch.strategy.FiveStarBoostStrategy;

/**
 * @author czetsuya
 **/
@Embeddable
@AnalyzerDef(name = "bookReviewAnalyzer", charFilters = {
		@CharFilterDef(factory = HTMLStripCharFilterFactory.class) }, tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class), filters = {
				@TokenFilterDef(factory = LowerCaseFilterFactory.class),
				@TokenFilterDef(factory = StandardFilterFactory.class),
				@TokenFilterDef(factory = StopFilterFactory.class) })
@DynamicBoost(impl = FiveStarBoostStrategy.class)
public class BookReview {

	@Field
	private String username;

	@Fields({ @Field(name = "stars", analyze = Analyze.YES) })
	@NumericFields({ @NumericField(forField = "stars") })
	private int stars;

	@Field
	@Analyzer(definition = "bookReviewAnalyzer")
	private String comments;

	public int getStars() {
		return stars;
	}

	public void setStars(int stars) {
		this.stars = stars;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String toString() {
		return "BookReview [username=" + username + ", stars=" + stars + ", comments=" + comments + "]";
	}

}
