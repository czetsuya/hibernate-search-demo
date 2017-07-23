Hibernate-search with infinispan integration demo project.
--

This tutorial is for developers who are just beginning to learn hibernate-search and trying to setup a demo project. Most of the codes are copied from hibernate-search documentation: https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/

To run this you must download the following wildfly modules:
--
1.) hibernate-search-5.7.1.Final-dist
2.) infinispan-wildfly-modules-9.1.0.Final
*Note that I installed in this order.

--

If you already have data in your database execute these lines of code to initialized.

EntityManager em = entityManagerFactory.createEntityManager();
FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
fullTextEntityManager.createIndexer().startAndWait();

To specify a different version of hibernate-search bundled in wildfly.
1.) Add this line in persistence.xml:
wildfly.jpa.hibernate.search.module=org.hibernate.search.orm:5.7.0.Final

2.) Download your desired hibernate-search wildfly distribution and extract in modules folder.

The infinispan version bundled with Wildfly 10.1.0.Final will throw a missing class exception to resolved that we must download a specific infinispan bundle:
infinispan-wildfly-modules-9.1.0.Final.zip

You can set the object retrieval in configuration:
hibernate.search.query.object_lookup_method = second_level_cache
hibernate.search.query.database_retrieval_method = query
wildfly.jpa.hibernate.search.module=org.hibernate.search.orm:5.7.1.Final