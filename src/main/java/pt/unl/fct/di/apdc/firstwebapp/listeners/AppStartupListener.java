package pt.unl.fct.di.apdc.firstwebapp.listeners;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import pt.unl.fct.di.apdc.firstwebapp.resources.RegisterResource;

@WebListener
public class AppStartupListener implements ServletContextListener {

	private static final String ROOT_ALREADY_EXISTS = "Root user already exists.";
	private static final String ROOT_CREATED = "User root created";
	private static final String ERROR_CREATING_ROOT = "Error during root user creation: ";

	private static final String USER_EMAIL = "user_email";
	private static final String USER_PWD = "user_pwd";
	private static final String USER_NAME = "user_name";
	private static final String USER_PHONE = "user_phone";
	private static final String USER_PRIVACY = "user_privacy";
	private static final String USER_ROLE = "user_role";
	private static final String USER_ACCOUNT_STATE = "user_account_state";

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
		Key userKey = datastore.newKeyFactory().setKind("User").newKey("root");
		Transaction txn = datastore.newTransaction();
		Entity rootUser = txn.get(userKey);
		if (rootUser != null) {
			txn.rollback();
			LOG.info(ROOT_ALREADY_EXISTS);
		} else {
			try {
				rootUser = Entity.newBuilder(userKey).set(USER_NAME, "Administrador")
						.set(USER_EMAIL, "root@example.com").set(USER_PHONE, "000000000")
						.set(USER_PWD, DigestUtils.sha512Hex("default_password")).set(USER_PRIVACY, "privada")
						.set(USER_ROLE, "ADMIN").set(USER_ACCOUNT_STATE, "ATIVADA").build();
				txn.put(rootUser);
				txn.commit();
				LOG.info(ROOT_CREATED);
			} catch (Exception e) {
				LOG.severe(ERROR_CREATING_ROOT + e.getMessage());
			} finally {
				if (txn.isActive()) {
					txn.rollback();
				}
			}
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// No cleanup needed
	}

}