If you already have data in your database execute these lines of code to initialized.

EntityManager em = entityManagerFactory.createEntityManager();
FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
fullTextEntityManager.createIndexer().startAndWait();

To specify a different version of hibernate-search bundled in wildfly.
1.) Add this line in persistence.xml:
wildfly.jpa.hibernate.search.module=org.hibernate.search.orm:5.7.0.Final

2.) Download your desired hibernate-search wildfly distribution and extract in modules folder.