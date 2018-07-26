#Hibernate-search with infinispan integration demo project.
--

This tutorial is for developers who are just beginning to learn hibernate-search and are trying to setup a demo project. Most of the codes are copied from hibernate-search documentation: https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/

##To run this you must download the following wildfly modules:
	- infinispan-wildfly-modules-9.3.1.Final (WF13)

It also requires PostgreSQL database.
 - jndi-name: java:jboss/datasources/hibernate-search-demoDS
 - user: demo
 - password: demo

--

If you already have data in your database, execute the lines of code below to initialized.
```
EntityManager em = entityManagerFactory.createEntityManager();
FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
fullTextEntityManager.createIndexer().startAndWait();
```

You can set the object retrieval in configuration:

	- hibernate.search.query.object_lookup_method = second_level_cache
	- hibernate.search.query.database_retrieval_method = query
	- wildfly.jpa.hibernate.search.module=org.hibernate.search.orm:5.7.1.Final
	
To run
```
mvn clean test -Parq-wildfly-managed
```