package com.broodcamp.hibernatesearch;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.slf4j.Logger;

/**
 * @author Edward P. Legaspi
 **/
@Singleton
@Startup
public class StartupListener {

	@Inject
	private EntityManager em;
	
	@Inject
	private Logger log;
	
	@PostConstruct
	private void init() {
		log.trace("Bootup initialization...");
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		try {
			fullTextEntityManager.createIndexer().startAndWait();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
