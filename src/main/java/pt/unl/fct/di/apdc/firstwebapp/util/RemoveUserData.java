package pt.unl.fct.di.apdc.firstwebapp.util;

public class RemoveUserData {
	
	public String username;
	public String targetUsername;
	
	public RemoveUserData() {
		
	}
	
	public RemoveUserData(String username, String targetUsername) {
		this.username = username;
		this.targetUsername = targetUsername;
	}
}
