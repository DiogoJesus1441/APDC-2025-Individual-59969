package pt.unl.fct.di.apdc.firstwebapp.util;

public class ChangeStateData {
	public String username;
	public String targetUsername;
	public String state;

	public ChangeStateData() {

	}

	public ChangeStateData(String username, String targetUsername, String state) {
		this.username = username;
		this.targetUsername = targetUsername;
		this.state = state;
	}
}
