package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.KeyFactory;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.util.ListUsersData;
import pt.unl.fct.di.apdc.firstwebapp.util.UserDto;

@Path("/list")
public class ListResource {

	private static final String MESSAGE_INVALID_USER = "Invalid user. Please check the username inputed.";
	private static final String MESSAGE_INVALID_TOKEN = "No valid token.";
	private static final String MESSAGE_NO_PERMISSION = "You do not have permission to list users.";

	private static final String LOG_MESSAGE_LIST_USERS_ATTEMPT = "List users attempt by: ";
	private static final String LOG_MESSAGE_NONEXISTING_USER = "Either the user, or the target dont exist in the data base.";
	private static final String LOG_MESSAGE_LIST_USERS_SUCCESSFUL = "List users attempt successful by: ";
	private static final String LOG_MESSAGE_NONEXISTING_VALID_TOKEN = "No valid token found for: ";

	private static final Logger LOG = Logger.getLogger(ListResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

	private final Gson g = new Gson();

	public ListResource() {

	}

	private String getValueOrDefault(Entity entity, String property, String defaultVal) {
		if (entity.contains(property)) {
			String val = entity.getString(property);
			if (val == null || val.trim().isEmpty()) {
				return defaultVal;
			}
			return val;
		}
		return defaultVal;
	}

	@POST
	@Path("/users")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response listUsers(ListUsersData data) {
		LOG.fine(LOG_MESSAGE_LIST_USERS_ATTEMPT + data.username);
		Key userKey = userKeyFactory.newKey(data.username);
		Entity user = datastore.get(userKey);
		if (user == null) {
			LOG.warning(LOG_MESSAGE_NONEXISTING_USER + data.username);
			return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_USER).build();
		}

		Query<Entity> tokenQuery = Query.newEntityQueryBuilder().setKind("AuthToken")
				.setFilter(StructuredQuery.PropertyFilter.eq("token_user", data.username)).build();
		QueryResults<Entity> tokens = datastore.run(tokenQuery);

		Entity tokenEntity = null;
		while (tokens.hasNext()) {
			Entity t = tokens.next();
			Timestamp validTo = t.getTimestamp("token_expiration");

			if (validTo != null && validTo.toDate().after(new Date())) {
				tokenEntity = t;
				break;
			}
		}

		if (tokenEntity == null) {
			LOG.warning(LOG_MESSAGE_NONEXISTING_VALID_TOKEN + data.username);
			return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_TOKEN).build();
		}

		String requesterRole = user.getString("user_role");
		Query<Entity> query;
		switch (requesterRole.toUpperCase()) {
		case "ENDUSER":
			query = Query.newEntityQueryBuilder().setKind("User")
					.setFilter(StructuredQuery.CompositeFilter.and(
							StructuredQuery.PropertyFilter.eq("user_role", "ENDUSER"),
							StructuredQuery.PropertyFilter.eq("user_privacy", "público"),
							StructuredQuery.PropertyFilter.eq("user_account_state", "ATIVADA")))
					.build();
			break;
		case "BACKOFFICE":
			query = Query.newEntityQueryBuilder().setKind("User")
					.setFilter(StructuredQuery.PropertyFilter.eq("user_role", "ENDUSER")).build();
			break;
		case "ADMIN":
			query = Query.newEntityQueryBuilder().setKind("User").build();
			break;
		default:
			return Response.status(Status.FORBIDDEN).entity(MESSAGE_NO_PERMISSION).build();
		}

		QueryResults<Entity> results = datastore.run(query);
		List<UserDto> usersList = new ArrayList<>();

		while (results.hasNext()) {
			Entity userEntity = results.next();
			UserDto userDto = new UserDto();
			userDto.username = userEntity.getKey().getName();
			if ("ENDUSER".equalsIgnoreCase(requesterRole)) {
				userDto.user_email = getValueOrDefault(userEntity, "user_email", "NOT DEFINED");
				userDto.user_name = getValueOrDefault(userEntity, "user_name", "NOT DEFINED");
			} else {
				userDto.user_email = getValueOrDefault(userEntity, "user_email", "NOT DEFINED");
				userDto.user_name = getValueOrDefault(userEntity, "user_name", "NOT DEFINED");
				userDto.user_phone = getValueOrDefault(userEntity, "user_phone", "NOT DEFINED");
				userDto.user_privacy = getValueOrDefault(userEntity, "user_privacy", "NOT DEFINED");
				userDto.user_account_state = getValueOrDefault(userEntity, "user_account_state", "NOT DEFINED");
				userDto.user_address = getValueOrDefault(userEntity, "user_address", "NOT DEFINED");
				userDto.user_citizen_card = getValueOrDefault(userEntity, "user_citizen_card", "NOT DEFINED");
				userDto.user_employer = getValueOrDefault(userEntity, "user_employer", "NOT DEFINED");
				userDto.user_employer_nif = getValueOrDefault(userEntity, "user_employer_nif", "NOT DEFINED");
				userDto.user_function = getValueOrDefault(userEntity, "user_function", "NOT DEFINED");
				userDto.user_nif = getValueOrDefault(userEntity, "user_nif", "NOT DEFINED");
			}
			usersList.add(userDto);
		}

		LOG.info(LOG_MESSAGE_LIST_USERS_SUCCESSFUL + data.username);
		String jsonResponse = g.toJson(usersList);
		return Response.ok(jsonResponse).build();
	}

}
