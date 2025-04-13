package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import java.util.Date;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.util.UpdateAdjudicationData;
import pt.unl.fct.di.apdc.firstwebapp.util.WorkSheetData;

@Path("/worksheet")
public class WorkSheetResource {

	private static final String MESSAGE_INVALID_USER = "Incorrect username, please try again.";
	private static final String MESSAGE_INVALID_TOKEN = "No valid token.";
	private static final String MESSAGE_INVALID_PERMISSION = "You are not allowed to create worksheets.";
	private static final String MESSAGE_INVALID_UPDATE = "User not authorized for update.";
	private static final String MESSAGE_INVALID_FIELDS = "Missing required fields.";
	private static final String MESSAGE_INVALID_REFERENCE = "WorkSheet already exists.";
	private static final String MESSAGE_INVALID_WORKSHEET = "WorkSheet not found.";
	private static final String MESSAGE_INVALID_ADJUDICATION_FIELDS = "Missing adjudication details.";
	private static final String MESSAGE_INVALID_UPDATE_ADJUDICATION = "Cannot update adjudication to not adjudicated";
	private static final String MESSAGE_WORK_SHEET_CREATION_SUCCESSFUL = "WorkSheet created successfully.";
	private static final String MESSAGE_WORK_SHEET_UPDATE_SUCCESSFUL = "WorkSheet updated successfully.";

	private static final String LOG_MESSAGE_CREATE_WORK_SHEET_ATTEMPT = "Create WorkSheet attempt by: ";
	private static final String LOG_MESSAGE_UPDATE_WORK_SHEET_ATTEMPT = "Update WorkSheet attempt by: ";
	private static final String LOG_MESSAGE_UNKNOWN_USER = "User not found in the database: ";
	private static final String LOG_MESSAGE_NONEXISTING_VALID_TOKEN = "No valid token found for: ";

	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

	private final Gson g = new Gson();

	public WorkSheetResource() {

	}

	@POST
	@Path("/create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createWorkSheet(WorkSheetData data) {
		LOG.fine(LOG_MESSAGE_CREATE_WORK_SHEET_ATTEMPT + data.username);

		Key requesterKey = userKeyFactory.newKey(data.username);
		Entity requester = datastore.get(requesterKey);
		if (requester == null) {
			LOG.warning(LOG_MESSAGE_UNKNOWN_USER + data.username);
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

		String role = requester.getString("user_role");
		if (!role.equals("BACKOFFICE")) {
			return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_PERMISSION).build();
		}

		if (data.reference == null || data.description == null || data.targetType == null
				|| data.adjudicationState == null || !data.isValidFields()) {
			return Response.status(Status.BAD_REQUEST).entity(MESSAGE_INVALID_FIELDS).build();
		}

		KeyFactory worksheetFactory = datastore.newKeyFactory().setKind("WorkSheet");
		Key worksheetKey = worksheetFactory.newKey(data.reference);
		if (datastore.get(worksheetKey) != null) {
			return Response.status(Status.CONFLICT).entity(MESSAGE_INVALID_REFERENCE).build();
		}

		Entity.Builder builder = Entity.newBuilder(worksheetKey).set("ws_desc", data.description)
				.set("ws_target_type", data.targetType).set("ws_adj_state", data.adjudicationState);

		if (data.adjudicationState.equals("ADJUDICADO")) {
			Key partnerKey = userKeyFactory.newKey(data.partnerAccount);
			Entity partnerEntity = datastore.get(partnerKey);
			if (data.adjudicationDate == null || data.startDate == null || data.endDate == null
					|| data.partnerAccount == null || data.adjudicationEntity == null || data.companyNIF == null
					|| !data.isValidAdjudicationFields() || partnerEntity == null
					|| !partnerEntity.getString("user_role").equalsIgnoreCase("PARTNER")) {
				return Response.status(Status.BAD_REQUEST).entity(MESSAGE_INVALID_ADJUDICATION_FIELDS).build();
			}
			builder.set("ws_adj_date", data.adjudicationDate).set("ws_start_date", data.startDate)
					.set("ws_end_date", data.endDate).set("ws_partner", data.partnerAccount)
					.set("ws_adj_entity", data.adjudicationEntity).set("ws_comp_nif", data.companyNIF)
					.set("ws_state", data.workState).set("ws_observations", data.observations);
		}

		datastore.put(builder.build());
		return Response.ok(g.toJson(MESSAGE_WORK_SHEET_CREATION_SUCCESSFUL)).build();
	}

	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateWorkSheet(UpdateAdjudicationData data) {
		LOG.fine(LOG_MESSAGE_UPDATE_WORK_SHEET_ATTEMPT + data.username);

		Key requesterKey = userKeyFactory.newKey(data.username);
		Entity requester = datastore.get(requesterKey);
		if (requester == null) {
			LOG.warning(LOG_MESSAGE_UNKNOWN_USER + data.username);
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

		String role = requester.getString("user_role");

		KeyFactory worksheetFactory = datastore.newKeyFactory().setKind("WorkSheet");
		Key worksheetKey = worksheetFactory.newKey(data.reference);
		Entity worksheet = datastore.get(worksheetKey);
		if (worksheet == null) {
			return Response.status(Status.NOT_FOUND).entity(MESSAGE_INVALID_WORKSHEET).build();
		}

		Entity.Builder builder = Entity.newBuilder(worksheet);

		switch (role) {
		case "BACKOFFICE":
			if (data.adjudicationState == null) {
				if (data.adjudicationDate != null && !data.adjudicationDate.trim().isEmpty()) {
					builder.set("ws_adj_date", data.adjudicationDate.trim());
				}
				if (data.startDate != null && !data.startDate.trim().isEmpty()) {
					builder.set("ws_start_date", data.startDate.trim());
				}
				if (data.endDate != null && !data.endDate.trim().isEmpty()) {
					builder.set("ws_end_date", data.endDate.trim());
				}
				if (data.partnerAccount != null && !data.partnerAccount.trim().isEmpty()) {
					Key partnerKey = userKeyFactory.newKey(data.partnerAccount.trim());
					Entity partnerEntity = datastore.get(partnerKey);
					if (partnerEntity == null || !partnerEntity.getString("user_role").equalsIgnoreCase("PARTNER")) {
						return Response.status(Status.BAD_REQUEST).entity(MESSAGE_INVALID_ADJUDICATION_FIELDS).build();
					}
					builder.set("ws_partner", data.partnerAccount.trim());
				}
				if (data.adjudicationEntity != null && !data.adjudicationEntity.trim().isEmpty()) {
					builder.set("ws_adj_entity", data.adjudicationEntity.trim());
				}
				if (data.companyNIF != null && !data.companyNIF.trim().isEmpty()) {
					builder.set("ws_comp_nif", data.companyNIF.trim());
				}
				if (data.workState != null && !data.workState.trim().isEmpty()) {
					builder.set("ws_state", data.workState.trim());
				}
				if (data.observations != null && !data.observations.trim().isEmpty()) {
					builder.set("ws_observations", data.observations.trim());
				}
			} else if (data.adjudicationState.equals("NÃO ADJUDICADO")) {
				return Response.status(Status.BAD_REQUEST).entity(MESSAGE_INVALID_UPDATE_ADJUDICATION).build();
			} else if (data.adjudicationState.equals("ADJUDICADO")) {
				Key partnerKey = userKeyFactory.newKey(data.partnerAccount);
				Entity partnerEntity = datastore.get(partnerKey);
				if (data.adjudicationDate == null || data.startDate == null || data.endDate == null
						|| data.partnerAccount == null || data.adjudicationEntity == null || data.companyNIF == null
						|| !data.isValidAdjudicationFields() || partnerEntity == null
						|| !partnerEntity.getString("user_role").equalsIgnoreCase("PARTNER")) {
					return Response.status(Status.BAD_REQUEST).entity(MESSAGE_INVALID_ADJUDICATION_FIELDS).build();
				}
				builder.set("ws_adj_date", data.adjudicationDate).set("ws_start_date", data.startDate)
						.set("ws_end_date", data.endDate).set("ws_partner", data.partnerAccount)
						.set("ws_adj_entity", data.adjudicationEntity).set("ws_comp_nif", data.companyNIF)
						.set("ws_state", data.workState).set("ws_observations", data.observations)
						.set("ws_adj_state", "ADJUDICADO");
			}else {
				return Response.status(Status.BAD_REQUEST).entity(MESSAGE_INVALID_ADJUDICATION_FIELDS).build();
			}
			break;

		case "PARTNER":
			if (data.workState == null || data.workState.trim().isEmpty()) {
				return Response.status(Status.BAD_REQUEST).entity(MESSAGE_INVALID_ADJUDICATION_FIELDS).build();
			}
			builder.set("ws_state", data.workState.trim());
			break;

		default:
			return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_UPDATE).build();
		}

		datastore.put(builder.build());
		return Response.ok(g.toJson(MESSAGE_WORK_SHEET_UPDATE_SUCCESSFUL)).build();
	}

}
