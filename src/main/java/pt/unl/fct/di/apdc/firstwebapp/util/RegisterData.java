package pt.unl.fct.di.apdc.firstwebapp.util;

public class RegisterData {

	// Mandatory fields
	public String email;
	public String username;
	public String name;
	public String telephone;
	public String password;
	public String confirmation;
	public String privacy;
	public String role;
	public String accountState;

	// Optional fields
    public String citizenCard;
    public String nif;
    public String employer;
    public String function;
    public String address;
    public String employerNIF;
		
	public RegisterData() {

	}

	public RegisterData(String email, String username, String name, String telephone, String password,
			String confirmation, String privacy) {
		this.email = email;
		this.username = username;
		this.name = name;
		this.telephone = telephone;
		this.password = password;
		this.confirmation = confirmation;
		this.privacy = privacy;
	}

	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}
	
	private boolean isValidPrivacy() {
        return privacy.equalsIgnoreCase("público") || privacy.equalsIgnoreCase("privado");
    }

	public boolean validRegistration() {
		return nonEmptyOrBlankField(username) && nonEmptyOrBlankField(password) && nonEmptyOrBlankField(email)
				&& nonEmptyOrBlankField(name) && email.contains("@") && password.equals(confirmation)
				&& isValidPrivacy();
	}
}
