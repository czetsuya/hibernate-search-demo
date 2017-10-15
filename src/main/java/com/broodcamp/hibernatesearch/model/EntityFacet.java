package com.broodcamp.hibernatesearch.model;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.facet.Facet;

/**
 * @author Edward P. Legaspi
 * @created 15 Oct 2017
 */
public class EntityFacet<T> implements Facet {
	private final Facet delegate;
	private final T entity;

	public EntityFacet(Facet delegate, T entity) {
		this.delegate = delegate;
		this.entity = entity;
	}

	@Override
	public String getFacetingName() {
		return delegate.getFacetingName();
	}

	@Override
	public String getFieldName() {
		return delegate.getFieldName();
	}

	@Override
	public String getValue() {
		return delegate.getValue();
	}

	@Override
	public int getCount() {
		return delegate.getCount();
	}

	@Override
	public Query getFacetQuery() {
		return delegate.getFacetQuery();
	}

	public T getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return "EntityFacet [delegate=" + delegate + ", entity=" + entity + "]";
	}
}
