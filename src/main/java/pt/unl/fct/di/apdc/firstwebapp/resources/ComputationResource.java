package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.Date;
import java.util.logging.Logger;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import pt.unl.fct.di.apdc.firstwebapp.util.ChangeRoleData;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeStateData;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.Timestamp;

import com.google.gson.Gson;

@Path("/utils")
public class ComputationResource {

	private static final String MESSAGE_INVALID_USER = "Invalid user or target. Please check the usernames inputed.";
	private static final String MESSAGE_INVALID_TOKEN = "No valid token.";
	private static final String MESSAGE_INVALID_PERMISSION = "Permission denied.";

	private static final String LOG_MESSAGE_CHANGE_ROLE_ATTEMPT = "Change role attempt by user: ";
	private static final String LOG_MESSAGE_NONEXISTING_USER = "Either the user, or the target dont exist in the data base.";
	private static final String LOG_MESSAGE_NONEXISTING_VALID_TOKEN = "No valid token found for: ";
	private static final String LOG_MESSAGE_USER_WITHOUT_PERMISSION = "Unauthorized change attempt by: ";
	private static final String LOG_MESSAGE_CHANGE_ROLE_SUCCESSFUL = "The role change was successful by: ";
	private static final String LOG_MESSAGE_CHANGE_STATE_SUCCESSFUL = "The state change was successful by: ";

	private static final Logger LOG = Logger.getLogger(ComputationResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

	private final Gson g = new Gson();

	public ComputationResource() {
	}

	@POST
	@Path("/changerole")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response ChangeUserRole(ChangeRoleData data) {
		LOG.fine(LOG_MESSAGE_CHANGE_ROLE_ATTEMPT + data.username);

		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = userKeyFactory.newKey(data.username);
			Key userTargetKey = userKeyFactory.newKey(data.targetUsername);
			Entity user = txn.get(userKey);
			Entity targetUser = txn.get(userTargetKey);
			if (user == null || targetUser == null) {
				LOG.warning(LOG_MESSAGE_NONEXISTING_USER + data.username);
				return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_USER).build();
			}

			Query<Entity> query = Query.newEntityQueryBuilder().setKind("AuthToken")
					.setFilter(StructuredQuery.PropertyFilter.eq("token_user", data.username)).build();
			QueryResults<Entity> tokens = datastore.run(query);

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
			String targetRole = targetUser.getString("user_role");
			String newRole = data.role;

			boolean allowed = false;
			switch (requesterRole) {
			case "ADMIN":
				allowed = true;
				break;
			case "BACKOFFICE":
				if ((targetRole.equals("ENDUSER") && newRole.equals("PARTNER"))
						|| (targetRole.equals("PARTNER") && newRole.equals("ENDUSER"))) {
					allowed = true;
				}
				break;
			default:
				allowed = false;
			}

			if (!allowed) {
				LOG.warning(LOG_MESSAGE_USER_WITHOUT_PERMISSION + data.username);
				return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_PERMISSION).build();
			}

			Entity updatedTarget = Entity.newBuilder(targetUser).set("user_role", newRole).build();
			txn.put(updatedTarget);
			txn.commit();

			LOG.info(LOG_MESSAGE_CHANGE_ROLE_SUCCESSFUL + data.username);
			return Response.ok(g.toJson(true)).build();
		} catch (DatastoreException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
	
	@POST
	@Path("/changestate")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response changeUserState(ChangeStateData data) {
		LOG.fine(LOG_MESSAGE_CHANGE_ROLE_ATTEMPT + data.username);

		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = userKeyFactory.newKey(data.username);
			Key userTargetKey = userKeyFactory.newKey(data.targetUsername);
			Entity user = txn.get(userKey);
			Entity targetUser = txn.get(userTargetKey);
			if (user == null || targetUser == null) {
				LOG.warning(LOG_MESSAGE_NONEXISTING_USER + data.username);
				return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_USER).build();
			}

			Query<Entity> query = Query.newEntityQueryBuilder().setKind("AuthToken")
					.setFilter(StructuredQuery.PropertyFilter.eq("token_user", data.username)).build();
			QueryResults<Entity> tokens = datastore.run(query);

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
			String targetState = targetUser.getString("user_account_state");
			String newState = data.state;

			boolean allowed = false;
			switch (requesterRole) {
			case "ADMIN":
				allowed = true;
				break;
			case "BACKOFFICE":
				if ((targetState.equals("DESATIVADA") && newState.equals("ATIVADA"))
						|| (targetState.equals("ATIVADA") && newState.equals("DESATIVADA"))) {
					allowed = true;
				}
				break;
			default:
				allowed = false;
			}

			if (!allowed) {
				LOG.warning(LOG_MESSAGE_USER_WITHOUT_PERMISSION + data.username);
				return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_PERMISSION).build();
			}
			
			Entity updatedTarget = Entity.newBuilder(targetUser).set("user_account_state", newState).build();
			txn.put(updatedTarget);
			txn.commit();
			
			LOG.info(LOG_MESSAGE_CHANGE_STATE_SUCCESSFUL + data.username);
			return Response.ok(g.toJson(true)).build();
		}catch (DatastoreException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}

}