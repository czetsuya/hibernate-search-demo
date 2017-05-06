package com.broodcamp.hibernatesearch.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

import com.broodcamp.hibernatesearch.filter.BookNameFactory;
import com.broodcamp.hibernatesearch.filter.BookReviewFilter;

@Entity
@AnalyzerDef(name = "customanalyzer", charFilters = {
		@CharFilterDef(factory = HTMLStripCharFilterFactory.class) }, tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class), filters = {
				@TokenFilterDef(factory = StandardFilterFactory.class),
				@TokenFilterDef(factory = StopFilterFactory.class),
				@TokenFilterDef(factory = LowerCaseFilterFactory.class),
				@TokenFilterDef(factory = SnowballPorterFilterFactory.class, params = {
						@Parameter(name = "language", value = "English") }) })
@Indexed
@Boost(2f)
@FullTextFilterDefs({ @FullTextFilterDef(name = "bookReviewFilter", impl = BookReviewFilter.class),
		@FullTextFilterDef(name = "bookNameFilter", impl = BookNameFactory.class) })
public class Book {

	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;

	@Column(name = "TITLE")
	@Fields({ @Field(store = Store.COMPRESS), @Field(name = "sorting_title", analyze = Analyze.NO) })
	@Analyzer(definition = "customanalyzer")
	@Boost(1.5f)
	@SortableField
	private String title;

	@Column(name = "SUB_TITLE")
	@Field(store = Store.YES, termVector = TermVector.YES)
	@Analyzer(definition = "customanalyzer")
	private String subTitle;

	@IndexedEmbedded
	@ManyToMany
	@JoinTable(name = "BOOK_AUTHOR", joinColumns = @JoinColumn(name = "BOOK_ID", referencedColumnName = "ID"), inverseJoinColumns = @JoinColumn(name = "AUTHOR_ID", referencedColumnName = "ID"))
	private Set<Author> authors = new HashSet<Author>();

	@Temporal(TemporalType.DATE)
	@Column(name = "PUBLICATION_DATE")
	@Field(analyze = Analyze.NO, store = Store.YES)
	@DateBridge(resolution = Resolution.DAY)
	private Date publicationDate;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "BOOK_REVIEW", joinColumns = @JoinColumn(name = "BOOK_ID"))
	@Fetch(FetchMode.SELECT)
	@IndexedEmbedded(depth = 1, includePaths = { "stars", "comments" })
	private Set<BookReview> bookReviews;

	public Book() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubtitle() {
		return subTitle;
	}

	public void setSubtitle(String subTitle) {
		this.subTitle = subTitle;
	}

	public Set<Author> getAuthors() {
		return authors;
	}

	public void setAuthors(Set<Author> authors) {
		this.authors = authors;
	}

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}

	public String getSubTitle() {
		return subTitle;
	}

	public void setSubTitle(String subTitle) {
		this.subTitle = subTitle;
	}

	public Set<BookReview> getBookReviews() {
		return bookReviews;
	}

	public void setBookReviews(Set<BookReview> bookReviews) {
		this.bookReviews = bookReviews;
	}

	@Override
	public String toString() {
		return "Book [id=" + id + ", title=" + title + ", subTitle=" + subTitle + ", authors=" + authors
				+ ", publicationDate=" + publicationDate + ", bookReviews=" + bookReviews + "]";
	}
}
