package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.Date;
import java.util.logging.Logger;
import java.net.URI;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	private static final String MESSAGE_INVALID_CREDENTIALS = "Incorrect username or password.";

	private static final String LOG_MESSAGE_LOGIN_ATTEMP = "Login attempt by user: ";
	private static final String LOG_MESSAGE_LOGIN_SUCCESSFUL = "Login successful by user: ";
	private static final String LOG_MESSAGE_WRONG_PASSWORD = "Wrong password for: ";
	private static final String LOG_MESSAGE_UNKNOW_USER = "Failed login attempt for username: ";

	private static final String USER_PWD = "user_pwd";
	private static final String USER_ROLE = "user_role";

	private static final String TOKEN_USER = "token_user";
	private static final String TOKEN_ROLE = "token_role";
	private static final String TOKEN_EMISSION = "token_emission";
	private static final String TOKEN_EXPIRATION = "token_expiration";

	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

	public LoginResource() {

	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response doLogin(LoginData data) {
		LOG.fine(LOG_MESSAGE_LOGIN_ATTEMP + data.username);

		Key userKey = userKeyFactory.newKey(data.username);
		Entity user = datastore.get(userKey);
		if (user == null) {
			LOG.warning(LOG_MESSAGE_UNKNOW_USER + data.username);
			return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS).build();
		}

		String hashedStoredPassword = user.getString(USER_PWD);
		if (!hashedStoredPassword.equals(DigestUtils.sha512Hex(data.password))) {
			LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.username);
			return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS).build();
		}

		Query<Entity> tokenQuery = Query.newEntityQueryBuilder().setKind("AuthToken")
				.setFilter(StructuredQuery.PropertyFilter.eq(TOKEN_USER, data.username)).build();
		QueryResults<Entity> tokenResults = datastore.run(tokenQuery);

		AuthToken token;
		Entity tokenEntity;

		if (tokenResults.hasNext()) {
			
			Timestamp newValidFrom = Timestamp.now();
			long newExpirationMillis = newValidFrom.toDate().getTime() + AuthToken.EXPIRATION_TIME;
			Timestamp newValidTo = Timestamp.of(new Date(newExpirationMillis));
			
			tokenEntity = tokenResults.next();
			Key tokenKey = tokenEntity.getKey();
			token = new AuthToken();
			token.username = data.username;
			token.role = user.getString(USER_ROLE);
			token.tokenID = tokenKey.getName();
			token.validFrom = newValidFrom;
			token.validTo = newValidTo;

			tokenEntity = Entity.newBuilder(tokenKey).set(TOKEN_USER, token.username).set(TOKEN_ROLE, token.role)
					.set(TOKEN_EMISSION, token.validFrom).set(TOKEN_EXPIRATION, token.validTo).build();
			datastore.put(tokenEntity);
		} else {
			token = new AuthToken(data.username, user.getString(USER_ROLE));
			Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(token.tokenID);
			tokenEntity = Entity.newBuilder(tokenKey).set(TOKEN_USER, token.username).set(TOKEN_ROLE, token.role)
					.set(TOKEN_EMISSION, token.validFrom).set(TOKEN_EXPIRATION, token.validTo).build();
			datastore.put(tokenEntity);
		}

		LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + data.username);

		URI redirectUri = URI.create("/welcome/welcome.html?user=" + data.username + "&role=" + token.role);
		return Response.temporaryRedirect(redirectUri).build();
	}

}
