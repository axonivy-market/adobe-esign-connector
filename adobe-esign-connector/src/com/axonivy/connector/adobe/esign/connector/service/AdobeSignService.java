package com.axonivy.connector.adobe.esign.connector.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import com.axonivy.connector.adobe.esign.connector.rest.DownloadResult;
import com.axonivy.connector.adobe.esign.connector.rest.UploadWrapper;

import api.rest.v6.client.AgreementCreationInfo;
import api.rest.v6.client.AgreementCreationInfo.SignatureTypeEnum;
import api.rest.v6.client.AgreementCreationInfo.StateEnum;
import api.rest.v6.client.AgreementCreationInfoMemberInfos;
import api.rest.v6.client.AgreementCreationInfoParticipantSetsInfo;
import api.rest.v6.client.AgreementCreationInfoParticipantSetsInfo.RoleEnum;
import api.rest.v6.client.AgreementCreationResponse;
import api.rest.v6.client.AgreementDocuments;
import api.rest.v6.client.AgreementInfo;
import api.rest.v6.client.AgreementsAnchorTextInfo;
import api.rest.v6.client.AgreementsAnchorTextInfoAnchoredFormFieldLocation;
import api.rest.v6.client.AgreementsEmailOption;
import api.rest.v6.client.AgreementsEmailOptionSendOptions;
import api.rest.v6.client.AgreementsEmailOptionSendOptions.CompletionEmailsEnum;
import api.rest.v6.client.AgreementsEmailOptionSendOptions.InFlightEmailsEnum;
import api.rest.v6.client.AgreementsEmailOptionSendOptions.InitEmailsEnum;
import api.rest.v6.client.AgreementsFileInfos;
import api.rest.v6.client.AgreementsFormFieldDescription;
import api.rest.v6.client.AgreementsFormFieldDescription.ContentTypeEnum;
import api.rest.v6.client.AgreementsFormFieldGenerators;
import api.rest.v6.client.AgreementsFormFieldGenerators.GeneratorTypeEnum;
import api.rest.v6.client.AgreementsPostSignOption;
import api.rest.v6.client.FormFieldPutInfo;
import api.rest.v6.client.SigningUrlResponseSigningUrlSetInfos;
import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.call.SubProcessCall;
import ch.ivyteam.ivy.process.call.SubProcessCallResult;
import ch.ivyteam.ivy.request.IHttpRequest;

/**
 * Service class to call Adobe Sign services and create necessary objects
 * 
 * @author jpl
 *
 */
public class AdobeSignService {

	private static final AdobeSignService instance = new AdobeSignService();
	
	// process paths, signature and parameter names
	private static final String SERVICE_BASE = "connector/";
	private static final String TRANSIENT_DOCUMENTS_SERVICE = SERVICE_BASE + "TransientDocuments";
	private static final String AGREEMENTS_SERVICE = SERVICE_BASE + "Agreements";
	
	private static final String CREATE_AGREEMENT_SIGNATURE = "createAgreement(AgreementCreationInfo)";
	private static final String GET_AGREEMENT_SIGNATURE = "getAgreementById(String)";
	private static final String GET_DOCUMENTS_SIGNATURE = "getDocuments(String)";
	private static final String UPLOAD_SIGNATURE = "uploadDocument(UploadWrapper)";
	private static final String DOWNLOAD_SIGNATURE = "dowloadDocument(String, String, String, Boolean)";
	private static final String SIGNING_URL_SIGNATURE = "getSigningURLs(String,String)";
	private static final String FORMFIELD_SIGNATURE = "addFormFields(String, FormFieldPutInfo)";
	
	private static final String ID_PARAM = "id";
	private static final String ERROR_PARAM = "error";
	private static final String UPLOAD_PARAM = "upload";
	private static final String AGREEMENT_PARAM = "agreement";
	private static final String AGREEMENT_ID_PARAM = "agreementId";

	
	private AdobeSignService() {} // locked constructor
	
	public static AdobeSignService getInstance() {
		return instance;
	}
	
	/**
	 * Convenience method to call upload document and create agreement methods
	 * 
	 * @param name
	 * @param workflowId
	 * @param signerEmail
	 * @param upload
	 * @return
	 */
	public String uploadDocumentAndCreateSimpleAgreement(String name, String signerEmail, UploadWrapper upload) {
		String documentId = uploadDocument(upload);
		return createAgreement(buildSimpleAgreement(name, documentId, signerEmail));
	}
	
	/**
	 * Convenience method to call upload document and create agreement methods
	 * 
	 * @param name
	 * @param workflowId
	 * @param signerEmail
	 * @param upload
	 * @return
	 */
	public String uploadDocumentAndCreateSimpleAgreementWithFormFields(String name, String signerEmail, UploadWrapper upload, List<AgreementsFormFieldGenerators> formFieldGenerators) {
		String documentId = uploadDocument(upload);
		String agreementId = createAgreement(
				buildSimpleAgreementWithFormFields(name, Arrays.asList(documentId), signerEmail, formFieldGenerators));
		
		return agreementId;
	}
	
	/**
	 * Helper method to create agreement object as required for the REST service
	 * 
	 * @param name
	 * @param workflowId
	 * @param documentId
	 * @param signerEmail
	 * @return
	 */
	public AgreementCreationInfo buildSimpleAgreement(String name, String documentId, String signerEmail) {
		return buildSimpleAgreementWithFormFields(name, Arrays.asList(documentId), signerEmail, null);
	}
	
	/**
	 * Helper method to create agreement object as required for the REST service
	 * 
	 * @param name
	 * @param workflowId
	 * @param documentId
	 * @param signerEmail
	 * @return
	 */
	public AgreementCreationInfo buildSimpleAgreementWithFormFields(String name, List<String> documentIds,
			String signerEmail, List<AgreementsFormFieldGenerators> formFieldGenerators) {
		AgreementCreationInfo agreement = new AgreementCreationInfo();
		
		agreement.setName(name);
		agreement.setMessage("Please sign this document!");
		agreement.setSignatureType(SignatureTypeEnum.ESIGN);
		agreement.setState(StateEnum.IN_PROCESS);
		
		// add signers
		agreement.setParticipantSetsInfo(createParticipantInfoForEmail(Arrays.asList(signerEmail), RoleEnum.SIGNER));
		
		// add documentIds - need to be already transferred with the upload document service
		agreement.setFileInfos(createFileInfosForDocumentIds(documentIds));
		
		agreement.setEmailOption(createAllDisabledSendOptions());
		
		String baseUrl = getRequestBaseUrl();
		String fullUrl = baseUrl + Ivy.var().get("adobe-sign-connector.returnPage");
		agreement.postSignOption(new AgreementsPostSignOption().redirectUrl(fullUrl));
		
		if(Objects.nonNull(formFieldGenerators)) {
			agreement.setFormFieldGenerators(formFieldGenerators);
		}
		
		return agreement;
	}
	
	/**
	 * Helper method to create agreement object for two groups of signers as required for the REST service
	 * 
	 * @param name
	 * @param workflowId
	 * @param documentId
	 * @param signer1Email
	 * @return
	 */
	public AgreementCreationInfo buildSimpleAgreementFor2SignerGroups(String name, String documentId, List<String> signers1, List<String> signers2) {
		return buildSimpleAgreementFor2ParticipantGroups(name, documentId, signers1, RoleEnum.SIGNER, signers2, RoleEnum.SIGNER);
	}
	
	/**
	 * Helper method to create agreement for two groups of participants object as required for the REST service
	 * 
	 * @param name
	 * @param workflowId
	 * @param documentId
	 * @param signer1Email
	 * @return
	 */
	public AgreementCreationInfo buildSimpleAgreementFor2ParticipantGroups(String name, String documentId, List<String> participants1,
			RoleEnum participants1Role, List<String> participants2, RoleEnum participants2Role) {
		AgreementCreationInfo agreement = new AgreementCreationInfo();
		
		agreement.setName(name);
		agreement.setMessage("Please sign this document!");
		agreement.setSignatureType(SignatureTypeEnum.ESIGN);
		agreement.setState(StateEnum.IN_PROCESS);
		
		// add signers
		agreement.setParticipantSetsInfo(createParticipantInfoForEmail(participants1, participants1Role));
		agreement.getParticipantSetsInfo().add(createParticipants(participants2, participants2Role, 2));
		
		// add documentIds - need to be already transferred with the upload document service
		agreement.setFileInfos(createFileInfosForDocumentIds(Arrays.asList(documentId)));
		
		agreement.setEmailOption(createAllDisabledSendOptions());
		
		agreement.postSignOption(new AgreementsPostSignOption().redirectUrl(Ivy.var().get("adobe-sign-connector.returnPage")));
		
		return agreement;
	}

	/**
	 * Wrapper method to call sub process to post a new agreement
	 * 
	 * @param agreement
	 * @return
	 * @throws BpmError
	 */
	public String createAgreement(AgreementCreationInfo agreement) throws BpmError {
		SubProcessCallResult callResult = SubProcessCall.withPath(AGREEMENTS_SERVICE).withStartSignature(CREATE_AGREEMENT_SIGNATURE).withParam(AGREEMENT_PARAM, agreement).call();
		
		handleError(callResult);
		AgreementCreationResponse agreementResponse = callResult.get("agreementInfo", AgreementCreationResponse.class);
		
		return agreementResponse.getId();
	}

	/**
	 * Wrapper method to call sub process to get agreement by its id
	 * 
	 * @param agreementId
	 * @return
	 */
	public AgreementInfo getAgreement(String agreementId) {
		SubProcessCallResult callResult = SubProcessCall.withPath(AGREEMENTS_SERVICE).withStartSignature(GET_AGREEMENT_SIGNATURE).withParam(AGREEMENT_ID_PARAM, agreementId).call();
		
		handleError(callResult);
		return callResult.get("agreementInfo", AgreementInfo.class);
	}
	
	/**
	 * Wrapper method to call sub process to get list of documents for an agreement
	 * 
	 * @param agreementId
	 * @return
	 */
	public AgreementDocuments getDocuments(String agreementId) {
		SubProcessCallResult callResult = SubProcessCall.withPath(AGREEMENTS_SERVICE).withStartSignature(GET_DOCUMENTS_SIGNATURE).withParam(AGREEMENT_ID_PARAM, agreementId).call();
		
		handleError(callResult);
		return callResult.get("documents", AgreementDocuments.class);
	}
	
	/**
	 * Wrapper method to call sub process to retrieve the signing URIs of an agreement
	 * 
	 * @param agreementId
	 * @param frameParent
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<SigningUrlResponseSigningUrlSetInfos> getSigningURIs(String agreementId, String frameParent) {
		SubProcessCallResult callResult = SubProcessCall.withPath(AGREEMENTS_SERVICE).withStartSignature(SIGNING_URL_SIGNATURE)
				.withParam(AGREEMENT_ID_PARAM, agreementId)
				.withParam("frameParent", frameParent)
				.call();
		
		handleError(callResult);
		return (List<SigningUrlResponseSigningUrlSetInfos>)callResult.get("signingURIs");
	}
	
	/**
	 * Wrapper method to call sub process to upload a new transient document required to create a new agreement
	 * 
	 * @param upload
	 * @return
	 * @throws BpmError
	 */
	public String uploadDocument(String filename, byte[] bytes) throws BpmError {
		return uploadDocument(new UploadWrapper(filename, bytes));
	}
	
	/**
	 * Wrapper method to call sub process to upload a new transient document required to create a new agreement
	 * 
	 * @param upload
	 * @return
	 * @throws BpmError
	 */
	public String uploadDocument(UploadWrapper upload) throws BpmError {
		String documentId;
		
		SubProcessCallResult callResult = SubProcessCall.withPath(TRANSIENT_DOCUMENTS_SERVICE).withStartSignature(UPLOAD_SIGNATURE).withParam(UPLOAD_PARAM, upload).call();
		
		handleError(callResult);
		documentId = callResult.get(ID_PARAM, String.class);
		
		return documentId;
	}
	
	/**
	 * Wrapper method to call sub process to download a document of an agreement
	 * 
	 * @param agreementId
	 * @param documentId
	 * @param filename
	 * @param asFile
	 * @return
	 */
	public DownloadResult downloadDocument(String agreementId, String documentId, String filename, Boolean asFile) {
		SubProcessCallResult callResult = SubProcessCall.withPath(AGREEMENTS_SERVICE).withStartSignature(DOWNLOAD_SIGNATURE)
				.withParam("agreementId", agreementId)
				.withParam("documentId", documentId)
				.withParam("filename", filename)
				.withParam("asFile", asFile)
				.call();
		
		handleError(callResult);
		return callResult.get("download", DownloadResult.class);
	}
	
	/**
	 * Wrapper method to call sub process to add form fields to an agreement
	 * 
	 * @param agreementId
	 * @param formFieldInfo
	 */
	public void addFormFieldsToAgreement(String agreementId, FormFieldPutInfo formFieldInfo) {
		SubProcessCallResult callResult = SubProcessCall.withPath(AGREEMENTS_SERVICE).withStartSignature(FORMFIELD_SIGNATURE)
				.withParam("agreementId", agreementId)
				.withParam("formFieldInfo", formFieldInfo)
				.call();
		
		handleError(callResult);
	}
	
	/**
	 * creates participant information for a list of email addresses and role as required for the REST services
	 * 
	 * @param signerEmails
	 * @param role
	 * @param order 
	 * @return
	 */
	public List<AgreementCreationInfoParticipantSetsInfo> createParticipantInfoForEmail(List<String> signerEmails, RoleEnum role) {
		return createParticipantInfoForEmail(signerEmails, role, 1);
	}
	
	/**
	 * creates participant information for a list of email addresses and role as required for the REST services
	 * 
	 * @param signerEmails
	 * @param role
	 * @param order 
	 * @return
	 */
	public List<AgreementCreationInfoParticipantSetsInfo> createParticipantInfoForEmail(List<String> signerEmails, RoleEnum role, Integer order) {
		List<AgreementCreationInfoParticipantSetsInfo> participantSetsInfo = new ArrayList<>();
		AgreementCreationInfoParticipantSetsInfo participant = createParticipants(signerEmails, role, order);
		participantSetsInfo.add(participant);
		return participantSetsInfo;
	}

	/**
	 * creates participants for a list of email addresses
	 * 
	 * @param signerEmails
	 * @param role
	 * @param order
	 * @return
	 */
	public AgreementCreationInfoParticipantSetsInfo createParticipants(List<String> signerEmails, RoleEnum role,
			Integer order) {
		AgreementCreationInfoParticipantSetsInfo participant = new AgreementCreationInfoParticipantSetsInfo();
		List<AgreementCreationInfoMemberInfos> memberInfos = new ArrayList<>();
		
		for(String email : signerEmails) {
			AgreementCreationInfoMemberInfos member = new AgreementCreationInfoMemberInfos();
			member.setEmail(email);
			memberInfos.add(member);
		}
		
		participant.setMemberInfos(memberInfos);
		participant.setRole(role);
		participant.setOrder(order);
		return participant;
	}
	
	/**
	 * creates file info for a list of document ids
	 * 
	 * @param documentIds
	 * @return
	 */
	public List<AgreementsFileInfos> createFileInfosForDocumentIds(List<String> documentIds) {
		List<AgreementsFileInfos> fileInfos = new ArrayList<>();
		
		for(String id : documentIds) {
			AgreementsFileInfos fileInfo = new AgreementsFileInfos();
			fileInfo.transientDocumentId(id);
			fileInfos.add(fileInfo );
		}
		return fileInfos;
	}
	
	public AgreementsFormFieldGenerators createFormFieldWithAnchor(String anchorText, ContentTypeEnum contentType, Double offsetX, Double offsetY) {
		return new AgreementsFormFieldGenerators().generatorType(GeneratorTypeEnum.ANCHOR_TEXT)
			.formFieldDescription(new AgreementsFormFieldDescription().contentType(contentType))
			.anchorTextInfo(
					new AgreementsAnchorTextInfo().anchorText(anchorText)
						.anchoredFormFieldLocation(new AgreementsAnchorTextInfoAnchoredFormFieldLocation().offsetX(offsetX).offsetY(offsetY)));
	}

	/**
	 * creates options for sending email
	 * 
	 * @param completion
	 * @param inflight
	 * @param init
	 * @return
	 */
	public AgreementsEmailOption createSendOptions(CompletionEmailsEnum completion, InFlightEmailsEnum inflight, InitEmailsEnum init) {
		AgreementsEmailOption emailOptions = new AgreementsEmailOption();
		AgreementsEmailOptionSendOptions sendOptions = new AgreementsEmailOptionSendOptions();
		sendOptions.setCompletionEmails(completion);
		sendOptions.setInFlightEmails(inflight);
		sendOptions.setInitEmails(init);
		emailOptions.setSendOptions(sendOptions );
		return emailOptions;
	}
	
	/**
	 * create options for sending email with all disabled
	 * 
	 * @return
	 */
	public AgreementsEmailOption createAllDisabledSendOptions() {
		return createSendOptions(CompletionEmailsEnum.NONE, InFlightEmailsEnum.NONE, InitEmailsEnum.NONE);
	}
	
	private void handleError(SubProcessCallResult callResult) {
		BpmError error = callResult.get(ERROR_PARAM, BpmError.class);
		if(Objects.nonNull(error)) {
			throw error;
		}
	}
	
	/**
	 * Extracts the base url from actual request.
	 * Example: http://localhost:8081
	 * @return
	 */
	private String getRequestBaseUrl() {
		IHttpRequest req = (IHttpRequest) Ivy.request();
		HttpServletRequest request = req.getHttpServletRequest();
        StringBuilder baseUrlBuilder = new StringBuilder();
        baseUrlBuilder.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            baseUrlBuilder.append(":").append(port);
        }
        return baseUrlBuilder.toString();
	}
	
}
