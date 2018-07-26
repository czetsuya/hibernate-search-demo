package com.broodcamp.hibernatesearch;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.Session;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSelection;
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
import com.broodcamp.hibernatesearch.filter.AuthorNameFactory;
import com.broodcamp.hibernatesearch.filter.BookIdFilter;
import com.broodcamp.hibernatesearch.filter.BookNameFactory;
import com.broodcamp.hibernatesearch.filter.BookReviewFactory;
import com.broodcamp.hibernatesearch.model.Author;
import com.broodcamp.hibernatesearch.model.Book;
import com.broodcamp.hibernatesearch.model.BookReview;
import com.broodcamp.hibernatesearch.model.EntityFacet;
import com.broodcamp.hibernatesearch.strategy.FiveStarBoostStrategy;

/**
 * @author czetsuya
 */
@RunWith(Arquillian.class)
public class HibernateSearchTest {

	private Logger log = Logger.getLogger(this.getClass().getName());

	@Inject
	private EntityManager em;

	@Deployment
	public static Archive<?> createTestArchive() {
		return ShrinkWrap.create(WebArchive.class, "test.war")
				.addClasses(Author.class, Book.class, BookReview.class, Resources.class, StartupListener.class, FiveStarBoostStrategy.class, BookIdFilter.class,
						BookReviewFactory.class, BookNameFactory.class, BigDecimalNumericFieldBridge.class, AuthorNameFactory.class, EntityFacet.class)
				.addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
				.addAsResource("jboss-deployment-structure.xml", "WEB-INF/jboss-deployment-structure.xml").addAsResource("import.sql", "import.sql")
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
				// Deploy our test datasource
				.addAsWebInfResource("test-ds.xml", "test-ds.xml");
	}

	/**
	 * Simple keyword search. Search can be perform on multiple fields.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSimpleJPALuceneSearch() {
		log.info("---------------------------------------------------------------------");
		log.info("testSimpleJPALuceneSearch");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		// create native Lucene query using the query DSL
		// alternatively you can write the Lucene query using the Lucene query
		// parser
		// or the Lucene programmatic API. The Hibernate Search DSL is
		// recommended though
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().onFields("title", "subTitle", "authors.name").matching("Programmers").createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(9, result.size());
	}

	/**
	 * Keyword search with sorting.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSorting() {
		log.info("---------------------------------------------------------------------");
		log.info("testSorting");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().onFields("title", "subTitle", "authors.name").matching("Programmers").createQuery();

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

	/**
	 * Search entities related to a given entity.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testMoreLikeThis() {
		log.info("---------------------------------------------------------------------");
		log.info("testMoreLikeThis");

		Book book = em.find(Book.class, 14);
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.moreLikeThis().comparingFields("title").toEntity(book).createQuery();
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(5, result.size());
	}

	/**
	 * Search entities related to a given entity. Returns a projected result.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testMoreLikeThisProjection() {
		log.info("---------------------------------------------------------------------");
		log.info("testMoreLikeThisProjection");

		Book book = em.find(Book.class, 14);
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.moreLikeThis().comparingFields("title").toEntity(book).createQuery();
		List<Object> result = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class).setProjection(ProjectionConstants.THIS, ProjectionConstants.SCORE).getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(5, result.size());
	}

	/**
	 * Simple search on entity field (subField). Example book.bookReview.comments.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testBookReview() {
		log.info("---------------------------------------------------------------------");
		log.info("testBookReview");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().onField("bookReviews.comments").matching("interesting").createQuery();
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(2, result.size());
	}

	/**
	 * Test using static fuzzy weighted boost at field title. Fuzzy means relevant,
	 * not entirely a perfect match.
	 * 
	 * Book id=27 has Programming is fuzzily matched to Programmers.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testWeighted() {
		log.info("---------------------------------------------------------------------");
		log.info("testWeighted");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.phrase().onField("title").boostedTo(2).andField("subTitle").andField("authors.name").sentence("\"Programmers\"")
				.createQuery();

		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(9, result.size());

		// --

		log.info("fuzzy weighted");

		luceneQuery = qb.keyword().fuzzy().withEditDistanceUpTo(1).withPrefixLength(1).onFields("title").boostedTo(2).andField("subTitle").andField("authors.name")
				.matching("\"Programmers\"").createQuery();

		jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.toString()));

		assertEquals(10, result.size());
	}

	/**
	 * Simple search with paging.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testHibernateSearchPaging() {
		log.info("---------------------------------------------------------------------");
		log.info("testHibernateSearchPaging");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().onFields("title", "subTitle", "authors.name").matching("Programmers").createQuery();

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

	/**
	 * Simple search with wild card. It matches the word, not the phrase.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testWildCard() {
		log.info("---------------------------------------------------------------------");
		log.info("testWildCard");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().wildcard().onFields("title", "subTitle").matching("*theo*").createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle() + " " + p.getSubTitle()));

		assertEquals(2, result.size());
	}

	/**
	 * Search the phrase. Can include other words using slop.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testPhrase() {
		log.info("---------------------------------------------------------------------");
		log.info("testPhrase");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.phrase().onField("subTitle").sentence("best practices").createQuery();

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
		result.forEach(p -> log.info(p.getSubTitle()));

		assertEquals(1, result.size());
	}

	/**
	 * Search using integer range. Commonly use on rating. Limit can be excluded.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testStarred() {
		log.info("---------------------------------------------------------------------");
		log.info("testStarred");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.range().onField("bookReviews.stars").from(4).to(5).excludeLimit().createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		// result.forEach(p -> p.getBookReviews().forEach(q -> log.info(p.getId() + " :
		// " + q.getStars())));
		result.forEach(p -> log.info(p.getId() + " " + p.getTitle() + " " + p.getShortTitle() + " " + p.getSubTitle()));

		assertEquals(3, result.size());
	}

	/**
	 * Search entities with field greater than a given date.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDateRangeAbove() {
		log.info("---------------------------------------------------------------------");
		log.info("testDateRangeAbove");

		Calendar c = Calendar.getInstance();
		c.set(2000, 01, 01);

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.range().onField("publicationDate").above(c.getTime()).createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info("" + p.getPublicationDate()));

		assertEquals(50, result.size());
	}

	/**
	 * Search entities between dates.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDateRangeBetween() {
		log.info("---------------------------------------------------------------------");
		log.info("testDateRangeBetween");

		Calendar c = Calendar.getInstance();
		c.set(2005, 01, 01);

		Date from = c.getTime();

		c.set(2010, 01, 01);
		Date to = c.getTime();

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.range().onField("publicationDate").from(from).to(to).createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info("" + p.getPublicationDate()));

		assertEquals(28, result.size());
	}

	/**
	 * Must=And, Should=Or. Add not() at the end to negate.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testAndOr() {
		log.info("---------------------------------------------------------------------");
		log.info("testAndOr");

		Calendar c = Calendar.getInstance();
		c.set(2005, 01, 01);

		Date from = c.getTime();

		c.set(2010, 01, 01);
		Date to = c.getTime();

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();

		org.apache.lucene.search.Query luceneQuery = qb.bool().must(qb.keyword().onField("subTitle").matching("java").createQuery())
				.must(qb.keyword().onFields("title", "subTitle").matching("javascript").createQuery()).not()
				.must(qb.range().onField("publicationDate").from(from).to(to).createQuery()).should(qb.range().onField("bookReviews.stars").above(5).createQuery()).createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle() + " | " + p.getSubTitle() + " | " + p.getPublicationDate()));

		assertEquals(2, result.size());
	}

	/**
	 * Search a keyword ignoring the analyzer.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testIgNoreAnalyzer() {
		log.info("---------------------------------------------------------------------");
		log.info("testIgNoreAnalyzer");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.keyword().onFields("subTitle", "authors.name").andField("title").ignoreAnalyzer().matching("Programmers").createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);
		// raise an exception
		jpaQuery.setHint("javax.persistence.query.timeout", 400);

		// execute search
		List<Book> result = (List<Book>) jpaQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle() + " | " + p.getSubTitle()));

		assertEquals(4, result.size());
	}

	/**
	 * Returns an array of field (projection) from a given query. Useful if you
	 * don't want to return the whole object.
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testProjection() {
		log.info("---------------------------------------------------------------------");
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
		String subTitle = (String) firstResult[0];
		log.info("subTitle=" + subTitle);

		assertEquals(6, results.size());
	}

	/**
	 * Search using a pre-defined filter. Doesn't work on child entity field?
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testFilter() {
		log.info("---------------------------------------------------------------------");
		log.info("testFilter");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.all().createQuery();

		// wrap Lucene query in a javax.persistence.Query
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);
		fullTextQuery.enableFullTextFilter("bookIdFilter");

		// execute search
		List<Book> result = (List<Book>) fullTextQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle() + " | " + p.getSubTitle()));

		assertEquals(1, result.size());
	}

	/**
	 * Implements a fulltext filter using a factory. Don't forget to add a name to
	 * our name field, otherwise the filter will not work.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testFilterFactory() {
		log.info("---------------------------------------------------------------------");
		log.info("testFilterFactory");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.all().createQuery();

		// wrap Lucene query in a javax.persistence.Query
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);
		fullTextQuery.enableFullTextFilter("Book.NameFilter").setParameter("bookName", "Theory");

		// execute search
		List<Book> result = (List<Book>) fullTextQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle() + " | " + p.getSubTitle()));

		assertEquals(2, result.size());
	}

	/*
	 * Seems like filter doesn't work on collectionTable?
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testBookReviewFactory() {
		log.info("---------------------------------------------------------------------");
		log.info("testBookReviewFactory");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.all().createQuery();

		// wrap Lucene query in a javax.persistence.Query
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);
		fullTextQuery.enableFullTextFilter("Book.ReviewFactory").setParameter("stars", 5);

		// execute search
		List<Book> result = (List<Book>) fullTextQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle() + " | " + p.getSubTitle()));

//		assertEquals(2, result.size());
	}

	/**
	 * Filter using inner fields (author.name).
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testAuthorNameFilter() {
		log.info("---------------------------------------------------------------------");
		log.info("testAuthorNameFilter");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();
		org.apache.lucene.search.Query luceneQuery = qb.all().createQuery();

		// wrap Lucene query in a javax.persistence.Query
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);
		fullTextQuery.enableFullTextFilter("Author.NameFactory").setParameter("authorName", "Erich Gamma");

		// execute search
		List<Book> result = (List<Book>) fullTextQuery.getResultList();

		log.info("Record found=" + result.size());
		result.forEach(p -> log.info(p.getTitle() + " | " + p.getSubTitle()));

		assertEquals(1, result.size());
	}

	/**
	 * Groups together a given category with count.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDiscreetFacet() {
		log.info("---------------------------------------------------------------------");
		log.info("testDiscreetFacet");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();

		org.apache.lucene.search.Query luceneQuery = qb.all().createQuery();
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		FacetingRequest authorFacet = qb.facet().name("authorFacetRequest").onField("authors.name_facet").discrete().orderedBy(FacetSortOrder.FIELD_VALUE).includeZeroCounts(false)
				.createFacetingRequest();

		// retrieve facet manager and apply faceting request
		FacetManager facetManager = fullTextQuery.getFacetManager();
		facetManager.enableFaceting(authorFacet);

		// retrieve the faceting results
		List<Facet> facets = facetManager.getFacets("authorFacetRequest");
		facets.forEach(p -> log.info(p.getValue() + " - " + p.getCount()));

		Facet x = facets.stream().filter(p -> p.getValue().equals("Stephen King")).findFirst().get();
		assertEquals(3, x.getCount());

		// filter query by the first facet
		FacetSelection facetSelection = facetManager.getFacetGroup("authorFacetRequest");
		facetSelection.selectFacets(facets.get(0));

		List<Book> tweets = fullTextQuery.getResultList();
		for (Book t : tweets) {
			log.info(t.toString());
		}
	}

	/**
	 * Groups together a given category. For example we can group by author id. Then
	 * we can load the author entity.
	 */
	@Test
	public void testDiscreetFacetWithEntityData() {
		log.info("---------------------------------------------------------------------");
		log.info("testDiscreetFacetWithEntityData");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();

		org.apache.lucene.search.Query luceneQuery = qb.all().createQuery();
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		// define the facet
		FacetingRequest authorFacet = qb.facet().name("authorIdFacet").onField("authors.id_facet").discrete().orderedBy(FacetSortOrder.COUNT_DESC).includeZeroCounts(false)
				.maxFacetCount(5).createFacetingRequest();

		// retrieve facet manager and apply faceting request
		FacetManager facetManager = fullTextQuery.getFacetManager();
		facetManager.enableFaceting(authorFacet);

		// retrieve the faceting results
		List<Facet> facets = facetManager.getFacets("authorIdFacet");

		// collect all the ids
		List<Integer> vcIds = facets.stream().map(p -> Integer.parseInt(p.getValue())).collect(Collectors.toList());
		// query all the Authors given the id we faceted above, I think multiLoad has
		// been introduced in HS 5.x
		List<Author> authors = fullTextEntityManager.unwrap(Session.class).byMultipleIds(Author.class).multiLoad(vcIds);

		// fill our container object with the facet and author entity
		List<EntityFacet<Author>> entityFacets = new ArrayList<>(facets.size());
		for (int i = 0; i < facets.size(); i++) {
			entityFacets.add(new EntityFacet<Author>(facets.get(i), authors.get(i)));
		}

		entityFacets.stream().forEach(System.out::println);

		assertEquals(5, facets.size());
		assertEquals(1, entityFacets.get(0).getCount());
	}

	/**
	 * Group price by amount. See how price is divided.
	 */
	@Test
	public void testRangeFacet() {
		log.info("---------------------------------------------------------------------");
		log.info("testRangeFacet");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(Book.class).get();

		org.apache.lucene.search.Query luceneQuery = qb.all().createQuery();
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery, Book.class);

		FacetingRequest priceFacet = qb.facet().name("priceFacetRequest").onField("price").range().below(500).from(500).to(1000).above(1000).orderedBy(FacetSortOrder.COUNT_DESC)
				.includeZeroCounts(true).createFacetingRequest();

		// retrieve facet manager and apply faceting request
		FacetManager facetManager = fullTextQuery.getFacetManager();
		facetManager.enableFaceting(priceFacet);

		// retrieve the faceting results
		List<Facet> facets = facetManager.getFacets("priceFacetRequest");
		facets.forEach(p -> log.info(p.getValue() + " - " + p.getCount()));

		assertEquals(3, facets.size());
	}

}