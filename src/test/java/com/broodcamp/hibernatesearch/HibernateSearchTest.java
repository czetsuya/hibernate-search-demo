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

import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

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
						FiveStarBoostStrategy.class)
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
	public void testSimpleSortedJPALuceneSearch() {
		log.info("testSimpleSortedJPALuceneSearch");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().onFields("title", "subTitle", "authors.name")
				.matching("Programmers").createQuery();

		// wrap Lucene query in a javax.persistence.Query
		FullTextQuery fullTextQueryJPA = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);
		fullTextQueryJPA = fullTextQueryJPA.setSort(new Sort(new SortField("sorting_title", SortField.Type.STRING)));

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

		// --

		log.info("fuzzy weighted");

		luceneQuery = qb.keyword().fuzzy().withEditDistanceUpTo(1).onFields("title").boostedTo(2).andField("subTitle")
				.andField("authors.name").matching("\"Programmers\"").createQuery();

		jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

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

		// execute search
		List<Book> result = (List<Book>) fullTextQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(4, result.size());
	}

}
