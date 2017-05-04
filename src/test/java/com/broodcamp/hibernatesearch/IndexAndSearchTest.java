package com.broodcamp.hibernatesearch;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example testcase for Hibernate Search
 */
public class IndexAndSearchTest {

	private EntityManagerFactory emf;

	private EntityManager em;

	private static Logger log = LoggerFactory.getLogger(IndexAndSearchTest.class);

	@Before
	public void setUp() {
		initHibernate();
	}

	@After
	public void tearDown() {
		purge();
	}

	@Test
	public void test1() {
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		em.getTransaction().begin();

		// create native Lucene query using the query DSL
		// alternatively you can write the Lucene query using the Lucene query
		// parser
		// or the Lucene programmatic API. The Hibernate Search DSL is
		// recommended though
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query query = qb.keyword().onFields("title", "subtitle", "authors.name")
				.matching("Java rocks!").createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query persistenceQuery = fullTextEntityManager.createFullTextQuery(query, Book.class);

		// execute search
		List result = persistenceQuery.getResultList();

		em.getTransaction().commit();
		em.close();
	}

	// @Test
	public void testIndexAndSearch() throws Exception {
		List<Book> books = search("hibernate");
		assertEquals("Should get empty list since nothing is indexed yet", 0, books.size());

		index();

		// search by title
		books = search("hibernate");
		assertEquals("Should find one book", 1, books.size());
		assertEquals("Wrong title", "Java Persistence with Hibernate", books.get(0).getTitle());

		// search author
		books = search("\"Gavin King\"");
		assertEquals("Should find one book", 1, books.size());
		assertEquals("Wrong title", "Java Persistence with Hibernate", books.get(0).getTitle());
	}

	// @Test
	public void testStemming() throws Exception {

		index();

		List<Book> books = search("refactor");
		assertEquals("Wrong title", "Refactoring: Improving the Design of Existing Code", books.get(0).getTitle());

		books = search("refactors");
		assertEquals("Wrong title", "Refactoring: Improving the Design of Existing Code", books.get(0).getTitle());

		books = search("refactored");
		assertEquals("Wrong title", "Refactoring: Improving the Design of Existing Code", books.get(0).getTitle());

		books = search("refactoring");
		assertEquals("Wrong title", "Refactoring: Improving the Design of Existing Code", books.get(0).getTitle());
	}

	private void initHibernate() {
		emf = Persistence.createEntityManagerFactory("hibernate-search-example");
		em = emf.createEntityManager();
	}

	private void index() {
		FullTextEntityManager ftEm = org.hibernate.search.jpa.Search.getFullTextEntityManager(em);
		try {
			ftEm.createIndexer().startAndWait();
		} catch (InterruptedException e) {
			log.error("Was interrupted during indexing", e);
		}
	}

	private void purge() {
		FullTextEntityManager ftEm = org.hibernate.search.jpa.Search.getFullTextEntityManager(em);
		ftEm.purgeAll(Book.class);
		ftEm.flushToIndexes();
		ftEm.close();
		emf.close();
	}

	private List<Book> search(String searchQuery) throws ParseException {
		Query query = searchQuery(searchQuery);

		List<Book> books = query.getResultList();

		for (Book b : books) {
			log.info("Title: " + b.getTitle());
		}
		return books;
	}

	private Query searchQuery(String searchQuery) throws ParseException {

		String[] bookFields = { "title", "subtitle", "authors.name", "publicationDate" };

		// lucene part
		Map<String, Float> boostPerField = new HashMap<String, Float>(4);
		boostPerField.put(bookFields[0], (float) 4);
		boostPerField.put(bookFields[1], (float) 3);
		boostPerField.put(bookFields[2], (float) 4);
		boostPerField.put(bookFields[3], (float) .5);

		// FullTextEntityManager ftEm =
		// org.hibernate.search.jpa.Search.getFullTextEntityManager( em );
		// Analyzer customAnalyzer = ftEm.getSearchFactory().getAnalyzer(
		// "customanalyzer" );
		// QueryParser parser = new MultiFieldQueryParser(
		// Version.LUCENE_34, bookFields,
		// customAnalyzer, boostPerField
		// );
		//
		// org.apache.lucene.search.Query luceneQuery;
		// luceneQuery = parser.parse( searchQuery );
		//
		// final FullTextQuery query = ftEm.createFullTextQuery( luceneQuery,
		// Book.class );

		// return query;
		return null;
	}
}
