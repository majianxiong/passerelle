/**
 * 
 */
package com.isencia.passerelle.project.repository.api;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;

import com.isencia.passerelle.model.Flow;

/**
 * Entry point for the Sherpa Repository, when applied for the 3-level structure
 * that is defined for Passerelle&Drools-related assets :
 * <ul>
 * <li>Project
 * <li>Package : set of assets; always contains a set of Drools rules files, and
 * already the serialized resulting Knowledgebase
 * <li>Asset : can be a Passerelle Flow or a Drools rules file
 * </ul>
 * 
 * The Passerelle Repository API hides the underlying 3-level structure, and
 * just exposes Projects, that can contain Passerelle Flows and Drools pre-built
 * KnowledgeBases.
 * 
 * @author delerw
 * 
 */
public interface RepositoryService {

	String REVISION_ID = "revisionID";
	String LOCALE = "locale";
	String REFERENCE = "REFERENCE";
	String FLOW_NAME = "flowname";
	String USER_ID = "userID";
	String JOB_ID = "jobID";
	String SYSTEM_PARAMETERS = "systemParameters";
	String APPLICATION_PARAMETERS = "applicationParameters";

	/**
	 * 
	 * @param submodelCode
	 * @return the flow for the given sequenceCode, or null if not found
	 */
	Flow getSubmodel(String sequenceCode);
	
	/**
	 * 
	 * @param id
	 * @return the flow for the given sequenceCode, or null if not found
	 */
	String getFlowCode(Long id);

	/**
	 * 
	 * @param sequenceCode
	 * @return the flow for the given sequenceCode, or null if not found
	 */
	Flow getFlow(String sequenceCode);

	/**
	 * 
	 * @param projectCode
	 * @return the project for the given projectCode, or null if not found
	 */
	Project getProject(String projectCode);
	/**
	 * 
	 * @param id
	 * @return the project for the given projectCode, or null if not found
	 */
	Project getProject(Long projectId);

	/**
	 * 
	 * @return an array of the codes for all projects in the Repository
	 */
	String[] getAllProjectCodes();

	/**
	 * 
	 * @param packageCode
	 * @return the project for the given packageCode, or null if not found
	 */
	KnowledgeBase getKnowledgeBase(String packageCode) throws Exception;

	/**
	 * 
	 * @return an array of the codes for all packages in the Repository
	 */
	String[] getAllPackageCodes();

	/**
	 * 
	 * @param projectCode
	 * @return exists a new version of the project in the repository
	 */
	boolean existNewProject(String projectCode);

	/**
	 * 
	 * @param packageCode
	 * @return exists a new version of the package in the repository
	 */
	boolean existNewPackage(String packageCode);

	String getDefaultDsl(String name);

	String getDefaultDslr(String name);
	 
  KnowledgeBaseConfiguration getKnowledgeBaseConfiguration();
}