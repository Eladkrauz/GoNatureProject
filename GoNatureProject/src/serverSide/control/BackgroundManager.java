package serverSide.control;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;

import clientSide.control.ParkController;
import common.communication.Communication;
import common.communication.Communication.CommunicationType;
import common.communication.Communication.QueryType;
import common.communication.Communication.ServerMessageType;
import common.communication.CommunicationException;
import entities.Booking;
import entities.Booking.VisitType;
import entities.Park;
import serverSide.gui.GoNatureServerUI;
import serverSide.jdbc.DatabaseController;
import serverSide.jdbc.DatabaseException;

public class BackgroundManager {
//	private final ScheduledExecutorService scheduler;
//	private int numberOfTasks = 4;
//	private NotificationsController notifications;
	private DatabaseController database;
	private ParkController parkControl = ParkController.getInstance();
	private ArrayList<Park> parks = new ArrayList<>();

	public BackgroundManager(DatabaseController database) {
//		scheduler = Executors.newScheduledThreadPool(numberOfTasks);
//		notification = new NotificationsController();
		this.database = database;
	}

//	public void startBackgroundOperations() {
//		scheduler.scheduleAtFixedRate(this::updateWaitingLists, 0, 1, TimeUnit.HOURS);
//		scheduler.scheduleAtFixedRate(this::updateActiveTables, 0, 1, TimeUnit.HOURS);
//		scheduler.scheduleAtFixedRate(this::sendReminders, 0, 1, TimeUnit.HOURS);
//		scheduler.scheduleAtFixedRate(this::checkReminders, 0, 1, TimeUnit.HOURS);
//
//	}

	///////////////////////////////////////
	/// WAITING LISTS BACKGROUND UPDATE ///
	///////////////////////////////////////

	/**
	 * This method is a background method executed repeatedly by a background thread
	 * for clearing old bookings from waiting lists of the parks. These bookings are
	 * bookings that their time and date of arrival have passed and no spot has
	 * found for them in the requested park. The process of this method is done as a
	 * transaction where all waiting list tables are scanned and relevant bookings
	 * are deleted (not transferred to another table).
	 * 
	 * @throws DatabaseException if there is a problem with the transaction
	 */
	@SuppressWarnings("static-access")
	public void waitingListsBackgroundUpdates() throws DatabaseException {
		// fetching parks information from the database
		fetchParks();

		// setting auto commit of the database to false
		try {
			database.toggleAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// creating a conquer request
		Communication conquer = new Communication(CommunicationType.SERVER_CLIENT_MESSAGE);
		conquer.setServerMessageType(ServerMessageType.CONQUER);
		conquer.setServerMessageContent("Updating Information.\nThis could take several seconds...");
		GoNatureServerUI.server.sendToAllClients(conquer);

		// executing queries
		for (Park park : parks) {
			// creating the select check for this specific park
			Communication checkWaitingBookings = new Communication(CommunicationType.SELF);
			try {
				checkWaitingBookings.setQueryType(QueryType.SELECT);
			} catch (CommunicationException e) {
				e.printStackTrace();
			}
			String tableName = parkControl.nameOfTable(park) + checkWaitingBookings.waitingList;
			checkWaitingBookings.setTables(Arrays.asList(tableName));
			checkWaitingBookings.setSelectColumns(Arrays.asList("bookingId", "dayOfVisit", "timeOfVisit"));
			checkWaitingBookings.setWhereConditions(Arrays.asList("dayOfVisit"), Arrays.asList("<="),
					Arrays.asList(LocalDate.now()));

			// getting the result from the database
			// all bookings in the waiting list table of this park
			// that their date has passed AND their hour has passed also
			ArrayList<Object[]> results = database.executeSelectQuery(checkWaitingBookings);
			ArrayList<String> idNumbers = new ArrayList<>();
			LocalDate today = LocalDate.now();
			LocalTime now = LocalTime.now();

			for (Object[] row : results) {
				LocalDate visitDate = ((Date) row[1]).toLocalDate();
				LocalTime visitTime = ((Time) row[2]).toLocalTime();
				boolean isTimeBeforeNow = visitTime.compareTo(now) <= 0;

				if ((visitDate.isEqual(today) && isTimeBeforeNow) || visitDate.isBefore(today)) {
					String idNumber = (String) row[0];
					idNumbers.add(idNumber);
				}
			}

			// in case there are bookings to delete, creating a request to delete them
			if (!idNumbers.isEmpty()) {
				Communication deleteBookings = new Communication(CommunicationType.SELF);
				try {
					deleteBookings.setQueryType(QueryType.DELETE);
				} catch (CommunicationException e) {
					e.printStackTrace();
				}
				deleteBookings.setTables(Arrays.asList(tableName));

				String value = "(";
				// creating the booking ids values
				for (int i = 0; i < idNumbers.size(); i++) {
					value += "'" + idNumbers.get(i) + "'";
					if (i + 1 < idNumbers.size())
						value += ", ";
				}
				value += ")";
				deleteBookings.setWhereConditions(Arrays.asList("bookingId"), Arrays.asList("IN"),
						Arrays.asList(value));

				boolean deleteResult = database.executeDeleteQuery(deleteBookings);
				if (!deleteResult) {
					throw new DatabaseException("Problem with DELETE query");
				}
			}
		}

		// commiting all the waiting queries
		try {
			database.commit();
		} catch (SQLException e) {
			try {
				// if a problem occures, rolling back all queries
				database.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
		try {
			// toggeling auto commit to allow other transactions
			database.toggleAutoCommit(true);
		} catch (SQLException e1) {
			e1.printStackTrace();

		}
	}

	////////////////////////////////////////
	/// ACTIVE TABLES BACKGROUND UPDATES ///
	////////////////////////////////////////

	/**
	 * This i
	 * 
	 * @throws DatabaseException
	 */
	@SuppressWarnings("static-access")
	public void activeTablesBackgroundUpdates() throws DatabaseException {
		// fetching parks information from the database
		fetchParks();

		// setting auto commit of the database to false
		try {
			database.toggleAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// creating a conquer request
		Communication conquer = new Communication(CommunicationType.SERVER_CLIENT_MESSAGE);
		conquer.setServerMessageType(ServerMessageType.CONQUER);
		conquer.setServerMessageContent("Updating Information.\nThis could take several seconds...");
		GoNatureServerUI.server.sendToAllClients(conquer);

		// executing queries
		for (Park park : parks) {
			// creating the select check for this specific park
			Communication checkWaitingBookings = new Communication(CommunicationType.SELF);
			try {
				checkWaitingBookings.setQueryType(QueryType.SELECT);
			} catch (CommunicationException e) {
				e.printStackTrace();
			}
			String tableName = parkControl.nameOfTable(park) + checkWaitingBookings.activeBookings;
			checkWaitingBookings.setTables(Arrays.asList(tableName));
			checkWaitingBookings.setSelectColumns(Arrays.asList("*"));
			checkWaitingBookings.setWhereConditions(Arrays.asList("dayOfVisit"), Arrays.asList("<="),
					Arrays.asList(LocalDate.now()));

			// getting the result from the database
			// all bookings in the active bookings table of this park
			// that their date has passed AND their hour has passed also, and they did not
			// arrive to the park
			ArrayList<Object[]> results = database.executeSelectQuery(checkWaitingBookings);
			ArrayList<String> idNumbers = new ArrayList<>();
			ArrayList<Booking> cancelledBookings = new ArrayList<>();
			LocalDate today = LocalDate.now();
			LocalTime now = LocalTime.now();

			for (Object[] row : results) {
				LocalDate visitDate = ((Date) row[1]).toLocalDate();
				LocalTime visitTime = ((Time) row[2]).toLocalTime();
				boolean isTimeBeforeNow = visitTime.compareTo(now) <= 0;
				boolean areTimesNull = (Time) row[14] == null && (Time) row[15] == null;

				if ((visitDate.isEqual(today) && isTimeBeforeNow && areTimesNull) || visitDate.isBefore(today)) {
					String idNumber = (String) row[0];
					idNumbers.add(idNumber);
					cancelledBookings.add(new Booking((String) row[0], ((Date) row[1]).toLocalDate(),
							((Time) row[2]).toLocalTime(), ((Date) row[3]).toLocalDate(),
							((String) row[4]).equals("group") ? VisitType.GROUP : VisitType.INDIVIDUAL,
							(Integer) row[5], (String) row[6], (String) row[7], (String) row[8], (String) row[9],
							(String) row[10], (Integer) row[11], (Integer) row[12] == 0 ? false : true,
							(Integer) row[13] == 0 ? false : true,
							((Time) row[14]) == null ? null : ((Time) row[14]).toLocalTime(),
							((Time) row[15]) == null ? null : ((Time) row[15]).toLocalTime(),
							(Integer) row[16] == 0 ? false : true,
							((Time) row[17]) == null ? null : ((Time) row[17]).toLocalTime(), park));
				}
			}

			// in case there are bookings to delete, creating a request to delete them
			if (!idNumbers.isEmpty()) {
				Communication deleteBookings = new Communication(CommunicationType.SELF);
				try {
					deleteBookings.setQueryType(QueryType.DELETE);
				} catch (CommunicationException e) {
					e.printStackTrace();
				}
				deleteBookings.setTables(Arrays.asList(tableName));

				String value = "(";
				// creating the booking ids values
				for (int i = 0; i < idNumbers.size(); i++) {
					value += "'" + idNumbers.get(i) + "'";
					if (i + 1 < idNumbers.size())
						value += ", ";
				}
				value += ")";
				deleteBookings.setWhereConditions(Arrays.asList("bookingId"), Arrays.asList("IN"),
						Arrays.asList(value));

				boolean deleteResult = database.executeDeleteQuery(deleteBookings);
				if (!deleteResult) {
					throw new DatabaseException("Problem with DELETE query");
				}
			}

			// if there were bookings to delete, inserting them to the cancelled table
			if (!cancelledBookings.isEmpty()) {
				for (Booking cancelledBooking : cancelledBookings) {
					// creating a communication request
					Communication insertCancelled = new Communication(CommunicationType.SELF);
					try {
						insertCancelled.setQueryType(QueryType.INSERT);
					} catch (CommunicationException e) {
						e.printStackTrace();
					}
					insertCancelled.setTables(
							Arrays.asList(parkControl.nameOfTable(park) + insertCancelled.cancelledBookings));
					insertCancelled.setColumnsAndValues(
							Arrays.asList("bookingId", "dayOfVisit", "timeOfVisit", "dayOfBooking", "visitType",
									"numberOfVisitors", "idNumber", "firstName", "lastName", "emailAddress",
									"phoneNumber", "cancellationReason"),
							Arrays.asList(cancelledBooking.getBookingId(), cancelledBooking.getDayOfVisit(),
									cancelledBooking.getTimeOfVisit(), cancelledBooking.getDayOfBooking(),
									cancelledBooking.getVisitType() == VisitType.GROUP ? "group" : "individual",
									cancelledBooking.getNumberOfVisitors(), cancelledBooking.getIdNumber(),
									cancelledBooking.getFirstName(), cancelledBooking.getLastName(),
									cancelledBooking.getEmailAddress(), cancelledBooking.getPhoneNumber(),
									"Did not arrive"));

					// sending the request to the database
					boolean insertResult = database.executeInsertQuery(insertCancelled);
					if (!insertResult) {
						throw new DatabaseException("Problem with INSERT query");
					}
				}
			}
		}

		// commiting all the waiting queries
		try {
			database.commit();
		} catch (SQLException e) {
			try {
				// if a problem occures, rolling back all queries
				database.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
		try {
			// toggeling auto commit to allow other transactions
			database.toggleAutoCommit(true);
		} catch (SQLException e1) {
			e1.printStackTrace();

		}
	}

	////////////////////////////////////////////
	/// REMINDERS SENDING BACKGROUND PROCESS ///
	////////////////////////////////////////////

//	private void remindersSendingBackground() {
//
//	}

	/////////////////////////////////////////////
	/// REMINDERS CHECKING BACKGROUND PROCESS ///
	/////////////////////////////////////////////

//	private void remindersCheckingBackground() {
//
//	}

	///////////////////////
	/// GENERAL METHODS ///
	///////////////////////

	/**
	 * This method is called from the server side (to itself) if a visitor cancelled
	 * his booking in a specific park. In this case, checking the waiting list in
	 * order to release booking/s from it, since there is a capacity update cause
	 * the visitor cancelled his booking.
	 * 
	 * @param park
	 * @param date
	 * @param time
	 */
	public void checkWaitingListReleasePossibility(int parkId, LocalDate date, LocalTime time) {

		//////////////////
		/// FIRST PART ///
		//////////////////

		// getting all waiting list bookings that can enter the park (in terms of group
		// size) in the given time frame

		// get updated parks
		fetchParks();
		parks.sort(Park.parkComparator);
		Park park = parks.get(parkId - 1);

		int timeLimit = park.getTimeLimit();
		int currentCapacity = getCurrentParkCapacities(park, date, time, timeLimit);
		int moreCanEnter = park.getMaximumOrders() - currentCapacity;

		// creating a communication for checking the possibility of releasing booking/s
		// from the waiting list
		Communication checkWaitingList = new Communication(CommunicationType.SELF);
		try {
			checkWaitingList.setQueryType(QueryType.SELECT);
		} catch (CommunicationException e) {
			e.printStackTrace();
		}
		String parkTableName = parkControl.nameOfTable(park) + Communication.waitingList;
		checkWaitingList.setTables(Arrays.asList(parkTableName));
		checkWaitingList.setSelectColumns(Arrays.asList("*"));
		checkWaitingList.setWhereConditions(
				Arrays.asList("dayOfVisit", "timeOfVisit", "timeOfVisit", "numberOfVisitors"),
				Arrays.asList("=", "AND", ">", "AND", "<", "AND", "<="),
				Arrays.asList(date, time.minusHours(timeLimit), time.plusHours(timeLimit), moreCanEnter));

		ArrayList<Object[]> selectResult = database.executeSelectQuery(checkWaitingList);
		ArrayList<Booking> waitingResults = new ArrayList<>();

		// run over the result of the SELECT query and take all relevant waiting list
		// bookings that are in the same time frame as the paramteres of the method
		for (Object[] row : selectResult) {
			Booking add = new Booking((String) row[0], ((Date) row[1]).toLocalDate(), ((Time) row[2]).toLocalTime(),
					((Date) row[3]).toLocalDate(),
					((String) row[5]).equals("group") ? VisitType.GROUP : VisitType.INDIVIDUAL, (Integer) row[6],
					(String) row[7], (String) row[8], (String) row[9], (String) row[10], (String) row[11], -1, false,
					false, null, null, false, null, park);
			add.setWaitingListPriority((Integer)row[4]);
			// HERE: need to add:
			// add.setFinalPrice(PaymentController:
			// .calculateFinalDiscountPrice(booking,isGroupReservation, false));
			waitingResults.add(add);
		}

		// waitingResults holds all the bookings that CAN be entered in terms of their
		// group size
		waitingResults.sort(Booking.waitingListComparator); // sorting these bookings by their priority

		/////////////////////////////////////////////
		System.out.println("Order of bookings:");
		for (Booking in : waitingResults) {
			System.out.println(in.getBookingId() + " - " + in.getWaitingListPriority());
		}		
		
		///////////////////
		/// SECOND PART ///
		///////////////////

		// locking the database
		try {
			database.toggleAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// transferring all relevant bookings from the waiting list table to the active
		// bookings table

		// the bookings in transferBookings will be transferred to the active table
		ArrayList<Booking> transferBookings = new ArrayList<>();
		int decreasePriority = 0;
		for (Booking currentBooking : waitingResults) {
			System.out.println("Checking ID: " + currentBooking.getBookingId());
			int currentBookingSize = currentBooking.getNumberOfVisitors();
			if (currentBookingSize <= moreCanEnter) {
				moreCanEnter -= currentBookingSize;
				decreasePriority++;
				transferBookings.add(currentBooking);
			} else {
				currentBooking.setWaitingListPriority(currentBooking.getWaitingListPriority() - decreasePriority);
			}
		}
		
		waitingResults.removeAll(transferBookings);
		
		System.out.println("Transfer: " + transferBookings);
		System.out.println("Waiting: " + waitingResults);

		// now: waitingResults holds all the waiting list bookings that are going to
		// stay in the waiting list, but with possible new priority. transferBookings
		// holds all the bookings that need to be released from the waiting list.

		// first: removing all released waiting list bookings from the waiting list
		// table and inserting them to the active bookings table
		for (Booking transfer : transferBookings) {
			// deleting
			Communication delete = new Communication(CommunicationType.SELF);
			try {
				delete.setQueryType(QueryType.DELETE);
			} catch (CommunicationException e) {
				e.printStackTrace();
			}
			delete.setTables(Arrays.asList(parkTableName));
			delete.setWhereConditions(Arrays.asList("bookingId"), Arrays.asList("="),
					Arrays.asList(transfer.getBookingId()));

			database.executeDeleteQuery(delete);

			// inserting
			Communication insert = new Communication(CommunicationType.SELF);
			try {
				insert.setQueryType(QueryType.INSERT);
			} catch (CommunicationException e) {
				e.printStackTrace();
			}
			insert.setTables(Arrays.asList(parkControl.nameOfTable(park) + Communication.activeBookings));

			// updating final price
			// HERE: need to add:
			// transfer.setFinalPrice(PaymentController:
			// .calculateFinalDiscountPrice(booking,isGroupReservation, false));
			transfer.setFinalPrice(transfer.getNumberOfVisitors() * 50);

			insert.setColumnsAndValues(
					Arrays.asList("bookingId", "dayOfVisit", "timeOfVisit", "dayOfBooking", "visitType",
							"numberOfVisitors", "idNumber", "firstName", "lastName", "emailAddress", "phoneNumber",
							"finalPrice", "paid", "confirmed", "entryParkTime", "exitParkTime", "isRecievedReminder",
							"reminderArrivalTime"),
					Arrays.asList(transfer.getBookingId(), transfer.getDayOfVisit(), transfer.getTimeOfVisit(),
							transfer.getDayOfBooking(),
							transfer.getVisitType() == VisitType.GROUP ? "group" : "individual",
							transfer.getNumberOfVisitors(), transfer.getIdNumber(), transfer.getFirstName(),
							transfer.getLastName(), transfer.getEmailAddress(), transfer.getPhoneNumber(),
							transfer.getFinalPrice(), transfer.isPaid() == false ? 0 : 1,
							transfer.isConfirmed() == false ? 0 : 1, transfer.getEntryParkTime(),
							transfer.getExitParkTime(), transfer.isRecievedReminder() == false ? 0 : 1,
							transfer.getReminderArrivalTime()));

			database.executeInsertQuery(insert);
		}

		// second: updating all remaining waiting list bookings' priorities

		for (Booking updatePriority : waitingResults) {
			Communication update = new Communication(CommunicationType.SELF);
			try {
				update.setQueryType(QueryType.UPDATE);
			} catch (CommunicationException e) {
				e.printStackTrace();
			}
			update.setTables(Arrays.asList(parkTableName));
			update.setColumnsAndValues(Arrays.asList("waitingListOrder"),
					Arrays.asList(updatePriority.getWaitingListPriority()));
			update.setWhereConditions(Arrays.asList("bookingId"), Arrays.asList("="),
					Arrays.asList(updatePriority.getBookingId()));

			database.executeUpdateQuery(update);
		}

		// commiting all the waiting queries
		try {
			database.commit();
		} catch (SQLException e) {
			try {
				// if a problem occures, rolling back all queries
				database.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
		try {
			// toggeling auto commit to allow other transactions
			database.toggleAutoCommit(true);
		} catch (SQLException e1) {
			e1.printStackTrace();

		}

		//////////////////
		/// THIRD PART ///
		//////////////////

		// sending confirmation to the transferred bookings
//		for (Booking transfer : transferBookings) {
//			sendConfirmationPlaceFound(transfer);
//		}
	}

	/**
	 * This method gets a park, a date and a time and returns the park's current
	 * orders capacity in this time frame
	 * 
	 * @param park
	 * @param date
	 * @param time
	 * @param timeLimit
	 * @return the park's current capacity for the specified time frame
	 */
	private int getCurrentParkCapacities(Park park, LocalDate date, LocalTime time, int timeLimit) {
		// creating the request for the availability check
		Communication availabilityRequest = new Communication(CommunicationType.SELF);
		try {
			availabilityRequest.setQueryType(QueryType.SELECT);
		} catch (CommunicationException e) {
			e.printStackTrace();
		}

		@SuppressWarnings("static-access")
		String parkTableName = parkControl.nameOfTable(park) + availabilityRequest.activeBookings;

		availabilityRequest.setTables(Arrays.asList(parkTableName));
		availabilityRequest.setSelectColumns(Arrays.asList("numberOfVisitors"));
		availabilityRequest.setWhereConditions(Arrays.asList("dayOfVisit", "timeOfVisit", "timeOfVisit"),
				Arrays.asList("=", "AND", ">", "AND", "<"),
				Arrays.asList(date, time.minusHours(timeLimit), time.plusHours(timeLimit)));

		ArrayList<Object[]> results = database.executeSelectQuery(availabilityRequest);

		// getting the result from the database
		int countVisitors = 0;
		// checking the orders amount for the specific time
		for (Object[] row : results) {
			countVisitors += (Integer) row[0];
		}

		return countVisitors;
	}

	/**
	 * This method is called in order to insert parks details into the parks array
	 * list property
	 */
	private void fetchParks() {
		// creating a communication instance for fetching the up to date parks
		Communication getParks = new Communication(CommunicationType.SELF);
		try {
			getParks.setQueryType(QueryType.SELECT);
		} catch (CommunicationException e) {
			e.printStackTrace();
		}
		getParks.setTables(Arrays.asList(Communication.park));
		getParks.setSelectColumns(Arrays.asList("*"));

		// executing the SELECT query
		ArrayList<Object[]> results = database.executeSelectQuery(getParks);
		// getting the result
		if (!parks.isEmpty()) {
			parks.removeAll(parks);
		}
		// setting the Object[] from DB to the parkList
		for (Object[] row : results) {
			Park parkToAdd = new Park((Integer) row[0], (String) row[1], (String) row[2], (String) row[3],
					(String) row[4], (String) row[5], (String) row[6], (Integer) row[7], (Integer) row[8],
					(Integer) row[9], (Integer) row[10]);
			parks.add(parkToAdd);
		}
	}
}