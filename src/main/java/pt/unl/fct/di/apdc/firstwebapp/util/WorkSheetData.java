package pt.unl.fct.di.apdc.firstwebapp.util;

public class WorkSheetData {

	public String username;
	public String reference;
	public String description;
	public String targetType;
	public String adjudicationState;

	public String adjudicationDate;
	public String startDate;
	public String endDate;
	public String partnerAccount;
	public String adjudicationEntity;
	public String companyNIF;
	public String workState;
	public String observations;

	public WorkSheetData() {
	}

	public WorkSheetData(String username, String reference, String description, String targetType,
			String adjudicationState, String adjudicationDate, String startDate, String endDate, String partnerAccount,
			String adjudicationEntity, String companyNIF, String workState, String observations) {
		this.username = username;
		this.reference = reference;
		this.description = description;
		this.targetType = targetType;
		this.adjudicationState = adjudicationState;
		if (this.adjudicationState.equals("ADJUDICADO")) {
			this.adjudicationDate = adjudicationDate;
			this.startDate = startDate;
			this.endDate = endDate;
			this.partnerAccount = partnerAccount;
			this.adjudicationEntity = adjudicationEntity;
			this.companyNIF = companyNIF;
			this.workState = workState;
			this.observations = observations;
		}
	}

	public boolean isValidFields() {
		return (targetType.equals("Propriedade Pública") || targetType.equals("Propriedade Privada"))
				&& (adjudicationState.equals("ADJUDICADO") || adjudicationState.equals("NÃO ADJUDICADO"));
	}

	public boolean isValidAdjudicationFields() {
		return workState.equals("NÃO INICIADO") || workState.equals("EM CURSO") || workState.equals("CONCLUÍDO");
	}
}
