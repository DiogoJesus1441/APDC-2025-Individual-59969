package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

@Path("/register")
public class RegisterResource {

	private static final String MESSAGE_INVALID_REGISTRATION = "Missing or invalid required fields.";
	private static final String MESSAGE_USER_ALREADY_REGISTRED = "User already exists.";

	private static final String LOG_MESSAGE_REGISTER_ATTEMPT = "Register attempt by user: ";
	private static final String LOG_MESSAGE_REGISTER_SUCCESSFUL = "Register successful by user: ";

	private static final String USER_EMAIL = "user_email";
	private static final String USER_PWD = "user_pwd";
	private static final String USER_NAME = "user_name";
	private static final String USER_PHONE = "user_phone";
	private static final String USER_PRIVACY = "user_privacy";
	private static final String USER_ROLE = "user_role";
	private static final String USER_ACCOUNT_STATE = "user_account_state";
	
	private static final String REGISTER_ROLE = "enduser";
	private static final String REGISTER_STATE = "DESATIVADA";

	private static final String USER_CITIZEN_CARD = "user_citizen_card";
	private static final String USER_NIF = "user_nif";
	private static final String USER_EMPLOYER = "user_employer";
	private static final String USER_FUNCTION = "user_function";
	private static final String USER_ADDRESS = "user_address";
	private static final String USER_EMPLOYER_NIF = "user_employer_nif";

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();

	public RegisterResource() {
	} 

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerUser(RegisterData data) {
		LOG.fine(LOG_MESSAGE_REGISTER_ATTEMPT + data.username);

		if (!data.validRegistration()) {
			return Response.status(Status.BAD_REQUEST).entity(MESSAGE_INVALID_REGISTRATION).build();
		}

		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = txn.get(userKey);

			if (user != null) {
				txn.rollback();
				return Response.status(Status.CONFLICT).entity(MESSAGE_USER_ALREADY_REGISTRED).build();
			}

			Entity.Builder userBuilder = Entity.newBuilder(userKey).set(USER_NAME, data.name)
					.set(USER_EMAIL, data.email).set(USER_PHONE, data.telephone)
					.set(USER_PWD, DigestUtils.sha512Hex(data.password)).set(USER_PRIVACY, data.privacy)
					.set(USER_ROLE, REGISTER_ROLE).set(USER_ACCOUNT_STATE, REGISTER_STATE);

			if (data.citizenCard != null)
				userBuilder.set(USER_CITIZEN_CARD, data.citizenCard);
			if (data.nif != null)
				userBuilder.set(USER_NIF, data.nif);
			if (data.employer != null)
				userBuilder.set(USER_EMPLOYER, data.employer);
			if (data.function != null)
				userBuilder.set(USER_FUNCTION, data.function);
			if (data.address != null)
				userBuilder.set(USER_ADDRESS, data.address);
			if (data.employerNIF != null)
				userBuilder.set(USER_EMPLOYER_NIF, data.employerNIF);

			user = userBuilder.build();
			txn.put(user);
			txn.commit();
			LOG.info(LOG_MESSAGE_REGISTER_SUCCESSFUL + data.username);
			return Response.ok(g.toJson(true)).build();
		} catch (DatastoreException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
}
