/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.broodcamp.hibernatesearch;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.broodcamp.hibernatesearch.bridge.BigDecimalNumericFieldBridge;
import com.broodcamp.hibernatesearch.filter.BookNameFactory;
import com.broodcamp.hibernatesearch.filter.BookReviewFactory;
import com.broodcamp.hibernatesearch.filter.BookReviewFilter;
import com.broodcamp.hibernatesearch.model.Author;
import com.broodcamp.hibernatesearch.model.Book;
import com.broodcamp.hibernatesearch.model.BookReview;
import com.broodcamp.hibernatesearch.strategy.FiveStarBoostStrategy;

@RunWith(Arquillian.class)
public class HibernateSearchTest {

	private Logger log = Logger.getLogger(this.getClass().getName());

	@Inject
	private EntityManager em;

	@Deployment
	public static Archive<?> createTestArchive() {
		return ShrinkWrap.create(WebArchive.class, "test.war")
				.addClasses(Author.class, Book.class, BookReview.class, Resources.class, StartupListener.class,
						FiveStarBoostStrategy.class, BookReviewFilter.class, BookReviewFactory.class,
						BookNameFactory.class, BigDecimalNumericFieldBridge.class)
				.addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
				.addAsResource("import.sql", "import.sql").addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
				// Deploy our test datasource
				.addAsWebInfResource("test-ds.xml", "test-ds.xml");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSimpleJPALuceneSearch() {
		log.info("testSimpleJPALuceneSearch");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		// create native Lucene query using the query DSL
		// alternatively you can write the Lucene query using the Lucene query
		// parser
		// or the Lucene programmatic API. The Hibernate Search DSL is
		// recommended though
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().onFields("title", "subTitle", "authors.name")
				.matching("Programmers").createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(9, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSorting() {
		log.info("testSorting");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().onFields("title", "subTitle", "authors.name")
				.matching("Programmers").createQuery();

		Sort sort = new Sort(SortField.FIELD_SCORE, new SortField("sorting_title", SortField.Type.STRING));

		// wrap Lucene query in a javax.persistence.Query
		FullTextQuery fullTextQueryJPA = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);
		fullTextQueryJPA = fullTextQueryJPA.setSort(sort);

		// execute search
		List<Book> result = (List<Book>) fullTextQueryJPA.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(9, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMoreLikeThis() {
		log.info("testMoreLikeThis");

		Book book = em.find(Book.class, 14);
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.moreLikeThis().comparingFields("title").toEntity(book)
				.createQuery();
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(5, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMoreLikeThisProjection() {
		log.info("testMoreLikeThisProjection");

		Book book = em.find(Book.class, 14);
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.moreLikeThis().comparingFields("title").toEntity(book)
				.createQuery();
		List<Object> result = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class)
				.setProjection(ProjectionConstants.THIS, ProjectionConstants.SCORE).getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(5, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testBookReview() {
		log.info("testBookReview");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().onField("bookReviews.comments")
				.matching("interesting").createQuery();
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(2, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testWeighted() {
		log.info("testWeighted");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.phrase().onField("title").boostedTo(2).andField("subTitle")
				.andField("authors.name").sentence("\"Programmers\"").createQuery();

		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(9, result.size());

		// --

		log.info("fuzzy weighted");

		luceneQuery = qb.keyword().fuzzy().withEditDistanceUpTo(1).withPrefixLength(1).onFields("title").boostedTo(2)
				.andField("subTitle").andField("authors.name").matching("\"Programmers\"").createQuery();

		jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(10, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testHibernateSearchPaging() {
		log.info("testHibernateSearchPaging");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().onFields("title", "subTitle", "authors.name")
				.matching("Programmers").createQuery();

		// wrap Lucene query in a javax.persistence.Query
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		fullTextQuery.setFirstResult(5);
		fullTextQuery.setMaxResults(5);

		assertEquals(9, fullTextQuery.getResultSize());

		// execute search
		List<Book> result = (List<Book>) fullTextQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle()));

		assertEquals(4, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testWildCard() {
		log.info("testWildCard");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().wildcard().onFields("title", "subTitle")
				.matching("*theo*").createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle() + " " + p.getSubtitle()));

		assertEquals(2, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPhrase() {
		log.info("testPhrase");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.phrase().onField("subTitle").sentence("best practices")
				.createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(1, result.size());

		log.info("testPhrase");

		// --
		log.info("testPhrase withSlop");

		luceneQuery = qb.phrase().withSlop(1).onField("subTitle").sentence("core pattern").createQuery();

		// wrap Lucene query in a javax.persistence.Query
		jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getSubtitle()));

		assertEquals(1, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testStarred() {
		log.info("testStarred");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.range().onField("bookReviews.stars").from(4).to(5)
				.excludeLimit().createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> p.getBookReviews().forEach(q -> log.info("" + q.getStars())));

		assertEquals(3, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDateRangeAbove() {
		log.info("testDateRangeAbove");

		Calendar c = Calendar.getInstance();
		c.set(2000, 01, 01);

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.range().onField("publicationDate").above(c.getTime())
				.createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info("" + p.getPublicationDate()));

		assertEquals(50, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDateRangeBetween() {
		log.info("testDateRangeBetween");

		Calendar c = Calendar.getInstance();
		c.set(2005, 01, 01);

		Date from = c.getTime();

		c.set(2010, 01, 01);
		Date to = c.getTime();

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.range().onField("publicationDate").from(from).to(to)
				.createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info("" + p.getPublicationDate()));

		assertEquals(28, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAndOr() {
		log.info("testAndOr");

		Calendar c = Calendar.getInstance();
		c.set(2005, 01, 01);

		Date from = c.getTime();

		c.set(2010, 01, 01);
		Date to = c.getTime();

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();

		org.apache.lucene.search.Query luceneQuery = qb.bool()
				.must(qb.keyword().onField("subTitle").matching("java").createQuery())
				.must(qb.keyword().onFields("title", "subTitle").matching("javascript").createQuery()).not()
				.must(qb.range().onField("publicationDate").from(from).to(to).createQuery())
				.must(qb.range().onField("bookReviews.stars").above(3).createQuery()).createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle() + " | " + p.getSubtitle() + " | " + p.getPublicationDate()));

		assertEquals(2, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMoreOptions() {
		log.info("testMoreOptions");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().onFields("subTitle", "authors.name").andField("title")
				.ignoreAnalyzer().matching("Programmers").createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);
		// raise an exception
		jpaQuery.setHint("javax.persistence.query.timeout", 400);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle() + " | " + p.getSubtitle()));

		assertEquals(4, result.size());
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testProjection() {
		log.info("testProjection");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.range().onField("bookReviews.stars").above(4).createQuery();

		// wrap Lucene query in a javax.persistence.Query
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);
		fullTextQuery.setProjection("subTitle", "authors.name");
		fullTextQuery.initializeObjectsWith(ObjectLookupMethod.SECOND_LEVEL_CACHE, DatabaseRetrievalMethod.QUERY);
		// return whatever you have
		fullTextQuery.limitExecutionTimeTo(500, TimeUnit.MILLISECONDS);

		// execute search
		List results = fullTextQuery.getResultList();

		log.info("Record found=" + results.size());
		Object[] firstResult = (Object[]) results.get(0);
		String title = (String) firstResult[0];
		log.info("title=" + title);

		assertEquals(6, results.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFilter() {
		log.info("testFilter");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.all().createQuery();

		// wrap Lucene query in a javax.persistence.Query
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);
		fullTextQuery.enableFullTextFilter("bookReviewFilter");

		// execute search
		List<Book> result = (List<Book>) fullTextQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle() + " | " + p.getSubtitle()));

		// assertEquals(4, result.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFilterFactory() {
		log.info("testFilterFactory");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.all().createQuery();

		// wrap Lucene query in a javax.persistence.Query
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);
		fullTextQuery.enableFullTextFilter("bookNameFilter").setParameter("bookName", "programming");

		// execute search
		List<Book> result = (List<Book>) fullTextQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle() + " | " + p.getSubtitle()));

		// assertEquals(4, result.size());
	}

	@Test
	public void testDiscreetFacet() {
		log.info("testDiscreetFacet");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();

		org.apache.lucene.search.Query luceneQuery = qb.all().createQuery();
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		FacetingRequest authorFacet = qb.facet().name("authorFacetRequest").onField("authors.name_facet").discrete()
				.orderedBy(FacetSortOrder.FIELD_VALUE).includeZeroCounts(false).createFacetingRequest();

		// retrieve facet manager and apply faceting request
		FacetManager facetManager = fullTextQuery.getFacetManager();
		facetManager.enableFaceting(authorFacet);

		// retrieve the faceting results
		List<Facet> facets = facetManager.getFacets("authorFacetRequest");
		facets.forEach(p -> log.info(p.getValue() + " - " + p.getCount()));

		Facet x = facets.stream().filter(p -> p.getValue().equals("Stephen King")).findFirst().get();
		assertEquals(3, x.getCount());
	}

	@Test
	public void testRangeFacet() {
		log.info("testRangeFacet");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();

		org.apache.lucene.search.Query luceneQuery = qb.all().createQuery();
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		FacetingRequest priceFacet = qb.facet().name("priceFacetRequest").onField("price").range().below(500).from(500)
				.to(1000).above(1000).orderedBy(FacetSortOrder.COUNT_DESC).includeZeroCounts(true)
				.createFacetingRequest();

		// retrieve facet manager and apply faceting request
		FacetManager facetManager = fullTextQuery.getFacetManager();
		facetManager.enableFaceting(priceFacet);

		// retrieve the faceting results
		List<Facet> facets = facetManager.getFacets("priceFacetRequest");
		facets.forEach(p -> log.info(p.getValue() + " - " + p.getCount()));

		assertEquals(3, facets.size());
	}

}